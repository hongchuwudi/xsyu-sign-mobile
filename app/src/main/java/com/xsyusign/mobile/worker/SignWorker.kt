package com.xsyusign.mobile.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import com.xsyusign.mobile.MainActivity
import com.xsyusign.mobile.data.db.AppDatabase
import com.xsyusign.mobile.data.entity.SignLog
import com.xsyusign.mobile.data.entity.User
import com.xsyusign.mobile.data.repository.SignLogRepository
import com.xsyusign.mobile.data.repository.UserRepository
import android.util.Log
import com.xsyusign.mobile.network.CasLoginService
import com.xsyusign.mobile.network.SignApiService
import com.xsyusign.mobile.util.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * 签到 Worker — 拉取签到列表、筛选可签项、执行签到。
 *
 * 输入参数（可选，用于单用户即时签到）：
 *   - "user_id": Long  — 指定用户 ID
 *   - "username": String — 指定用户名（与 user_id 二选一）
 */
class SignWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val CHANNEL_ID = "sign_result_channel"
        const val CHANNEL_NAME = "签到通知"

        /** 创建一次性签到任务 */
        fun oneTimeRequest(userId: Long? = null, username: String? = null): OneTimeWorkRequest {
            val builder = OneTimeWorkRequestBuilder<SignWorker>()
            if (userId != null) builder.setInputData(workDataOf("user_id" to userId))
            if (username != null) builder.setInputData(workDataOf("username" to username))
            builder.addTag("sign_manual")
            return builder.build()
        }

        /** 创建周期巡检任务（每 15 分钟检查一次） */
        fun periodicRequest(): PeriodicWorkRequest {
            return PeriodicWorkRequestBuilder<SignWorker>(15, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .addTag("sign_periodic")
                .build()
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val db = AppDatabase.getInstance(applicationContext)
        val userRepo = UserRepository(db.userDao())
        val logRepo = SignLogRepository(db.signLogDao())

        // 确定要签到的用户列表
        val keyValueMap = inputData.keyValueMap
        val users = when {
            "user_id" in keyValueMap -> {
                val id = inputData.getLong("user_id", 0)
                userRepo.getById(id)?.let { listOf(it) } ?: emptyList()
            }
            "username" in keyValueMap -> {
                val name = inputData.getString("username") ?: ""
                userRepo.getByUsername(name)?.let { listOf(it) } ?: emptyList()
            }
            "sign_all" in keyValueMap -> {
                // 手动「全部签到」：所有用户，不论设置
                userRepo.getAll()
            }
            else -> {
                // 自动巡检：筛选开启自动签到且当前在窗口内的用户
                val now = Calendar.getInstance()
                val dayOfWeek = (now.get(Calendar.DAY_OF_WEEK) - 1).toString()
                val currentTime = String.format("%02d:%02d", now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE))

                userRepo.getAutoSignUsers().filter { user ->
                    val days = user.signDays.split(",").map { it.trim() }
                    dayOfWeek in days &&
                            currentTime >= user.signStartTime &&
                            currentTime <= user.signEndTime
                }
            }
        }

        if (users.isEmpty()) return@withContext Result.success()

        for (user in users) {
            processUser(user, userRepo, logRepo)
        }

        Result.success()
    }

    private suspend fun processUser(
        user: User,
        userRepo: UserRepository,
        logRepo: SignLogRepository
    ) {
        // 1. 每次签到前实时 CAS 登录
        val jws = CasLoginService.login(user.username, user.password)
        if (jws == null) {
            Log.e("XSYUSign", "CAS登录失败: ${user.username}")
            logRepo.insert(SignLog(userId = user.id, username = user.username, result = "CAS 登录失败"))
            showNotification(applicationContext, user.name.ifEmpty { user.username }, "CAS 登录失败")
            return
        }
        Log.d("XSYUSign", "CAS登录成功: ${user.username}")

        // 2. 获取签到列表
        val signListResp = SignApiService.getSignList(jws)
        if (signListResp.code != 0 && signListResp.code != 200) {
            logRepo.insert(SignLog(userId = user.id, username = user.username, result = "获取签到列表失败: ${signListResp.message}"))
            showNotification(applicationContext, user.name.ifEmpty { user.username }, "获取签到列表失败")
            return
        }

        val items = signListResp.data ?: emptyList()

        // 3. 筛选可签项：signStatus==1(未签) && type!=1(非已完成) && 在时间窗口内
        val now = System.currentTimeMillis()
        val available = items.filter { item ->
            item.signStatus == 1 &&
                    item.type != 1 &&
                    item.start != null && item.end != null &&
                    now >= item.start && now <= item.end
        }

        if (available.isEmpty()) {
            logRepo.insert(SignLog(userId = user.id, username = user.username, result = "无可签项目"))
            showNotification(applicationContext, user.name.ifEmpty { user.username }, "当前无可签项目，可能已全部签完")
            return
        }

        // 4. 逐个执行签到，每成功一项弹一次通知
        for (item in available) {
            val id = item.id ?: continue
            val signId = item.signId ?: continue
            val schoolId = item.schoolId ?: continue

            val signResult = SignApiService.doSign(jws, id, signId, schoolId)
            val title = item.signTitle ?: signId

            if (signResult.code == 0 || signResult.code == 200) {
                logRepo.insert(SignLog(userId = user.id, username = user.username, signTitle = title, result = "签到成功"))
                showNotification(applicationContext, user.name.ifEmpty { user.username }, "$title 签到成功")
            } else {
                val errMsg = signResult.message ?: "未知错误"
                logRepo.insert(SignLog(userId = user.id, username = user.username, signTitle = title, result = "失败: $errMsg"))
                showNotification(applicationContext, user.name.ifEmpty { user.username }, "$title 签到失败: $errMsg")
            }
        }
    }

    private fun showNotification(context: Context, title: String, body: String) {
        // 用户关闭了通知则静默跳过
        if (!SettingsManager.isNotificationEnabled(context)) return

        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(context).notify((System.currentTimeMillis() % 10000).toInt(), notification)
        } else {
            // 无通知权限时用 Toast 兜底
            android.os.Handler(context.mainLooper).post {
                android.widget.Toast.makeText(context, "$title: $body", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
                description = "签到结果通知"
                enableLights(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
