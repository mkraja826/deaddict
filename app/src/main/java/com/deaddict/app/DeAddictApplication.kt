package com.deaddict.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import com.deaddict.app.notifications.NotificationChannels

@HiltAndroidApp
class DeAddictApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationChannels.create(this)
    }
}

