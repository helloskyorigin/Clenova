package com.clenova.app.data.storage

import android.content.ContentResolver
import android.graphics.Bitmap
import android.os.Build
import android.util.Size
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlin.math.abs

/** Bounded on-device comparison; results are suggestions, never exact duplicates. */
internal object SimilarPhotos {
    private const val MAX_PHOTOS = 1200
    private const val VERIFY_BUDGET = 1_000_000_000L
    data class Fingerprint(val horizontal: Long, val vertical: Long, val red: Int, val green: Int, val blue: Int, val aspect: Double)
    data class Result(val groups: List<List<ScannedMedia>>, val incomplete: Boolean)
    fun fingerprint(pixels: IntArray, aspect: Double): Fingerprint {
        require(pixels.size == 81)
        val luminance = pixels.map { p -> (77 * ((p ushr 16) and 255) + 150 * ((p ushr 8) and 255) + 29 * (p and 255)) shr 8 }
        var horizontal = 0L
        var vertical = 0L
        for (y in 0..7) for (x in 0..7) {
            val bit = 1L shl (y * 8 + x)
            val i = y * 9 + x
            if (luminance[i] > luminance[i + 1]) horizontal = horizontal or bit
            if (luminance[i] > luminance[i + 9]) vertical = vertical or bit
        }
        return Fingerprint(horizontal, vertical, pixels.sumOf { (it ushr 16) and 255 } / 81,
            pixels.sumOf { (it ushr 8) and 255 } / 81, pixels.sumOf { it and 255 } / 81, aspect)
    }
    fun matches(a: Fingerprint, b: Fingerprint): Boolean =
        abs(a.aspect - b.aspect) <= .12 &&
            abs(a.red - b.red) <= 22 && abs(a.green - b.green) <= 22 && abs(a.blue - b.blue) <= 22 &&
            java.lang.Long.bitCount(a.horizontal xor b.horizontal) <= 6 &&
            java.lang.Long.bitCount(a.vertical xor b.vertical) <= 6
    private fun read(resolver: ContentResolver, media: ScannedMedia): Fingerprint {
        val bitmap = resolver.loadThumbnail(media.uri, Size(96, 96), null)
        try {
            val tiny = Bitmap.createScaledBitmap(bitmap, 9, 9, true)
            try {
                val pixels = IntArray(81)
                tiny.getPixels(pixels, 0, 9, 0, 0, 9, 9)
                return fingerprint(pixels, bitmap.width.toDouble() / bitmap.height)
            } finally { if (tiny !== bitmap) tiny.recycle() }
        } finally { bitmap.recycle() }
    }
    suspend fun find(resolver: ContentResolver, photos: List<ScannedMedia>,
        progress: suspend (Int, Int, Int) -> Unit, hash: suspend (ScannedMedia) -> String): Result {
        if (Build.VERSION.SDK_INT < 29) return Result(emptyList(), photos.isNotEmpty())
        val candidates = photos.takeLast(MAX_PHOTOS)
        var incomplete = photos.size > candidates.size
        val buckets = mutableListOf<MutableList<Pair<ScannedMedia, Fingerprint>>>()
        progress(3, 0, candidates.size)
        candidates.forEachIndexed { index, media ->
            currentCoroutineContext().ensureActive()
            try {
                val fp = read(resolver, media)
                val group = buckets.firstOrNull { bucket -> bucket.all { matches(it.second, fp) } }
                if (group == null) buckets.add(mutableListOf(media to fp)) else group.add(media to fp)
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { incomplete = true }
            progress(3, index + 1, candidates.size)
        }
        var budget = VERIFY_BUDGET
        val verified = mutableListOf<List<ScannedMedia>>()
        for (bucket in buckets.filter { it.size > 1 }) {
            currentCoroutineContext().ensureActive()
            val media = bucket.map { it.first }.sortedWith(compareByDescending<ScannedMedia> { it.favorite }.thenBy { it.uri.toString() })
            val cost = media.sumOf { it.bytes }
            if (cost > budget) { incomplete = true; continue }
            budget -= cost
            try { verified.add(media.map { it.copy(verifiedHash = hash(it)) }) }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { incomplete = true }
        }
        return Result(verified, incomplete)
    }
}
