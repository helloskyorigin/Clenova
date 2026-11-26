package com.clenova.app.feature.clean

import java.util.Locale

/** Local debug-only fixture; IDs never refer to actual device files. */
internal object DuplicateDesignData {
    data class Group(val id: Int, val copies: Int, val bytesPerCopy: Long) {
        val removableIds: List<Int> get() = (1 until copies).map { id * 10 + it }
    }
    val groups = List(24) { i ->
        if (i % 2 == 0) Group(i, 5, 12_500_000L) else Group(i, 6, 10_000_000L)
    }
    val removableIds = groups.flatMap { it.removableIds }
    fun selectedBytes(ids: Collection<Int>): Long {
        val selected = ids.toSet()
        return groups.sumOf { group -> group.removableIds.count { it in selected } * group.bytesPerCopy }
    }
    fun label(bytes: Long): String = if (bytes >= 1_000_000_000L)
        String.format(Locale.US, "%.1f GB", bytes / 1_000_000_000.0)
    else String.format(Locale.US, if (bytes % 1_000_000L == 0L) "%.0f MB" else "%.1f MB", bytes / 1_000_000.0)
}
