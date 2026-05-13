package com.xsyusign.mobile.network

import android.util.Log
import com.xsyusign.mobile.util.CryptoUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.Request
import java.net.URLEncoder
import kotlin.random.Random

/**
 * CAS 统一认证登录 — 从 XSYULoginUtil.java 移植为 Kotlin + OkHttp。
 *
 * 流程：
 *   1. GET service → 302 → 获取 CAS 登录页 URL
 *   2. GET CAS 页面 → 解析 execution
 *   3. POST CAS 登录表单 → 302 → 提取 ticket
 *   4. GET service?ticket=xxx → 跟随重定向链 → 获取 JWSESSION cookie
 */
object CasLoginService {

    private const val TAG = "XSYUSign-CAS"
    private const val SERVICE_URL = "https://gwxg.xsyu.edu.cn/basicinfo/mobile/login/casLogin"
    private const val CAS_LOGIN_URL = "https://ids.xsyu.edu.cn/authserver/login"
    private const val CAS_HOST = "ids.xsyu.edu.cn"

    private val client get() = OkHttpProvider.client

    /**
     * 登录并返回 JWSESSION，最多重试 3 次。
     */
    suspend fun login(username: String, rsaEncryptedPassword: String): String? {
        val password = CryptoUtil.decrypt(rsaEncryptedPassword)
        if (password.isNullOrEmpty()) {
            Log.e(TAG, "密码解密失败")
            return null
        }
        Log.d(TAG, "开始CAS登录: $username")

        for (attempt in 1..3) {
            try {
                if (attempt > 1) {
                    val delayMs = 3000L + Random.nextLong(2000)
                    Log.d(TAG, "第 $attempt 次重试, 等待 ${delayMs}ms")
                    delay(delayMs)
                }

                OkHttpProvider.clearCookies()

                val result = withContext(Dispatchers.IO) { doLogin(username, password) }
                if (result != null) {
                    Log.d(TAG, "CAS登录成功, 第${attempt}次尝试")
                    return result
                }
                Log.w(TAG, "第 $attempt 次尝试失败")
            } catch (e: Exception) {
                Log.e(TAG, "第 $attempt 次尝试异常", e)
            }
        }
        Log.e(TAG, "CAS登录最终失败")
        return null
    }

    private suspend fun doLogin(username: String, password: String): String? {
        // --- 第一步：获取 CAS 登录 URL ---
        val casLoginUrl = getCasLoginUrl() ?: run {
            Log.e(TAG, "第一步失败: 无法获取CAS登录URL")
            return null
        }
        Log.d(TAG, "第一步: CAS登录URL = $casLoginUrl")

        // --- 第二步：从 CAS 页面提取 execution ---
        val execution = getExecution(casLoginUrl) ?: run {
            Log.e(TAG, "第二步失败: 无法提取execution")
            return null
        }
        Log.d(TAG, "第二步: execution = $execution")

        // 模拟浏览器延迟
        delay(800 + Random.nextLong(400))

        // --- 第三步：提交 CAS 登录表单 ---
        val ticket = submitCasLogin(username, password, execution) ?: run {
            Log.e(TAG, "第三步失败: CAS登录提交失败")
            return null
        }
        Log.d(TAG, "第三步: ticket = $ticket")

        // --- 第四步：用 ticket 换取 JWSESSION ---
        val jws = getJwsWithTicket(ticket)
        if (jws == null) Log.e(TAG, "第四步失败: 无法获取JWSESSION")
        return jws
    }

