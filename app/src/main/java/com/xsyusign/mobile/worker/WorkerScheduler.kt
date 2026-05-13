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

    /** 启动周期签到巡检：先立刻执行一次，后续每 15 分钟闹钟循环 */
    fun startPeriodicCheck(context: Context) {
        // 立刻检查一次
        val workManager = WorkManager.getInstance(context)
        val firstCheck = OneTimeWorkRequestBuilder<SignWorker>()
            .setInitialDelay(30, TimeUnit.SECONDS)
            .addTag("sign_first_check")
            .build()
        workManager.enqueue(firstCheck)

        // 后续 15 分钟闹钟自循环
        SignAlarmReceiver.scheduleNext(context)
        Log.i(TAG, "AlarmManager 周期签到已启动 (30s后首次检查, 之后15min循环)")
    }

    /** 取消周期签到 */
    fun cancelPeriodicCheck(context: Context) {
        SignAlarmReceiver.cancel(context)
    }

    /** 立即为指定用户执行签到 */
    fun signNow(context: Context, userId: Long? = null, username: String? = null) {
        val workManager = WorkManager.getInstance(context)
        val request = SignWorker.oneTimeRequest(userId = userId, username = username)
        workManager.enqueue(request)
    }

    /** 立即为所有用户执行签到 */
    fun signAllNow(context: Context) {
        val workManager = WorkManager.getInstance(context)
        val request = OneTimeWorkRequestBuilder<SignWorker>()
            .setInputData(workDataOf("sign_all" to true))
            .addTag("sign_manual")
            .build()
        workManager.enqueue(request)
    }
}
