package com.clenova.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.clenova.app.data.storage.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32])
class CleanupHistoryTest {
    @Test fun persistsOnlyValidRecordsAndClearsHistory() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val history = CleanupHistory(context)
        assertTrue(history.clear())
        assertTrue(history.records().isEmpty())
        assertFalse(history.record(CleanupKind.SWIPE, 0, 0))
        assertFalse(history.record(CleanupKind.COMPRESS, 1, 200, 100))
        assertTrue(history.record(CleanupKind.SCREENSHOTS, 2, 200))
        assertTrue(history.record(CleanupKind.COMPRESS, 1, 50, 100))
        val records = CleanupHistory(context).records()
        assertEquals(listOf(CleanupKind.COMPRESS, CleanupKind.SCREENSHOTS), records.map { it.kind })
        assertEquals(100L, records.first().originalBytes)
        assertEquals(2, records.last().count)
        assertTrue(history.clear())
        assertTrue(CleanupHistory(context).records().isEmpty())
    }
}
