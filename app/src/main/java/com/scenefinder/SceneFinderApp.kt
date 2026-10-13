package com.scenefinder

import android.app.Application
import com.scenefinder.service.SessionNotificationManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class SceneFinderApp : Application() {

    @Inject
    lateinit var notificationManager: SessionNotificationManager

    override fun onCreate() {
        super.onCreate()
        notificationManager.start()
    }
}
