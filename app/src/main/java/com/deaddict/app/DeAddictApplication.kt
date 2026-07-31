package com.deaddict.app

import android.app.Application
import com.deaddict.app.health.AppStabilityMonitor
import com.deaddict.app.notifications.NotificationChannels
import com.deaddict.app.sync.SyncScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class DeAddictApplication : Application() {
    @Inject lateinit var syncScheduler: SyncScheduler
    @Inject lateinit var appStabilityMonitor: AppStabilityMonitor

    override fun onCreate() {
        super.onCreate()
        appStabilityMonitor.start()
        NotificationChannels.create(this)
        syncScheduler.ensurePeriodic()
        syncScheduler.scheduleNow()
    }
}
