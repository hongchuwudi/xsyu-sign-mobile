package com.xsyusign.mobile.worker

import android.content.Context
import android.util.Log
import androidx.work.*
import java.util.concurrent.TimeUnit

/**
 * WorkManager 任务调度器 — 启动 / 取消周期巡检。
 */
object WorkerScheduler {

    private const val TAG = "WorkerScheduler"
    private const val PERIODIC_WORK_NAME = "sign_periodic_check"

    /** 启动周期签到巡检（每 15 分钟） */
    fun startPeriodicCheck(context: Context) {
        val workManager = WorkManager.getInstance(context)
        val request = SignWorker.periodicRequest()

        workManager.enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            androidx.work.ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
        Log.i(TAG, "周期签到巡检已启动 (interval=15min)")
    }

    /** 取消周期签到巡检 */
    fun cancelPeriodicCheck(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WORK_NAME)
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
        val request = SignWorker.oneTimeRequest().let { builder ->
            OneTimeWorkRequestBuilder<SignWorker>()
                .setInputData(workDataOf("sign_all" to true))
                .addTag("sign_manual")
                .build()
        }
        workManager.enqueue(request)
    }
}
