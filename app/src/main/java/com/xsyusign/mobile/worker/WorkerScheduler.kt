package com.xsyusign.mobile.worker

import android.content.Context
import android.util.Log
import androidx.work.*
import java.util.concurrent.TimeUnit

/**
 * 签到任务调度器。
 * 定时签到使用 AlarmManager 自循环（进程被杀也能恢复），手动签到使用 WorkManager。
 */
object WorkerScheduler {

    private const val TAG = "WorkerScheduler"

    /** 启动周期签到巡检（AlarmManager 自循环），返回 null=成功 */
    fun startPeriodicCheck(context: Context): String? {
        return SignAlarmReceiver.scheduleNext(context)
    }

    /** 取消周期签到巡检 */
    fun cancelPeriodicCheck(context: Context) {
        SignAlarmReceiver.cancel(context)
        Log.d(TAG, "周期签到巡检已取消")
    }

    /** 立即为指定用户执行一次签到 */
    fun signNow(context: Context, userId: Long? = null, username: String? = null) {
        val workManager = WorkManager.getInstance(context)
        val request = SignWorker.oneTimeRequest(userId = userId, username = username)
        workManager.enqueue(request)
    }

    /** 立即为所有用户执行签到（无视自动签到设置） */
    fun signAllNow(context: Context) {
        val workManager = WorkManager.getInstance(context)
        val request = OneTimeWorkRequestBuilder<SignWorker>()
            .setInputData(workDataOf("sign_all" to true))
            .addTag("sign_manual")
            .build()
        workManager.enqueue(request)
    }
}
