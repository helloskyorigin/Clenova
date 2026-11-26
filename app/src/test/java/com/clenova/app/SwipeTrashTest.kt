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
class SwipeTrashTest {
    private fun photo(id: Int, favorite: Boolean = false, generation: Long? = 1L) =
        ScannedMedia(Uri.parse("content://media/external/images/media/$id"), "photo_$id", 100, false, "DCIM/", favorite, generation = generation, modified = 1)
    @Test fun protectionAndUndo() {
        val keep = photo(1); val copy = photo(2); val favorite = photo(3, true); val stale = photo(4, generation = null)
        val result = MediaScanResult(MediaAccess(true,true,false), 4, listOf(listOf(keep, copy)), emptyList(), emptyList(), false, photos = listOf(keep, copy, favorite, stale))
        val allowed = SwipeTrash.eligible(result)
        assertEquals(setOf(copy.uri), allowed)
        assertEquals(listOf(copy), SwipeTrash.plan(result, setOf(copy.uri)))
        assertTrue(runCatching { SwipeTrash.plan(result, setOf(keep.uri)) }.isFailure)
        val initial = SwipeSession()
        assertEquals(initial, initial.decide(keep.uri, true, allowed))
        val session = initial.decide(keep.uri, false, allowed).decide(copy.uri, true, allowed)
        assertEquals(setOf(copy.uri), session.selected)
        assertEquals(2, session.index)
        assertTrue(session.undo().selected.isEmpty())
        assertTrue(session.unmark(copy.uri).selected.isEmpty())
    }
}