    // ==================== 第一步 ====================
    private suspend fun getCasLoginUrl(): String? = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url(SERVICE_URL)
            .get()
            .header("Referer", "https://$CAS_HOST/")
            .build()
        val resp = client.newCall(req).execute()
        Log.d(TAG, "  初始访问响应码: ${resp.code}")
        if (resp.code == 302) {
            val location = resp.header("Location")
            Log.d(TAG, "  重定向到: $location")
            location
        } else {
            Log.w(TAG, "  期望302, 实际收到: ${resp.code}")
            // 打印响应体前300字符帮助排查
            val body = resp.body?.string()?.take(300) ?: ""
            Log.w(TAG, "  响应体: $body")
            null
        }
    }

    // ==================== 第二步 ====================
    private suspend fun getExecution(casUrl: String): String? = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url(casUrl)
            .get()
            .header("Referer", "https://$CAS_HOST/")
            .build()
        val resp = client.newCall(req).execute()
        Log.d(TAG, "  CAS页面响应码: ${resp.code}")

        if (resp.code != 200) {
            Log.w(TAG, "  CAS页面非200, 检查是否重定向")
            if (resp.code == 302) {
                val location = resp.header("Location")
                Log.w(TAG, "  CAS页面302重定向到: $location")
                // 递归跟随一次，获取真正的登录页
                return@withContext location?.let { getExecution(it) }
            }
        }

        val html = resp.body?.string() ?: return@withContext null
        Log.d(TAG, "  CAS页面HTML长度: ${html.length}")
        if (html.length < 100) {
            Log.w(TAG, "  HTML异常短: ${html.take(200)}")
        }
        extractExecution(html)
    }

    // ==================== 第三步 ====================
    private suspend fun submitCasLogin(
        username: String, password: String, execution: String
    ): String? = withContext(Dispatchers.IO) {
        val serviceEncoded = URLEncoder.encode(SERVICE_URL, "UTF-8")
        val body = FormBody.Builder()
            .add("username", username)
            .add("password", password)
            .add("execution", execution)
            .add("_eventId", "submit")
            .add("loginType", "1")
            .add("rememberMe", "true")
            .add("service", SERVICE_URL)
            .build()

        val req = Request.Builder()
            .url(CAS_LOGIN_URL)
            .post(body)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("Origin", "https://$CAS_HOST")
            .header("Referer", "https://$CAS_HOST/authserver/login?service=$serviceEncoded")
            // 浏览器指纹头 — 缺失可能触发风控
            .header("Sec-Ch-Ua", "\"Google Chrome\";v=\"125\", \"Chromium\";v=\"125\", \"Not.A/Brand\";v=\"24\"")
            .header("Sec-Ch-Ua-Mobile", "?0")
            .header("Sec-Ch-Ua-Platform", "\"Windows\"")
            .header("Sec-Fetch-Dest", "document")
            .header("Sec-Fetch-Mode", "navigate")
            .header("Sec-Fetch-Site", "same-origin")
            .header("Upgrade-Insecure-Requests", "1")
            .build()

        Log.d(TAG, "  提交CAS登录表单")
        val resp = client.newCall(req).execute()
        Log.d(TAG, "  CAS登录响应码: ${resp.code}")

        when {
            resp.code == 302 -> {
                val location = resp.header("Location") ?: return@withContext null
                Log.d(TAG, "  CAS登录后重定向: $location")
                extractTicket(location)
            }
            resp.code == 200 -> {
                val html = resp.body?.string() ?: return@withContext null
                Log.d(TAG, "  CAS返回200, 响应前300字符: ${html.take(300)}")

                if (html.contains("验证码") || html.contains("captcha")) {
                    Log.e(TAG, "  需要验证码!")
                    throw RuntimeException("登录需要验证码，请手动登录")
                }

                val ticket = extractTicket(html)
                if (ticket != null) {
                    Log.d(TAG, "  从响应页面提取到ticket: $ticket")
                    return@withContext ticket
                }

                // 提取错误信息
                val errPatterns = listOf("密码错误", "用户名", "账号不存在", "账号或密码")
                for (p in errPatterns) {
                    if (p in html) {
                        Log.e(TAG, "  登录失败: $p")
                    }
                }
                null
            }
            else -> {
                Log.w(TAG, "  意外的响应码: ${resp.code}")
                null
            }
        }
    }

    // ==================== 第四步 ====================
    private suspend fun getJwsWithTicket(ticket: String): String? {
        val encodedTicket = URLEncoder.encode(ticket, "UTF-8")
        val url = "$SERVICE_URL?ticket=$encodedTicket"
        Log.d(TAG, "  使用ticket访问: $url")
        return followRedirect(url, 0)
    }

    private suspend fun followRedirect(url: String, depth: Int): String? {
        if (depth > 5) {
            Log.w(TAG, "  重定向链过长($depth)次, 停止")
            return null
        }

        // 先检查是否已拿到 JWSESSION
        val jws = OkHttpProvider.findJws()
        if (jws != null) {
            Log.d(TAG, "  已获取JWSESSION! depth=$depth")
            return jws
        }

        Log.d(TAG, "  ${if (depth == 0) "访问" else "跟随重定向[$depth]"}: $url")

        val req = Request.Builder()
            .url(url)
            .get()
            .header("Referer", "https://$CAS_HOST/")
            .build()
        val resp = withContext(Dispatchers.IO) { client.newCall(req).execute() }
        Log.d(TAG, "  响应码[$depth]: ${resp.code}")

        // 每次请求后再检查 JWSESSION（可能在 Set-Cookie 中）
        val jwsNow = OkHttpProvider.findJws()
        if (jwsNow != null) {
            Log.d(TAG, "  成功获取JWSESSION!")
            return jwsNow
        }

        if (resp.code == 302) {
            val location = resp.header("Location")
            if (location != null) return followRedirect(location, depth + 1)
        }

        Log.w(TAG, "  重定向链结束, 未找到JWSESSION, depth=$depth, code=${resp.code}")
        return null
    }

    // ---- 正则提取 ----
    private fun extractTicket(text: String?): String? {
        if (text == null) return null
        val regex = Regex("ticket=([^\"&\\s]+)")
        return regex.find(text)?.groupValues?.getOrNull(1)
    }

    private fun extractExecution(html: String?): String? {
        if (html == null) return null
        val re1 = Regex("name=\"execution\" value=\"([^\"]+)\"")
        re1.find(html)?.groupValues?.getOrNull(1)?.let { return it }
        val re2 = Regex("execution\" value=\"([^\"]+)\"")
        return re2.find(html)?.groupValues?.getOrNull(1)
    }
}
