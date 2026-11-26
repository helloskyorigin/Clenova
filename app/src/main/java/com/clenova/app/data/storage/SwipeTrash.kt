package com.clenova.app.data.storage

import android.app.PendingIntent
import android.content.Context
import android.net.Uri
import android.provider.MediaStore

internal object SwipeTrash {
    const val MAX_BATCH = 100
    fun eligible(result: MediaScanResult): Set<Uri> {
        val keepers = (result.duplicateGroups + result.similarGroups).mapNotNull { it.firstOrNull()?.uri }.toSet()
        return result.photos.filter {
            !it.video && it.bytes > 0 && !it.favorite && it.uri !in keepers &&
                it.generation != null && it.generation > 0L && it.modified != null && it.uri.authority == MediaStore.AUTHORITY
        }.map { it.uri }.toSet()
    }
    fun plan(result: MediaScanResult, selected: Set<Uri>): List<ScannedMedia> {
        require(selected.isNotEmpty() && selected.size <= MAX_BATCH)
        val allowed = eligible(result)
        require(allowed.containsAll(selected))
        val items = result.photos.filter { it.uri in selected }.distinctBy { it.uri }
        require(items.map { it.uri }.toSet() == selected)
        return items
    }
    suspend fun request(context: Context, result: MediaScanResult, selected: Set<Uri>): PendingIntent =
        LargeMediaTrash.requestVerified(context, result, plan(result, selected))
}

internal data class SwipeSession(val decisions: List<Pair<Uri, Boolean>> = emptyList()) {
    val index: Int get() = decisions.size
    val selected: Set<Uri> get() = decisions.filter { it.second }.map { it.first }.toSet()
    fun decide(uri: Uri, mark: Boolean, allowed: Set<Uri>): SwipeSession {
        if (decisions.any { it.first == uri } || (mark && (uri !in allowed || selected.size >= SwipeTrash.MAX_BATCH))) return this
        return copy(decisions = decisions + (uri to mark))
    }
    fun undo(): SwipeSession = copy(decisions = decisions.dropLast(1))
    fun unmark(uri: Uri): SwipeSession = copy(decisions = decisions.map { if (it.first == uri) it.first to false else it })
}
