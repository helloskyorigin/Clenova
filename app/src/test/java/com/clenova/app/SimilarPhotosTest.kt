package com.clenova.app

import com.clenova.app.data.storage.SimilarPhotos
import org.junit.Assert.*
import org.junit.Test

class SimilarPhotosTest {
    private fun pixel(r: Int, g: Int, b: Int) = (255 shl 24) or (r shl 16) or (g shl 8) or b
    @Test fun sameStructureSmallBrightnessChangeMatches() {
        val a = IntArray(81) { i -> pixel(40 + i, 40 + i, 40 + i) }
        val b = IntArray(81) { i -> pixel(50 + i, 50 + i, 50 + i) }
        assertTrue(SimilarPhotos.matches(SimilarPhotos.fingerprint(a, 1.0), SimilarPhotos.fingerprint(b, 1.0)))
    }
    @Test fun reversedStructureAndDifferentColorDoNotMatch() {
        val a = IntArray(81) { i -> pixel(40 + i, 40 + i, 40 + i) }
        val b = IntArray(81) { i -> pixel(120 - i, 120 - i, 120 - i) }
        val c = IntArray(81) { i -> pixel(40 + i, 150 + i, 40 + i) }
        val fp = SimilarPhotos.fingerprint(a, 1.0)
        assertFalse(SimilarPhotos.matches(fp, SimilarPhotos.fingerprint(b, 1.0)))
        assertFalse(SimilarPhotos.matches(fp, SimilarPhotos.fingerprint(c, 1.0)))
        assertFalse(SimilarPhotos.matches(fp, SimilarPhotos.fingerprint(a, 1.5)))
    }
}
