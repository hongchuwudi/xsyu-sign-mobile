package com.xsyusign.mobile.worker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
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

        /** 调度下一次闹钟 */
        fun scheduleNext(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, SignAlarmReceiver::class.java).apply {
                action = ACTION_SIGN
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val triggerTime = System.currentTimeMillis() + INTERVAL_MS
            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent
                )
            } catch (e: SecurityException) {
                try {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                } catch (e2: Exception) {
                    Log.e(TAG, "无法调度闹钟", e2)
                }
            } catch (e: Exception) {
                Log.e(TAG, "调度闹钟失败", e)
            }
            Log.i(TAG, "下次签到已预约: ${triggerTime}")
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
