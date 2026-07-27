package com.deaddict.app.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.deaddict.app.MainActivity
import com.deaddict.app.R
import java.time.Instant
import java.time.ZoneId

class DailyCheckInWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val store = NotificationPreferenceStore(applicationContext)
        val preferences = store.current()
        if (!preferences.dailyCheckInEnabled) return Result.success()
        val now = System.currentTimeMillis()
        val hour = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).hour
        if (isQuietHour(hour, preferences.quietStartHour, preferences.quietEndHour)) {
            return Result.success()
        }
        if (now - preferences.lastDailyShownAtMillis < MINIMUM_GAP_MILLIS) {
            return Result.success()
        }
        if (
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return Result.success()
        }
        NotificationChannels.create(applicationContext)
        val contentIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            Intent(applicationContext, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(
            applicationContext,
            NotificationChannels.CHECK_INS,
        )
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("A private check-in")
            .setContentText("Take a moment for yourself when you’re ready.")
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .build()
        NotificationManagerCompat.from(applicationContext).notify(DAILY_ID, notification)
        store.markDailyShown(now)
        return Result.success()
    }

    private companion object {
        const val DAILY_ID = 1001
        const val MINIMUM_GAP_MILLIS = 20 * 60 * 60 * 1_000L
    }
}

internal fun isQuietHour(hour: Int, startHour: Int, endHour: Int): Boolean {
    require(hour in 0..23 && startHour in 0..23 && endHour in 0..23)
    return when {
        startHour == endHour -> true
        startHour < endHour -> hour in startHour until endHour
        else -> hour >= startHour || hour < endHour
    }
}

