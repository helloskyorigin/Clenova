package com.clenova.app.data.storage

import android.app.PendingIntent
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.IOException

/** MediaStore-scoped large files only. No all-files permission or direct deletion. */
internal object LargeMediaTrash {
    const val MAX_BATCH = 100

    fun eligible(result: MediaScanResult, videosOnly: Boolean): List<ScannedMedia> {
        val keepers = (result.duplicateGroups + result.similarGroups).mapNotNull { it.firstOrNull()?.uri }.toSet()
        val items = if (videosOnly) result.largeVideos else result.largeFiles
        return items.filter {
            it.bytes >= MediaScanner.LARGE_VIDEO_BYTES && (!videosOnly || it.video) &&
                !it.favorite && it.uri !in keepers && it.generation != null && it.generation > 0L &&
                it.modified != null && it.uri.authority == MediaStore.AUTHORITY
        }.distinctBy { it.uri }
    }

    fun plan(result: MediaScanResult, selected: Set<Uri>, videosOnly: Boolean): List<ScannedMedia> {
        require(selected.isNotEmpty() && selected.size <= MAX_BATCH)
        val items = eligible(result, videosOnly).filter { it.uri in selected }
        require(items.map { it.uri }.toSet() == selected)
        return items
    }

    suspend fun request(context: Context, result: MediaScanResult, selected: Set<Uri>, videosOnly: Boolean): PendingIntent =
        requestVerified(context, result, plan(result, selected, videosOnly))

    /** Shared metadata revalidation for explicitly reviewed, policy-filtered MediaStore items. */
    internal suspend fun requestVerified(context: Context, result: MediaScanResult, items: List<ScannedMedia>): PendingIntent =
        withContext(Dispatchers.IO) {
            check(Build.VERSION.SDK_INT >= 30)
            check(mediaAccess(context) == result.access)
            require(items.isNotEmpty() && items.size <= MAX_BATCH && items.distinctBy { it.uri }.size == items.size)
            val selected = items.map { it.uri }
            val resolver = context.contentResolver
            val fields = arrayOf(MediaStore.MediaColumns.SIZE, MediaStore.MediaColumns.IS_FAVORITE,
                MediaStore.MediaColumns.IS_TRASHED, MediaStore.MediaColumns.IS_PENDING,
                MediaStore.MediaColumns.GENERATION_MODIFIED, MediaStore.MediaColumns.DATE_MODIFIED,
                MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.RELATIVE_PATH)
            for (item in items) {
                currentCoroutineContext().ensureActive()
                val valid = resolver.query(item.uri, fields, null, null, null)?.use { c ->
                    c.moveToFirst() && c.getLong(0) == item.bytes && c.getInt(1) == 0 &&
                        c.getInt(2) == 0 && c.getInt(3) == 0 && !c.isNull(4) && !c.isNull(5) &&
                        c.getLong(4) == item.generation && c.getLong(5) == item.modified &&
                        c.getString(6).orEmpty() == item.name && c.getString(7).orEmpty() == item.folder
                } ?: false
                if (!valid) throw IOException("Large media changed")
            }
            check(mediaAccess(context) == result.access)
            currentCoroutineContext().ensureActive()
            MediaStore.createTrashRequest(resolver, selected.toList(), true)
        }
}
