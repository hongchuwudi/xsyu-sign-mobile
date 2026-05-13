package com.xsyusign.mobile.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import com.xsyusign.mobile.MainActivity
import java.util.concurrent.TimeUnit

/**
 * 定时任务测试 Worker — 延迟 30 秒后发一条通知，
 * 用于验证手机是否允许后台定时任务执行，不涉及任何网络请求。
 */
class TestWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    companion object {
        private const val CHANNEL_ID = "test_channel"
        private const val TAG = "XSYUSign-Test"

        fun oneTimeRequest(): OneTimeWorkRequest {
            return OneTimeWorkRequestBuilder<TestWorker>()
                .setInitialDelay(30, TimeUnit.SECONDS)
                .addTag("test_periodic")
                .build()
        }
    }

    override fun doWork(): Result {
        Log.i(TAG, "测试任务开始执行")

        // 休眠模拟定时触发
        try {
            Thread.sleep(500)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }

        showNotification(applicationContext)
        Log.i(TAG, "测试通知已发送")
        return Result.success()
    }

    private fun showNotification(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "定时测试",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "定时任务测试通知" }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("定时任务测试成功 ✅")
            .setContentText("你的手机支持后台定时任务，签到功能可以正常使用")
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                "你的手机支持后台定时任务，签到功能可以正常使用。\n如果退出 App 后未收到此通知，请检查电池优化和自启动设置。"
            ))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(context).notify(9999, notification)
        }
    }
}
