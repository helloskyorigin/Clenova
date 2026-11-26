package com.clenova.app

import com.clenova.app.feature.storage.StorageDesignData
import org.junit.Assert.assertEquals
import org.junit.Test

class StorageDesignDataTest {
    @Test
    fun `category totals match used capacity`() {
        val overview = StorageDesignData.state.overview
        assertEquals(overview.usedBytes, StorageDesignData.categories.sumOf { it.bytes })
        assertEquals(overview.totalBytes, overview.usedBytes + overview.availableBytes)
    }
}
