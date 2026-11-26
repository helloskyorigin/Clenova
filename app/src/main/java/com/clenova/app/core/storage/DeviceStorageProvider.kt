package com.clenova.app.core.storage

import android.app.usage.StorageStatsManager
import android.content.Context
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import com.clenova.app.domain.model.StorageOverview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reads genuine device internal volume metrics without fabricating or simulating numbers.
 */
class DeviceStorageProvider(
    private val context: Context,
) {
    suspend fun readInternalStorage(): StorageOverview? = withContext(Dispatchers.IO) {
        readViaStorageStatsManager() ?: readViaStatFs()
    }

    private fun readViaStorageStatsManager(): StorageOverview? {
        return runCatching {
            val statsManager = context.getSystemService(Context.STORAGE_STATS_SERVICE) as? StorageStatsManager
                ?: return@runCatching null
            val totalBytes = statsManager.getTotalBytes(StorageManager.UUID_DEFAULT)
            val availableBytes = statsManager.getFreeBytes(StorageManager.UUID_DEFAULT)
            buildOverviewIfValid(totalBytes = totalBytes, availableBytes = availableBytes)
        }.getOrNull()
    }

    private fun readViaStatFs(): StorageOverview? {
        return runCatching {
            val dataDir = Environment.getDataDirectory() ?: return@runCatching null
            val statFs = StatFs(dataDir.path)
            val totalBytes = statFs.totalBytes
            val availableBytes = statFs.availableBytes
            buildOverviewIfValid(totalBytes = totalBytes, availableBytes = availableBytes)
        }.getOrNull()
    }

    private fun buildOverviewIfValid(totalBytes: Long, availableBytes: Long): StorageOverview? {
        if (totalBytes <= 0L || availableBytes < 0L) return null
        val clampedAvailable = availableBytes.coerceAtMost(totalBytes)
        val usedBytes = (totalBytes - clampedAvailable).coerceAtLeast(0L)
        return StorageOverview(
            totalBytes = totalBytes,
            availableBytes = clampedAvailable,
            usedBytes = usedBytes,
        )
    }
}
