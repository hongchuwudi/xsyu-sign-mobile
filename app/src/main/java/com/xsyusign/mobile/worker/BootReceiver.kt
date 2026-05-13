package com.xsyusign.mobile.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.xsyusign.mobile.util.SettingsManager

/**
 * 开机自启 — 重新调度签到闹钟 + 前台保活服务。
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (Intent.ACTION_BOOT_COMPLETED == intent?.action) {
            Log.i("XSYUSign-Boot", "开机完成，恢复签到调度")
            WorkerScheduler.startPeriodicCheck(context)
            if (SettingsManager.isKeepAliveEnabled(context)) {
                KeepAliveService.start(context)
            }
        }
    }
}
