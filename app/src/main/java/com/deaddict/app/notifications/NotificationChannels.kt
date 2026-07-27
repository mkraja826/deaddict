package com.deaddict.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

object NotificationChannels {
    const val CHECK_INS = "check_ins"
    const val RISK_REMINDERS = "risk_reminders"
    const val REPORTS = "reports"
    const val FOCUS = "focus"
    const val SECURITY = "security"

    fun create(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannels(
            listOf(
                channel(CHECK_INS, "Check-ins", "Private daily and bedtime reminders"),
                channel(RISK_REMINDERS, "Risk reminders", "Private reminders during chosen risk periods"),
                channel(REPORTS, "Reports", "Weekly progress summaries"),
                channel(FOCUS, "Focus sessions", "Focus-session completion"),
                NotificationChannel(
                    SECURITY,
                    "Security",
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply { description = "Account and security notices" },
            ),
        )
    }

    private fun channel(id: String, name: String, details: String) =
        NotificationChannel(id, name, NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = details
            enableVibration(false)
        }
}

