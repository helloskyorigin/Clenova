package com.clenova.app

import com.clenova.app.core.utilities.ByteFormatter
import org.junit.Assert.assertEquals
import org.junit.Test

class ByteFormatterTest {

    @Test
    fun `formats zero and negative bytes safely`() {
        assertEquals("0 B", ByteFormatter.format(0L))
        assertEquals("0 B", ByteFormatter.format(-50L))
    }

    @Test
    fun `formats bytes kilobytes megabytes and gigabytes`() {
        assertEquals("512 B", ByteFormatter.format(512L))
        assertEquals("15 KB", ByteFormatter.format(15_000L))
        assertEquals("42.5 MB", ByteFormatter.format(42_500_000L))
        assertEquals("64.0 GB", ByteFormatter.format(64_000_000_000L))
        assertEquals("128 GB", ByteFormatter.format(128_000_000_000L))
    }
}
