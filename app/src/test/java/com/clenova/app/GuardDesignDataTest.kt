package com.clenova.app

import com.clenova.app.feature.guard.GuardDesignData
import org.junit.Assert.assertEquals
import org.junit.Test

class GuardDesignDataTest {
    @Test fun countMatchesSelectedCollections() {
        assertEquals(248, GuardDesignData.count(GuardDesignData.INITIAL_MASK))
        assertEquals(266, GuardDesignData.count(GuardDesignData.INITIAL_MASK or 16))
        assertEquals(0, GuardDesignData.count(0))
        assertEquals(278, GuardDesignData.count(GuardDesignData.ALL_MASK))
    }
}
