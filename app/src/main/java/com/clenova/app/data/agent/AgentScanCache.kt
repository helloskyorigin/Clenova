package com.clenova.app.data.agent

import android.content.Context
import android.os.SystemClock
import com.clenova.app.data.storage.MediaScanResult
import com.clenova.app.data.storage.MemoryGuardStore
import com.clenova.app.data.storage.mediaAccess

/** Short-lived process-only cache. Never persists names, hashes or media URIs. */
internal object AgentScanCache {
    private data class Entry(val result: MediaScanResult, val at: Long)
    private var entry: Entry? = null
    private const val MAX_AGE = 90_000L

    @Synchronized fun put(result: MediaScanResult) {
        // Partial grants may change without a permission flag change.
        entry = if (result.access.full) Entry(result, SystemClock.elapsedRealtime()) else null
    }
    @Synchronized fun take(context: Context): MediaScanResult? {
        val cached = entry ?: return null
        entry = null
        if (SystemClock.elapsedRealtime() - cached.at !in 0..MAX_AGE || mediaAccess(context) != cached.result.access) return null
        return runCatching {
            cached.result.copy(protectedUris = MemoryGuardStore(context).protectedUris(cached.result.allMedia))
        }.getOrNull()
    }
    @Synchronized fun clear() { entry = null }
}
