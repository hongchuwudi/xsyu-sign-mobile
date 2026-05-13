package com.xsyusign.mobile.network.dto

import com.google.gson.annotations.SerializedName

/**
 * 签到列表项 — 映射 getMySignLogs 返回的 JSON。
 */
data class SignItem(
    val id: String? = null,
    @SerializedName("signId") val signId: String? = null,
    @SerializedName("signTitle") val signTitle: String? = null,
    @SerializedName("signMode") val signMode: Int? = null,
    @SerializedName("signStatus") val signStatus: Int? = null,  // 1=未签 2=已签
    val start: Long? = null,    // 签到窗口开始时间戳(ms)
    val end: Long? = null,       // 签到窗口结束时间戳(ms)
    val type: Int? = null,       // 0=待处理 1=已完成
    @SerializedName("schoolId") val schoolId: String? = null,
    val latitude: String? = null,
    val longitude: String? = null,
    val area: String? = null,
    @SerializedName("areaId") val areaId: String? = null,
    @SerializedName("areaList") val areaList: List<AreaInfo>? = null,
    val date: String? = null     // 已签到时非 null
)

data class AreaInfo(
    val id: String? = null,
    val name: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val radius: Int? = null,
    val shape: Int? = null       // 0=圆形
)

/**
 * 签到 POST 请求体。
 */
data class SignDto(
    @SerializedName("inArea") val inArea: Int = 1,
    @SerializedName("areaJSON") val areaJSON: String = """{"id":"170002","name":"鄠邑校区"}""",
    val latitude: Double = 34.098273,
    val longitude: Double = 108.656693
)

/**
 * 通用 API 响应。
 */
data class ApiResponse<T>(
    val code: Int = -1,
    val message: String? = null,
    val data: T? = null
)
