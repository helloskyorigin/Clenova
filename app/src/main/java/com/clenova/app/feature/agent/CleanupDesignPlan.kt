package com.clenova.app.feature.agent

import androidx.annotation.StringRes
import com.example.R
import java.util.Locale

/** Isolated development fixtures, never connected to a deletion API. */
internal object CleanupDesignPlan {
    const val ALL = 7
    data class Group(val bit: Int, @StringRes val title: Int, @StringRes val detail: Int, val mb: Int)
    val groups = listOf(
        Group(1, R.string.review_duplicates, R.string.review_duplicates_detail, 1200),
        Group(2, R.string.review_videos, R.string.review_videos_detail, 3100),
        Group(4, R.string.review_screenshots, R.string.review_screenshots_detail, 500),
    )
    fun selectedMb(mask: Int): Int = groups.filter { mask and it.bit != 0 }.sumOf { it.mb }
    fun spaceLabel(mask: Int): String {
        val mb = selectedMb(mask)
        return if (mb >= 1000) String.format(Locale.US, "%.1f GB", mb / 1000.0) else "$mb MB"
    }
}
