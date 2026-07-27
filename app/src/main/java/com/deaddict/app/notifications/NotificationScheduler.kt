package com.deaddict.app.notifications

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

class NotificationScheduler(
    private val context: Context,
    private val workManager: WorkManager = WorkManager.getInstance(context),
) {
    fun scheduleDailyCheckIn() {
        val now = ZonedDateTime.now()
        var next = now.toLocalDate().atTime(9, 0).atZone(now.zone)
        if (!next.isAfter(now)) next = next.plusDays(1)
        val request = PeriodicWorkRequestBuilder<DailyCheckInWorker>(
            24,
            TimeUnit.HOURS,
            1,
            TimeUnit.HOURS,
        )
            .setInitialDelay(Duration.between(now, next))
            .addTag(LOCAL_NOTIFICATION_TAG)
            .build()
        workManager.enqueueUniquePeriodicWork(
            DAILY_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    fun cancelDailyCheckIn() {
        workManager.cancelUniqueWork(DAILY_WORK)
    }

    companion object {
        const val DAILY_WORK = "daily_private_check_in"
        const val LOCAL_NOTIFICATION_TAG = "local_notifications"
    }
}

