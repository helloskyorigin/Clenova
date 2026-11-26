package com.clenova.app.core.utilities

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

class ClenovaHaptics(
    private val hapticFeedback: HapticFeedback,
    private val enabled: Boolean,
) {
    fun performSelection() {
        if (enabled) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    fun performPrimaryAction() {
        if (enabled) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
}

@Composable
fun rememberClenovaHaptics(enabled: Boolean): ClenovaHaptics {
    val hapticFeedback = LocalHapticFeedback.current
    return remember(hapticFeedback, enabled) {
        ClenovaHaptics(hapticFeedback = hapticFeedback, enabled = enabled)
    }
}
