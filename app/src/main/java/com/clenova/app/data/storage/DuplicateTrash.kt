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

internal object DuplicateTrash {
    const val MAX_BATCH = 100
    private const val MAX_VERIFY_BYTES = 1_000_000_000L

    fun allowed(groups: List<List<ScannedMedia>>): Set<Uri> =
        groups.flatMap { it.drop(1).filterNot { media -> media.favorite } }.map { it.uri }.toSet()

    fun batch(groups: List<List<ScannedMedia>>): Set<Uri> {
        val selected = linkedSetOf<Uri>()
        var remaining = MAX_VERIFY_BYTES
        for (group in groups) {
            val keeper = group.firstOrNull() ?: continue
            var keeperCounted = false
            for (copy in group.drop(1).filterNot { it.favorite }) {
                if (selected.size >= MAX_BATCH) return selected
                val cost = copy.bytes + if (keeperCounted) 0L else keeper.bytes
                if (cost > remaining) continue
                remaining -= cost
                keeperCounted = true
                selected.add(copy.uri)
            }
        }
        return selected
    }

    fun plan(groups: List<List<ScannedMedia>>, selection: Set<Uri>): List<Pair<ScannedMedia, List<ScannedMedia>>> {
        require(selection.isNotEmpty() && selection.size <= MAX_BATCH)
        require(allowed(groups).containsAll(selection))
        val plan = groups.mapNotNull { group ->
            val selected = group.drop(1).filter { it.uri in selection }
            if (selected.isEmpty()) null else group.first() to selected
        }
        require(plan.flatMap { it.second }.map { it.uri }.toSet() == selection)
        require(plan.sumOf { (keep, copies) -> keep.bytes + copies.sumOf { it.bytes } } <= MAX_VERIFY_BYTES)
        return plan
    }

    /** Never modifies media directly. Recheck both keeper and selected files before OS consent. */
    suspend fun request(context: Context, snapshot: MediaScanResult, selection: Set<Uri>, similar: Boolean = false): PendingIntent =
        withContext(Dispatchers.IO) {
            check(Build.VERSION.SDK_INT >= 30)
            check(mediaAccess(context) == snapshot.access)
            val plan = plan(if (similar) snapshot.similarGroups else snapshot.duplicateGroups, selection)
            val scanner = MediaScanner(context.applicationContext)
            val resolver = context.contentResolver
            suspend fun verify(media: ScannedMedia, isKeeper: Boolean) {
                currentCoroutineContext().ensureActive()
                if (media.uri.authority != MediaStore.AUTHORITY || media.verifiedHash == null) throw IOException("Unverified media")
                val fields = arrayOf(MediaStore.MediaColumns.SIZE, MediaStore.MediaColumns.IS_FAVORITE, MediaStore.MediaColumns.IS_TRASHED, MediaStore.MediaColumns.IS_PENDING)
                val valid = resolver.query(media.uri, fields, null, null, null)?.use {
                    it.moveToFirst() && it.getLong(0) == media.bytes && (isKeeper || it.getInt(1) == 0) && it.getInt(2) == 0 && it.getInt(3) == 0
                } ?: false
                if (!valid || scanner.hash(resolver, media) != media.verifiedHash) throw IOException("Media changed")
            }
            for ((keeper, copies) in plan) {
                verify(keeper, true)
                for (copy in copies) {
                    if (!similar && copy.verifiedHash != keeper.verifiedHash) throw IOException("Duplicate mismatch")
                    verify(copy, false)
                }
            }
            check(mediaAccess(context) == snapshot.access)
            currentCoroutineContext().ensureActive()
            MediaStore.createTrashRequest(resolver, selection.toMutableList(), true)
        }
}
