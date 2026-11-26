package com.clenova.app.domain.model

/**
 * Snapshot of real device internal storage metrics.
 */
data class StorageOverview(
    val totalBytes: Long,
    val availableBytes: Long,
    val usedBytes: Long,
) {
    val usedFraction: Float
        get() = if (totalBytes > 0L) {
            (usedBytes.toDouble() / totalBytes.toDouble()).toFloat().coerceIn(0f, 1f)
        } else {
            0f
        }
}

sealed interface StorageState {
    data object Loading : StorageState
    data class Ready(val overview: StorageOverview) : StorageState
    data object Unavailable : StorageState
}

/**
 * Explicit representation of Autopilot lifecycle.
 * Phase 0 strictly remains in [NotConfigured].
 */
enum class AutopilotStatus {
    NotConfigured,
    Paused,
    AwaitingApproval,
}

enum class ThemePreference {
    LIGHT,
    DARK,
    SYSTEM,
}

data class UserPreferences(
    val themePreference: ThemePreference = ThemePreference.LIGHT,
    val hapticsEnabled: Boolean = true,
    val autopilotStatus: AutopilotStatus = AutopilotStatus.NotConfigured,
    val storageGoalGb: Int = 20,
    val checkIntervalDays: Int = 1,
)
