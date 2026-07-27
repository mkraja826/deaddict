package com.deaddict.app.accessibility

import kotlin.math.roundToInt

/** Consistent TalkBack descriptions for dynamic recovery UI state. */
object AccessibilityText {
    fun progress(label: String, fraction: Double): String {
        require(label.isNotBlank())
        val percent = (fraction.coerceIn(0.0, 1.0) * 100).roundToInt()
        return "$label, $percent percent"
    }

    fun selected(label: String, selected: Boolean): String {
        require(label.isNotBlank())
        return if (selected) "$label, selected" else "$label, not selected"
    }

    fun timer(label: String, remainingSeconds: Long): String {
        require(label.isNotBlank())
        val safeSeconds = remainingSeconds.coerceAtLeast(0)
        val minutes = safeSeconds / 60
        val seconds = safeSeconds % 60
        return when {
            minutes > 0 && seconds > 0 -> "$label, $minutes minutes $seconds seconds remaining"
            minutes > 0 -> "$label, $minutes minutes remaining"
            else -> "$label, $seconds seconds remaining"
        }
    }

    fun streak(days: Int): String {
        val safeDays = days.coerceAtLeast(0)
        return if (safeDays == 1) "1 day streak" else "$safeDays day streak"
    }

    fun privateValue(label: String, reveal: Boolean, value: String): String {
        require(label.isNotBlank())
        return if (reveal) "$label, $value" else "$label, hidden"
    }
}
