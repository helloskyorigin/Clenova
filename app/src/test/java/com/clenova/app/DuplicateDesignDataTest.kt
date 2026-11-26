package com.clenova.app

import com.clenova.app.feature.clean.DuplicateDesignData
import org.junit.Assert.assertEquals
import org.junit.Test

class DuplicateDesignDataTest {
    @Test fun oneCopyAlwaysExcludedAndTotalsMatchPlan() {
        val data = DuplicateDesignData
        assertEquals(108, data.removableIds.size)
        assertEquals(1_200_000_000L, data.selectedBytes(data.removableIds))
        assertEquals(0L, data.selectedBytes(data.groups.map { it.id * 10 }))
        assertEquals(12_500_000L, data.selectedBytes(listOf(1, 1, 0)))
    }
}
