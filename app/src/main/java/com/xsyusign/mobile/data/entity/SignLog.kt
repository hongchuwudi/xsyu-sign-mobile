package com.xsyusign.mobile.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 签到日志 — 记录每次签到执行的结果。
 */
@Entity(tableName = "sign_logs")
data class SignLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "user_id") val userId: Long,
    @ColumnInfo(name = "username") val username: String,
    @ColumnInfo(name = "sign_title") val signTitle: String = "",
    @ColumnInfo(name = "result") val result: String = "",     // 成功 / 失败原因
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)
