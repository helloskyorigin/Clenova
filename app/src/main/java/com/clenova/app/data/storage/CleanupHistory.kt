package com.clenova.app.data.storage

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

internal enum class CleanupKind { DUPLICATES, SIMILAR, SCREENSHOTS, LARGE_FILES, LARGE_VIDEOS, SWIPE, COMPRESS }
internal data class CleanupRecord(val kind: CleanupKind, val timestamp: Long, val count: Int, val bytes: Long, val originalBytes: Long = 0)

internal class CleanupHistory(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("clenova_cleanup_history", Context.MODE_PRIVATE)
    fun records(): List<CleanupRecord> = synchronized(lock) {
        val json = runCatching { JSONArray(prefs.getString("records", "[]") ?: "[]") }.getOrElse { JSONArray() }
        (0 until json.length()).mapNotNull { i ->
            runCatching {
                val item = json.getJSONObject(i)
                CleanupRecord(CleanupKind.valueOf(item.getString("kind")), item.getLong("time"),
                    item.getInt("count"), item.getLong("bytes"), item.optLong("original"))
            }.getOrNull()?.takeIf { it.count > 0 && it.bytes >= 0 && it.timestamp > 0 }
        }.take(LIMIT)
    }
    fun record(kind: CleanupKind, count: Int, bytes: Long, originalBytes: Long = 0): Boolean = synchronized(lock) {
        if (count <= 0 || bytes <= 0 || originalBytes < 0 || (kind == CleanupKind.COMPRESS && originalBytes <= bytes)) return false
        val next = (listOf(CleanupRecord(kind, System.currentTimeMillis(), count, bytes, originalBytes)) + records()).take(LIMIT)
        val json = JSONArray()
        next.forEach { item ->
            json.put(JSONObject().put("kind", item.kind.name).put("time", item.timestamp)
                .put("count", item.count).put("bytes", item.bytes).put("original", item.originalBytes))
        }
        prefs.edit().putString("records", json.toString()).commit()
    }
    fun clear(): Boolean = synchronized(lock) { prefs.edit().remove("records").commit() }
    companion object { private val lock = Any(); const val LIMIT = 200 }
}
