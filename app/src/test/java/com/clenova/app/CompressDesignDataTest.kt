package com.clenova.app

import com.clenova.app.feature.clean.CompressDesignData
import org.junit.Assert.assertEquals
import org.junit.Test

class CompressDesignDataTest {
    @Test fun estimatesRespectSelectionAndQuality() {
        val d = CompressDesignData
        assertEquals(840, d.originalMb(setOf(0)))
        assertEquals(420, d.estimateMb(setOf(0), CompressDesignData.Quality.BALANCED))
        assertEquals(630, d.estimateMb(setOf(0), CompressDesignData.Quality.HIGH))
        assertEquals(252, d.estimateMb(setOf(0), CompressDesignData.Quality.SMALL))
        assertEquals(0, d.estimateMb(emptySet(), CompressDesignData.Quality.BALANCED))
    }
}
