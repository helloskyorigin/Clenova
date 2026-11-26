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
class LargeMediaTrashTest {
    private fun media(id: Int, video: Boolean = true, bytes: Long = 200_000_000L, favorite: Boolean = false, generation: Long? = 1L) =
        ScannedMedia(Uri.parse("content://media/external/" + (if (video) "video" else "images") + "/media/$id"),
            "file_$id", bytes, video, "DCIM/", favorite, generation = generation, modified = 1L)

    @Test fun protectedAndUnverifiedMediaCannotBeSelected() {
        val keeper = media(1)
        val copy = media(2)
        val photo = media(3, video = false)
        val favorite = media(4, favorite = true)
        val small = media(5, bytes = 10)
        val stale = media(6, generation = null)
        val items = listOf(keeper, copy, photo, favorite, small, stale)
        val result = MediaScanResult(MediaAccess(true,true,false), items.size, listOf(listOf(keeper,copy)), emptyList(),
            items.filter { it.video }, false, largeFiles = items)
        assertEquals(listOf(copy, photo), LargeMediaTrash.eligible(result, false))
        assertEquals(listOf(copy), LargeMediaTrash.eligible(result, true))
        assertTrue(runCatching { LargeMediaTrash.plan(result, setOf(keeper.uri), false) }.isFailure)
        assertTrue(runCatching { LargeMediaTrash.plan(result, setOf(photo.uri), true) }.isFailure)
    }

    @Test fun multiGigabyteMediaIsNotExcludedByHashBudget() {
        val large = media(1, bytes = 4_000_000_000L)
        val result = MediaScanResult(MediaAccess(true,true,false), 1, emptyList(), emptyList(), listOf(large), false)
        assertEquals(listOf(large), LargeMediaTrash.plan(result, setOf(large.uri), true))
        assertEquals(4_000_000_000L, result.reviewBytes)
    }
}
