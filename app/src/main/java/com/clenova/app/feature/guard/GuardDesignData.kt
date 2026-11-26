package com.clenova.app.feature.guard

import androidx.annotation.StringRes
import com.example.R

/** Disjoint development collections, not device file identifiers. */
internal object GuardDesignData {
    const val INITIAL_MASK = 15
    const val ALL_MASK = 63
    data class Collection(val bit: Int, @StringRes val title: Int, val count: Int, val type: Int)
    val collections = listOf(
        Collection(1, R.string.guard_family, 120, 0),
        Collection(2, R.string.guard_favorites, 68, 0),
        Collection(4, R.string.guard_videos, 36, 1),
        Collection(8, R.string.guard_documents, 24, 2),
        Collection(16, R.string.guard_travel, 18, 0),
        Collection(32, R.string.guard_downloads, 12, 2),
    )
    fun count(mask: Int): Int = collections.filter { mask and it.bit != 0 }.sumOf { it.count }
}
