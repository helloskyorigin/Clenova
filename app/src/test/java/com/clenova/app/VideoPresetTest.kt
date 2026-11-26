package com.clenova.app

import com.clenova.app.data.storage.VideoPreset
import org.junit.Assert.*
import org.junit.Test

class VideoPresetTest {
    @Test fun presetsCapHeightAndBitrateWithoutUpscaling() {
        assertEquals(1080, VideoPreset.HIGH.height(2160))
        assertEquals(720, VideoPreset.BALANCED.height(1080))
        assertEquals(480, VideoPreset.SMALL.height(720))
        assertEquals(360, VideoPreset.HIGH.height(361))
        assertTrue(VideoPreset.HIGH.bitrate(10_000_000) > VideoPreset.BALANCED.bitrate(10_000_000))
        assertTrue(VideoPreset.BALANCED.bitrate(10_000_000) > VideoPreset.SMALL.bitrate(10_000_000))
    }
}
