package com.clenova.app

import android.net.Uri
import com.clenova.app.data.storage.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32])
class ScreenshotTrashTest {
    private fun media(id: Int, name: String = "Screenshot_$id", favorite: Boolean = false, hash: String? = "hash", bytes: Long = 100) =
        ScannedMedia(Uri.parse("content://media/external/images/media/$id"), name, bytes, false, "", favorite, hash)
    @Test fun onlyVerifiedScreenshotsAndNoKeeperOrFavorites() {
        val keeper = media(1)
        val copy = media(2)
        val favorite = media(3, favorite = true)
        val unverified = media(4, hash = null)
        val other = media(5, name = "IMG_5")
        val result = MediaScanResult(MediaAccess(true, true, false), 5, listOf(listOf(keeper, copy)),
            listOf(keeper, copy, favorite, unverified, other), emptyList(), false)
        assertEquals(listOf(copy), ScreenshotTrash.eligible(result))
        assertEquals(listOf(copy), ScreenshotTrash.plan(result, setOf(copy.uri)))
        for (item in listOf(keeper, favorite, unverified, other)) assertTrue(runCatching { ScreenshotTrash.plan(result, setOf(item.uri)) }.isFailure)
    }
    @Test fun byteAndCountLimits() {
        val items = (1..101).map { media(it, bytes = 20_000_000) }
        val result = MediaScanResult(MediaAccess(true, true, false), items.size, emptyList(), items, emptyList(), false)
        assertTrue(runCatching { ScreenshotTrash.plan(result, items.map { it.uri }.toSet()) }.isFailure)
        assertTrue(runCatching { ScreenshotTrash.plan(result, items.take(51).map { it.uri }.toSet()) }.isFailure)
        assertEquals(50, ScreenshotTrash.plan(result, items.take(50).map { it.uri }.toSet()).size)
    }
}
