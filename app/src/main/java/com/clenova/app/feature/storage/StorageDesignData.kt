package com.clenova.app.feature.storage

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.clenova.app.domain.model.StorageOverview
import com.clenova.app.domain.model.StorageState
import com.example.R

/** Local UI review fixtures. Both callers gate display on BuildConfig.DEBUG. */
internal object StorageDesignData {
    val state = StorageState.Ready(StorageOverview(128_000_000_000L, 24_600_000_000L, 103_400_000_000L))
    const val usedLabel = "103.4 GB"
    val categories = listOf(
        StorageCategory(R.string.storage_apps, 42_800_000_000L, Color(0xFF0057FF)),
        StorageCategory(R.string.storage_photos, 19_600_000_000L, Color(0xFF4D8BFF)),
        StorageCategory(R.string.storage_videos, 24_100_000_000L, Color(0xFF63B7D9)),
        StorageCategory(R.string.storage_audio, 3_200_000_000L, Color(0xFF8E80CA)),
        StorageCategory(R.string.storage_documents, 2_500_000_000L, Color(0xFFDAA65D)),
        StorageCategory(R.string.storage_other, 11_200_000_000L, Color(0xFF8C98AC)),
    )
}

internal data class StorageCategory(@StringRes val label: Int, val bytes: Long, val color: Color)
