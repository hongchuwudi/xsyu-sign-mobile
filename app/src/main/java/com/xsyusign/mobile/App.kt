package com.xsyusign.mobile

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.work.Configuration
import androidx.work.WorkManager
import com.xsyusign.mobile.util.SettingsManager
import com.xsyusign.mobile.worker.KeepAliveService
import com.xsyusign.mobile.worker.WorkerScheduler

class App : Application(), Configuration.Provider {

    override fun onCreate() {
        super.onCreate()

        // 初始化 WorkManager（必须在 super.onCreate() 之后）
        WorkManager.initialize(this, workManagerConfiguration)

        // 启动周期签到巡检
        val err = WorkerScheduler.startPeriodicCheck(this)
        if (err != null) {
            Log.e("hongchu-sign", "定时签到调度失败: $err")
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(this, "定时签到失败: $err", Toast.LENGTH_LONG).show()
            }
        } else {
            Log.i("hongchu-sign", "App 启动 — 定时签到巡检已调度")
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(this, "定时签到已启动", Toast.LENGTH_SHORT).show()
            }
        }

        // 如果用户开启了前台保活，启动服务
        if (SettingsManager.isKeepAliveEnabled(this)) {
            KeepAliveService.start(this)
            Log.i("hongchu-sign", "前台保活服务已启动")
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
}
