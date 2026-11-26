package com.clenova.app

import android.net.Uri
import com.clenova.app.data.storage.DuplicateTrash
import com.clenova.app.data.storage.ScannedMedia
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32])
class DuplicateTrashTest {
    private fun media(id: Int, bytes: Long = 100, favorite: Boolean = false) =
        ScannedMedia(Uri.parse("content://media/external/images/media/$id"), "IMG_$id", bytes, false, "", favorite, "hash")
    @Test fun keeperAndFavoritesNeverAllowed() {
        val keep = media(1)
        val favorite = media(2, favorite = true)
        val extra = media(3)
        val groups = listOf(listOf(keep, favorite, extra))
        assertEquals(setOf(extra.uri), DuplicateTrash.allowed(groups))
        assertEquals(setOf(extra.uri), DuplicateTrash.batch(groups))
        assertEquals(listOf(keep to listOf(extra)), DuplicateTrash.plan(groups, setOf(extra.uri)))
        assertTrue(runCatching { DuplicateTrash.plan(groups, setOf(keep.uri)) }.isFailure)
        assertTrue(runCatching { DuplicateTrash.plan(groups, setOf(favorite.uri)) }.isFailure)
    }
    @Test fun batchRespectsCountAndVerificationBudget() {
        val group = (1..150).map { media(it) }
        assertEquals(100, DuplicateTrash.batch(listOf(group)).size)
        val large = listOf(media(1, 600_000_000), media(2, 600_000_000))
        assertTrue(DuplicateTrash.batch(listOf(large)).isEmpty())
        assertTrue(runCatching { DuplicateTrash.plan(listOf(large), setOf(large[1].uri)) }.isFailure)
    }
}
