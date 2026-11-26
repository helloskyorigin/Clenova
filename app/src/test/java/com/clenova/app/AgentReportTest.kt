package com.clenova.app

import android.net.Uri
import com.clenova.app.data.agent.AgentReport
import com.clenova.app.data.storage.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32])
class AgentReportTest {
    @Test fun reportDoesNotDoubleCountOrIncludeProtectedCopies() {
        val first = ScannedMedia(Uri.parse("content://media/external_primary/images/media/1"), "Screenshot_a.png", 200, false, "Pictures/Screenshots/", verifiedHash = "hash")
        val second = first.copy(uri = Uri.parse("content://media/external_primary/images/media/2"))
        val third = first.copy(uri = Uri.parse("content://media/external_primary/images/media/3"))
        val result = MediaScanResult(MediaAccess(true,true,false), 3, listOf(listOf(first,second,third)), listOf(first,second,third), emptyList(), false, protectedUris = setOf(third.uri))
        val report = AgentReport.from(result, 1000, 1234)
        assertEquals(1, report.duplicateCount)
        assertEquals(0, report.screenshotCount)
        assertEquals(200L, report.reviewBytes)
        assertEquals(1000L, report.availableBytes)
    }
}
