package com.xsyusign.mobile.worker

import android.content.Context
import android.util.Log
import androidx.work.*
import java.util.concurrent.TimeUnit

/**
 * 签到任务调度器。
 * 定时签到使用 WorkManager PeriodicWorkRequest（官方推荐），手动签到使用 OneTimeWorkRequest。
 */
object WorkerScheduler {

    private const val TAG = "WorkerScheduler"
    private const val PERIODIC_NAME = "sign_periodic"

    /** 启动周期签到巡检（WorkManager，每 15 分钟） */
    fun startPeriodicCheck(context: Context) {
        val request = PeriodicWorkRequestBuilder<SignWorker>(15, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.LINEAR, 5, TimeUnit.MINUTES)
            .addTag("sign_periodic")
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
        Log.i(TAG, "WorkManager 周期签到已启动 (15min)")
    }

    /** 取消周期签到 */
    fun cancelPeriodicCheck(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_NAME)
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
