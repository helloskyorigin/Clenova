package com.clenova.app.core.utilities

import java.util.Locale
import kotlin.math.ln
import kotlin.math.pow

/**
 * Deterministic decimal SI byte formatter (1 GB = 1,000,000,000 bytes),
 * aligned with Android Storage Settings conventions.
 */
object ByteFormatter {
    private val units = arrayOf("B", "KB", "MB", "GB", "TB")

    fun format(bytes: Long, locale: Locale = Locale.US): String {
        if (bytes <= 0L) return "0 B"
        if (bytes < 1000L) return "$bytes B"

        val exp = (ln(bytes.toDouble()) / ln(1000.0)).toInt().coerceIn(0, units.lastIndex)
        val value = bytes / 1000.0.pow(exp.toDouble())
        val pattern = if (value >= 100.0 || exp <= 1) "%.0f %s" else "%.1f %s"
        return String.format(locale, pattern, value, units[exp])
    }
}
