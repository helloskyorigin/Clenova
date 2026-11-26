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

internal object ScreenshotTrash {
    const val MAX_BATCH = 100
    private const val MAX_BYTES = 1_000_000_000L

    fun eligible(result: MediaScanResult): List<ScannedMedia> {
        val keepers = result.duplicateGroups.mapNotNull { it.firstOrNull()?.uri }.toSet()
        return result.screenshots.filter {
            it.screenshot && !it.favorite && it.verifiedHash != null && it.uri !in keepers &&
                it.uri.authority == MediaStore.AUTHORITY
        }.distinctBy { it.uri }
    }

    fun plan(result: MediaScanResult, selected: Set<Uri>): List<ScannedMedia> {
        require(selected.isNotEmpty() && selected.size <= MAX_BATCH)
        val items = eligible(result).filter { it.uri in selected }
        require(items.map { it.uri }.toSet() == selected)
        require(items.sumOf { it.bytes } <= MAX_BYTES)
        return items
    }

    suspend fun request(context: Context, result: MediaScanResult, selected: Set<Uri>): PendingIntent =
        withContext(Dispatchers.IO) {
            if (Build.VERSION.SDK_INT < 30) throw UnsupportedOperationException("Android 11 required")
            check(mediaAccess(context) == result.access)
            val items = plan(result, selected)
            val resolver = context.contentResolver
            val scanner = MediaScanner(context.applicationContext)
            val columns = arrayOf(MediaStore.MediaColumns.SIZE, MediaStore.MediaColumns.IS_FAVORITE,
                MediaStore.MediaColumns.IS_TRASHED, MediaStore.MediaColumns.IS_PENDING,
                MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.RELATIVE_PATH)
            for (item in items) {
                currentCoroutineContext().ensureActive()
                val valid = resolver.query(item.uri, columns, null, null, null)?.use { c ->
                    c.moveToFirst() && c.getLong(0) == item.bytes && c.getInt(1) == 0 &&
                        c.getInt(2) == 0 && c.getInt(3) == 0 &&
                        item.copy(name = c.getString(4).orEmpty(), folder = c.getString(5).orEmpty()).screenshot
                } ?: false
                if (!valid || scanner.hash(resolver, item) != item.verifiedHash) throw IOException("Screenshot changed")
            }
            check(mediaAccess(context) == result.access)
            currentCoroutineContext().ensureActive()
            MediaStore.createTrashRequest(resolver, selected.toList(), true)
        }
}
