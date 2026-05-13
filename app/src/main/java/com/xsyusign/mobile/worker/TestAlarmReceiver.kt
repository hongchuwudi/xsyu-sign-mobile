package com.xsyusign.mobile.worker

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.xsyusign.mobile.MainActivity

/**
 * AlarmManager 触发的 BroadcastReceiver — 系统级闹钟，进程被杀也能收到。
 */
class TestAlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val CHANNEL_ID = "test_alarm_channel"
        const val ACTION_TEST = "com.xsyusign.mobile.TEST_ALARM"

        /** 调度一个 30 秒后的闹钟，返回 null 表示成功，否则返回错误信息 */
        fun schedule(context: Context): String? {
            return try {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val intent = Intent(context, TestAlarmReceiver::class.java).apply {
                    action = ACTION_TEST
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val triggerTime = System.currentTimeMillis() + 30_000L
                try {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent
                    )
                } catch (e: SecurityException) {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }
                null
            } catch (e: Exception) {
                e.message ?: "未知错误"
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent?) {
        android.util.Log.i("XSYUSign-Test", "TestAlarmReceiver.onReceive 触发!")
        showNotification(context)
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

        val clickIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, clickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("定时任务测试成功 ✅")
            .setContentText("你的手机支持后台定时任务，签到功能可以正常使用")
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                "你的手机支持后台定时任务，签到功能可以正常使用。"
            ))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        // Android 13+ 检查通知权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                android.util.Log.w("XSYUSign-Test", "无通知权限，用 Toast 替代")
                android.os.Handler(context.mainLooper).post {
                    android.widget.Toast.makeText(context,
                        "定时任务测试成功！但通知权限未开启，请去设置开启通知",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
                return
            }
        }
        NotificationManagerCompat.from(context).notify(9999, notification)
        android.util.Log.i("XSYUSign-Test", "通知已发送")
    }
}
