package com.clenova.app.data.storage

import android.Manifest
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.IOException
import java.security.MessageDigest

internal data class MediaAccess(val images: Boolean, val videos: Boolean, val selected: Boolean) {
    val any: Boolean get() = images || videos || selected
    val full: Boolean get() = images && videos
}

internal fun mediaAccess(context: Context): MediaAccess {
    fun granted(permission: String) = context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    if (Build.VERSION.SDK_INT < 33) {
        val allowed = granted(Manifest.permission.READ_EXTERNAL_STORAGE)
        return MediaAccess(allowed, allowed, false)
    }
    return MediaAccess(granted(Manifest.permission.READ_MEDIA_IMAGES), granted(Manifest.permission.READ_MEDIA_VIDEO),
        Build.VERSION.SDK_INT >= 34 && granted(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED))
}

internal fun mediaPermissions(): Array<String> = when {
    Build.VERSION.SDK_INT >= 34 -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
    Build.VERSION.SDK_INT >= 33 -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
    else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
}

internal data class ScannedMedia(val uri: Uri, val name: String, val bytes: Long, val video: Boolean, val folder: String, val favorite: Boolean = false, val verifiedHash: String? = null) {
    val screenshot: Boolean get() = !video && (folder.replace('\\', '/').split('/').any { it.equals("screenshots", true) || it.equals("screenshot", true) }
        || name.startsWith("Screenshot", true) || name.startsWith("Screen_Shot", true))
}

internal data class MediaScanResult(
    val access: MediaAccess,
    val scanned: Int,
    val duplicateGroups: List<List<ScannedMedia>>,
    val screenshots: List<ScannedMedia>,
    val largeVideos: List<ScannedMedia>,
    val incomplete: Boolean,
) {
    val extras = duplicateGroups.flatMap { it.drop(1).filterNot { media -> media.favorite } }
    // A file may appear in multiple categories; count it only once.
    val reviewBytes = (extras + screenshots + largeVideos).distinctBy { it.uri }.sumOf { it.bytes }
    val duplicateBytes = extras.sumOf { it.bytes }
}

internal class MediaScanner(private val context: Context) {
    companion object {
        const val LARGE_VIDEO_BYTES = 100_000_000L
        private const val MAX_ITEMS = 20_000
        private const val HASH_BUDGET = 1_000_000_000L
    }

    suspend fun scan(onProgress: suspend (stage: Int, done: Int, total: Int) -> Unit): MediaScanResult = withContext(Dispatchers.IO) {
        val access = mediaAccess(context)
        if (!access.any) throw SecurityException("Media access required")
        val resolver = context.contentResolver
        val inventory = linkedMapOf<Uri, ScannedMedia>()
        var incomplete = false
        val volumes = if (Build.VERSION.SDK_INT >= 29) MediaStore.getExternalVolumeNames(context).sorted() else listOf("external")
        val collections = volumes.flatMap { volume ->
            buildList {
                if (access.images || access.selected) add(MediaStore.Images.Media.getContentUri(volume) to false)
                if (access.videos || access.selected) add(MediaStore.Video.Media.getContentUri(volume) to true)
            }
        }
        if (collections.isEmpty()) throw IOException("No accessible media collections")
        onProgress(0, 0, 0)
        for ((collection, video) in collections) {
            currentCoroutineContext().ensureActive()
            val folderColumn = if (Build.VERSION.SDK_INT >= 29) MediaStore.MediaColumns.RELATIVE_PATH else MediaStore.MediaColumns.DATA
            val projection = arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.SIZE, folderColumn) + if (Build.VERSION.SDK_INT >= 30) arrayOf(MediaStore.MediaColumns.IS_FAVORITE) else emptyArray<String>()
            val selection = if (Build.VERSION.SDK_INT >= 30) "${MediaStore.MediaColumns.IS_PENDING}=0 AND ${MediaStore.MediaColumns.IS_TRASHED}=0"
                else if (Build.VERSION.SDK_INT >= 29) "${MediaStore.MediaColumns.IS_PENDING}=0" else null
            val cursor = resolver.query(collection, projection, selection, null, "${MediaStore.MediaColumns._ID} ASC")
                ?: throw IOException("Media provider unavailable")
            cursor.use {
                while (it.moveToNext()) {
                    currentCoroutineContext().ensureActive()
                    if (inventory.size >= MAX_ITEMS) { incomplete = true; break }
                    val bytes = it.getLong(2)
                    if (bytes <= 0L) { incomplete = true; continue }
                    val uri = ContentUris.withAppendedId(collection, it.getLong(0))
                    inventory[uri] = ScannedMedia(uri, it.getString(1).orEmpty(), bytes, video, it.getString(3).orEmpty(), favorite = Build.VERSION.SDK_INT >= 30 && it.getInt(4) != 0)
                }
            }
            if (inventory.size >= MAX_ITEMS) { incomplete = true; break }
        }
        val items = inventory.values.toList()
        val candidates = items.groupBy { it.bytes }.values.filter { it.size > 1 }.flatten()
        val hashes = linkedMapOf<Pair<Long, String>, MutableList<ScannedMedia>>()
        var remaining = HASH_BUDGET
        onProgress(1, 0, candidates.size)
        candidates.forEachIndexed { i, media ->
            currentCoroutineContext().ensureActive()
            if (media.bytes > remaining) {
                incomplete = true
            } else {
                remaining -= media.bytes
                try {
                    val hash = hash(resolver, media)
                    hashes.getOrPut(media.bytes to hash) { mutableListOf() }.add(media.copy(verifiedHash = hash))
                } catch (_: IOException) {
                    incomplete = true
                } catch (_: SecurityException) {
                    incomplete = true
                }
            }
            onProgress(1, i + 1, candidates.size)
        }
        onProgress(2, 0, 0)
        val screenshots = items.filter { it.screenshot }
        val largeVideos = items.filter { it.video && it.bytes >= LARGE_VIDEO_BYTES }.sortedByDescending { it.bytes }
        val groups = hashes.values.filter { it.size > 1 }.map { it.sortedWith(compareByDescending<ScannedMedia> { m -> m.favorite }.thenBy { m -> m.uri.toString() }) }
        currentCoroutineContext().ensureActive()
        if (mediaAccess(context) != access) throw SecurityException("Media access changed")
        MediaScanResult(access, items.size, groups, screenshots, largeVideos, incomplete)
    }

    internal suspend fun hash(resolver: ContentResolver, media: ScannedMedia): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(64 * 1024)
        var bytes = 0L
        val input = resolver.openInputStream(media.uri) ?: throw IOException("Unreadable media")
        input.use {
            while (true) {
                currentCoroutineContext().ensureActive()
                val n = it.read(buffer)
                if (n < 0) break
                if (n == 0) continue
                bytes += n
                if (bytes > media.bytes) throw IOException("Media changed during scan")
                digest.update(buffer, 0, n)
            }
        }
        if (bytes != media.bytes) throw IOException("Media size changed during scan")
        return digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }
}
