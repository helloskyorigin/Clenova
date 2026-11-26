package com.clenova.app

import com.clenova.app.feature.agent.CleanupDesignPlan
import org.junit.Assert.assertEquals
import org.junit.Test

class CleanupDesignPlanTest {
    @Test fun selectedTotals() {
        assertEquals(4800, CleanupDesignPlan.selectedMb(7))
        assertEquals(4300, CleanupDesignPlan.selectedMb(3))
        assertEquals(0, CleanupDesignPlan.selectedMb(0))
        assertEquals("500 MB", CleanupDesignPlan.spaceLabel(4))
    }
}
