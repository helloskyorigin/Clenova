package com.clenova.app.data.agent

import android.content.Context
import android.content.SharedPreferences
import com.clenova.app.data.storage.*
import org.json.JSONObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

internal data class AgentReport(
    val checkedAt: Long,
    val availableBytes: Long,
    val duplicateCount: Int,
    val duplicateBytes: Long,
    val screenshotCount: Int,
    val screenshotBytes: Long,
    val videoCount: Int,
    val videoBytes: Long,
    val partial: Boolean,
    val problem: String = "",
) {
    val reviewBytes get() = duplicateBytes + screenshotBytes + videoBytes
    companion object {
        fun from(result: MediaScanResult, available: Long, now: Long): AgentReport {
            val used = mutableSetOf<android.net.Uri>()
            val allowed = DuplicateTrash.allowed(result.duplicateGroups, result.protectedUris)
            val duplicates = result.duplicateGroups.flatMap { it.drop(1) }
                .filter { it.uri in allowed && used.add(it.uri) }
            val screenshots = ScreenshotTrash.eligible(result).filter { used.add(it.uri) }
            val videos = LargeMediaTrash.eligible(result, true).filter { used.add(it.uri) }
            return AgentReport(now, available, duplicates.size, duplicates.sumOf { it.bytes },
                screenshots.size, screenshots.sumOf { it.bytes }, videos.size, videos.sumOf { it.bytes },
                result.incomplete || !result.access.full)
        }
        fun problem(reason: String) = AgentReport(System.currentTimeMillis(), -1, 0, 0, 0, 0, 0, 0, true, reason)
    }
}

internal class AgentReportStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("agent_report", Context.MODE_PRIVATE)
    fun read(): AgentReport? = runCatching {
        val j = JSONObject(prefs.getString("report", null) ?: return null)
        AgentReport(j.getLong("time"), j.getLong("available"), j.getInt("dc"), j.getLong("db"),
            j.getInt("sc"), j.getLong("sb"), j.getInt("vc"), j.getLong("vb"), j.getBoolean("partial"), j.optString("problem"))
    }.getOrNull()
    fun save(r: AgentReport) {
        val j = JSONObject().put("time", r.checkedAt).put("available", r.availableBytes)
            .put("dc", r.duplicateCount).put("db", r.duplicateBytes).put("sc", r.screenshotCount)
            .put("sb", r.screenshotBytes).put("vc", r.videoCount).put("vb", r.videoBytes)
            .put("partial", r.partial).put("problem", r.problem)
        prefs.edit().putString("report", j.toString()).apply()
    }
    fun clear() { prefs.edit().remove("report").apply() }
    fun observe(): Flow<AgentReport?> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> trySend(read()) }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(read())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
}
