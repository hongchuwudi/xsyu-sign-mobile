package com.xsyusign.mobile.network

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.xsyusign.mobile.network.dto.ApiResponse
import com.xsyusign.mobile.network.dto.SignDto
import com.xsyusign.mobile.network.dto.SignItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * 签到 API — 从 BaseSignService.java 移植。
 *
 * 三个核心接口：
 *   - getSignList(jws, page, size)    → 获取签到列表
 *   - getSignDetail(jws, signId, schoolId) → 获取单个签到详情
 *   - doSign(jws, id, signId, schoolId, signDto) → 执行签到
 */
object SignApiService {

    private const val BASE_URL = "https://gwxg.xsyu.edu.cn/sign/mobile/receive"
    private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
    private val gson = Gson()
    private val client get() = OkHttpProvider.client

    /**
     * 获取签到列表。
     */
    suspend fun getSignList(jws: String, page: Int = 1, size: Int = 20): ApiResponse<List<SignItem>> {
        return withContext(Dispatchers.IO) {
            val req = Request.Builder()
                .url("$BASE_URL/getMySignLogs?page=$page&size=$size")
                .get()
                .header("JWSESSION", jws)
                .header("Accept", "application/json, text/plain, */*")
                .build()

            val resp = client.newCall(req).execute()
            val body = resp.body?.string() ?: return@withContext ApiResponse(code = -1, message = "响应为空")
            parseApiResponse(body, object : TypeToken<ApiResponse<List<SignItem>>>() {})
                ?: ApiResponse(code = -1, message = "解析失败")
        }
    }

    /**
     * 获取单个签到详情。
     */
    suspend fun getSignDetail(
        jws: String, signId: String, schoolId: String
    ): ApiResponse<SignItem> {
        return withContext(Dispatchers.IO) {
            val req = Request.Builder()
                .url("$BASE_URL/getSignLog?signId=$signId&schoolId=$schoolId")
                .get()
                .header("JWSESSION", jws)
                .header("Accept", "application/json, text/plain, */*")
                .build()

            val resp = client.newCall(req).execute()
            val body = resp.body?.string() ?: return@withContext ApiResponse(code = -1, message = "响应为空")
            parseApiResponse(body, object : TypeToken<ApiResponse<SignItem>>() {})
                ?: ApiResponse(code = -1, message = "解析失败")
        }
    }

    /**
     * 执行签到 — POST doSignByArea。
     *
     * @param id  记录 ID（来自 SignItem.id）
     * @param signId 签到 ID（来自 SignItem.signId）
     * @param schoolId 学校 ID（来自 SignItem.schoolId）
     * @param signDto 签到位置数据
     */
    suspend fun doSign(
        jws: String, id: String, signId: String, schoolId: String, signDto: SignDto = SignDto()
    ): ApiResponse<String> {
        return withContext(Dispatchers.IO) {
            val jsonBody = gson.toJson(signDto)
            val req = Request.Builder()
                .url("$BASE_URL/doSignByArea?id=$id&signId=$signId&schoolId=$schoolId")
                .post(jsonBody.toRequestBody(JSON_MEDIA))
                .header("JWSESSION", jws)
                .header("Content-Type", "application/json; charset=utf-8")
                .header("Accept", "application/json, text/plain, */*")
                .build()

            val resp = client.newCall(req).execute()
            val body = resp.body?.string() ?: return@withContext ApiResponse(code = -1, message = "响应为空")
            parseApiResponse(body, object : TypeToken<ApiResponse<String>>() {})
                ?: ApiResponse(code = -1, message = "解析失败")
        }
    }

    private inline fun <reified T> parseApiResponse(json: String, type: TypeToken<T>): T? {
        return try {
            gson.fromJson(json, type.type)
        } catch (e: Exception) {
            null
        }
    }
}
