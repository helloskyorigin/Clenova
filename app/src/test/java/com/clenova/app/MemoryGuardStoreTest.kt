package com.clenova.app

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.clenova.app.data.storage.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32])
class MemoryGuardStoreTest {
    @Test fun persistentRulesAreEnforcedAcrossCleanupPlans() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = MemoryGuardStore(context)
        val keep = ScannedMedia(Uri.parse("content://media/external/images/media/1"), "keep.png", 150_000_000, false, "Pictures/")
        val protected = ScannedMedia(Uri.parse("content://media/external/images/media/2"), "Screenshot_guard.png", 150_000_000, false, "Pictures/Screenshots/", verifiedHash = "test", generation = 1, modified = 1)
        try {
            assertTrue(store.add(listOf(protected)))
            val keys = MemoryGuardStore(context).records().keys
            assertTrue(protected.uri in keys)
            val result = MediaScanResult(MediaAccess(true,true,false), 2, listOf(listOf(keep,protected)), listOf(protected), emptyList(), false, largeFiles = listOf(protected), photos = listOf(protected), protectedUris = keys)
            assertTrue(DuplicateTrash.allowed(result.duplicateGroups, keys).isEmpty())
            assertTrue(DuplicateTrash.batch(result.duplicateGroups, keys).isEmpty())
            assertTrue(ScreenshotTrash.eligible(result).isEmpty())
            assertTrue(LargeMediaTrash.eligible(result, false).isEmpty())
            assertTrue(SwipeTrash.eligible(result).isEmpty())
            assertEquals(0L, result.reviewBytes)
            assertTrue(runCatching { store.ensureUnprotected(listOf(protected.uri)) }.isFailure)
            assertTrue(store.remove(protected.uri))
            store.ensureUnprotected(listOf(protected.uri))
        } finally { store.remove(protected.uri) }
    }
}
