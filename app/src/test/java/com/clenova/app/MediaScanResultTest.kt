package com.clenova.app

import android.net.Uri
import com.clenova.app.data.storage.MediaAccess
import com.clenova.app.data.storage.MediaScanResult
import com.clenova.app.data.storage.ScannedMedia
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32])
class MediaScanResultTest {
    private fun media(id: Int, bytes: Long, folder: String = "", video: Boolean = false) =
        ScannedMedia(Uri.parse("content://media/external/images/media/$id"), "IMG_$id", bytes, video, folder)

    @Test fun overlapIsCountedOnlyOnce() {
        val keep = media(1, 200)
        val extra = media(2, 200, "Pictures/Screenshots/")
        val video = media(3, 100_000_000, video = true)
        val result = MediaScanResult(MediaAccess(true, true, false), 3, listOf(listOf(keep, extra)), listOf(extra), listOf(video), false)
        assertEquals(200L, result.duplicateBytes)
        assertEquals(100_000_200L, result.reviewBytes)
        assertEquals(listOf(extra), result.extras)
    }

    @Test fun screenshotsAreHeuristicAndNeverVideos() {
        assertTrue(media(1, 100, "Pictures/Screenshots/").screenshot)
        assertFalse(media(2, 100, "Pictures/NotScreenshots/").screenshot)
        assertFalse(media(3, 100, "Pictures/Screenshots/", true).screenshot)
    }

    @Test fun selectedAccessIsNotFullAccess() {
        assertTrue(MediaAccess(false, false, true).any)
        assertFalse(MediaAccess(false, false, true).full)
    }
}
