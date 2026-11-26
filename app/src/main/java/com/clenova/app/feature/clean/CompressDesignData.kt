package com.clenova.app.feature.clean

/** UI fixtures only. Never used as media URIs or measured compression results. */
internal object CompressDesignData {
    data class Video(val id: Int, val name: String, val duration: String, val resolution: String, val mb: Int)
    val videos = listOf(
        Video(0, "VID_20261121.mp4", "02:38", "4K", 840),
        Video(1, "VID_20261118.mp4", "03:12", "1080p", 620),
        Video(2, "VID_20261110.mp4", "01:46", "1080p", 410),
        Video(3, "VID_20261105.mp4", "00:58", "1080p", 280),
    )
    enum class Quality(val percent: Int) { HIGH(75), BALANCED(50), SMALL(30) }
    fun originalMb(ids: Set<Int>) = videos.filter { it.id in ids }.sumOf { it.mb }
    fun estimateMb(ids: Set<Int>, quality: Quality) = (originalMb(ids) * quality.percent + 50) / 100
    fun label(mb: Int) = DuplicateDesignData.label(mb * 1_000_000L)
}
