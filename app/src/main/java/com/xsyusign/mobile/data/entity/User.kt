package com.xsyusign.mobile.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 用户 — 存储学号、加密密码、JWSESSION 以及自动签到设置。
 */
@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "username") val username: String,       // 学号
    @ColumnInfo(name = "password") val password: String,       // AES 加密后的密码
    @ColumnInfo(name = "name") val name: String = "",          // 姓名
    @ColumnInfo(name = "auto_sign") var autoSign: Boolean = false,
    @ColumnInfo(name = "sign_days") var signDays: String = "1,2,3,4,5",  // 0=周日,...,6=周六
    @ColumnInfo(name = "sign_start_time") var signStartTime: String = "18:30",
    @ColumnInfo(name = "sign_end_time") var signEndTime: String = "22:00",
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") var updatedAt: Long = System.currentTimeMillis()
)
