package com.xsyusign.mobile

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.work.Configuration
import androidx.work.WorkManager
import com.xsyusign.mobile.worker.WorkerScheduler

class App : Application(), Configuration.Provider {

    override fun onCreate() {
        super.onCreate()

        // 初始化 WorkManager（必须在 super.onCreate() 之后）
        WorkManager.initialize(this, workManagerConfiguration)

        // 启动 WorkManager 周期签到巡检
        WorkerScheduler.startPeriodicCheck(this)
        Log.i("hongchu-sign", "App 启动 — WorkManager 周期签到已调度")

        Handler(Looper.getMainLooper()).post {
            Toast.makeText(this, "定时签到已启动", Toast.LENGTH_SHORT).show()
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
}
