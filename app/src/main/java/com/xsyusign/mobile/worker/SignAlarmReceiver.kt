package com.xsyusign.mobile.worker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

/**
 * AlarmManager 驱动的签到调度 — 每次触发后自预约下一次。
 *
 * 流程：闹钟触发 → 加入签到队列 → 调度 15 分钟后的闹钟 → 循环
 */
class SignAlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "XSYUSign-Alarm"
        private const val ACTION_SIGN = "com.xsyusign.mobile.SIGN_ALARM"
        private const val INTERVAL_MS = 15 * 60 * 1000L // 15 分钟

        /** 检查是否有精确闹钟权限（Android 12+） */
        fun canScheduleExactAlarms(context: Context): Boolean {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            return alarmManager.canScheduleExactAlarms()
        }

        /** 跳转到系统闹钟权限设置页 */
        fun openAlarmSettings(context: Context) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = android.net.Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }

        /** 调度下一次闹钟，返回 null=成功，否则返回错误信息 */
        fun scheduleNext(context: Context): String? {
            return try {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

                // Android 12+ 检查精确闹钟权限
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                    return "需要「闹钟和提醒」权限，请点击前往设置开启"
                }

                val intent = Intent(context, SignAlarmReceiver::class.java).apply {
                    action = ACTION_SIGN
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val triggerTime = System.currentTimeMillis() + INTERVAL_MS
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent
                )
                Log.i(TAG, "下次签到已预约: ${triggerTime}")
                null
            } catch (e: SecurityException) {
                "权限不足: ${e.message}"
            } catch (e: Exception) {
                "调度失败: ${e.message}"
            }
        }

        /** 取消所有已调度的闹钟 */
        fun cancel(context: Context) {
            val intent = Intent(context, SignAlarmReceiver::class.java).apply {
                action = ACTION_SIGN
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent)
        }
    }

    override fun onReceive(context: Context, intent: Intent?) {
        Log.i(TAG, "闹钟触发，开始签到流程")

        // 加入 WorkManager 队列做实际签到
        val workManager = WorkManager.getInstance(context)
        val request = OneTimeWorkRequestBuilder<SignWorker>()
            .addTag("sign_alarm")
            .build()
        workManager.enqueue(request)

        // 预约下一次
        scheduleNext(context)
    }
}
