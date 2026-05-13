package com.xsyusign.mobile

import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkManager
import com.xsyusign.mobile.worker.WorkerScheduler

class App : Application(), Configuration.Provider {

    override fun onCreate() {
        super.onCreate()
        // 启动周期签到巡检
        WorkManager.initialize(this, workManagerConfiguration)
        WorkerScheduler.startPeriodicCheck(this)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
}
