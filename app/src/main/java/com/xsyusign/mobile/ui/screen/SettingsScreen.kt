package com.xsyusign.mobile.ui.screen

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.xsyusign.mobile.util.SettingsManager
import com.xsyusign.mobile.worker.SignAlarmReceiver
import com.xsyusign.mobile.worker.TestAlarmReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var notifyEnabled by remember { mutableStateOf(SettingsManager.isNotificationEnabled(context)) }
    var showGuide by remember { mutableStateOf(SettingsManager.isShowGuide(context)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("通知", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            ListItem(
                headlineContent = { Text("签到结果通知") },
                supportingContent = { Text("签到成功或失败时弹出通知") },
                leadingContent = { Icon(Icons.Filled.Notifications, contentDescription = null) },
                trailingContent = {
                    Switch(
                        checked = notifyEnabled,
                        onCheckedChange = {
                            notifyEnabled = it
                            SettingsManager.setNotificationEnabled(context, it)
                        }
                    )
                }
            )

            ListItem(
                headlineContent = { Text("首页保活引导") },
                supportingContent = { Text("在首页显示锁定后台、电池优化等设置说明") },
                leadingContent = { Icon(Icons.Filled.Info, contentDescription = null) },
                trailingContent = {
                    Switch(
                        checked = showGuide,
                        onCheckedChange = {
                            showGuide = it
                            SettingsManager.setShowGuide(context, it)
                        }
                    )
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text("定时任务", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            // 闹钟权限状态
            val canSchedule = remember {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    val am = context.getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager
                    am.canScheduleExactAlarms()
                } else true
            }
            if (!canSchedule) {
                ListItem(
                    headlineContent = { Text("闹钟权限未开启", color = MaterialTheme.colorScheme.error) },
                    supportingContent = { Text("定时签到需要此权限，点击前往系统设置开启") },
                    leadingContent = { Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    modifier = Modifier.clickable {
                        SignAlarmReceiver.openAlarmSettings(context)
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            }

            var isTesting by remember { mutableStateOf(false) }
            val scope = rememberCoroutineScope()
            ListItem(
                headlineContent = { Text("测试定时任务") },
                supportingContent = {
                    Text(if (isTesting) "30 秒后将收到通知…" else "请勿清理后台，保持 App 运行 30 秒后查看通知")
                },
                leadingContent = { Icon(Icons.Filled.PlayArrow, contentDescription = null) },
                trailingContent = {
                    if (isTesting) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        TextButton(onClick = {
                            isTesting = true
                            scope.launch {
                                val err = withContext(Dispatchers.IO) {
                                    TestAlarmReceiver.schedule(context)
                                }
                                isTesting = false
                                if (err != null) {
                                    Toast.makeText(context, "调度失败: $err", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "已调度，请不要清理后台，30 秒后查看通知", Toast.LENGTH_LONG).show()
                                }
                            }
                        }) { Text("运行") }
                    }
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text("网页版", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://xsyusign.hongchu.xyz")))
                    }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("xsyusign.hongchu.xyz", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline)
                    }
                    Spacer(Modifier.height(12.dp))
                    Row {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("✅ 全自动托管", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            Text("✅ 无需手机在线", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            Text("✅ 无需任何配置", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            Text("⚠ 账户需交给 hongchu 保管", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("✅ 账户本地存储", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            Text("✅ 数据不上传服务器", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            Text("⚠ 需自己配置签到计划", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                            Text("⚠ 手机需保持开机", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "hongchu.xyz 域名到期后网页版将停止运营，届时请使用本 App 替代。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "注意：小米/OPPO/vivo/华为等机型对后台管控严格，可能导致定时签到失效。首次使用请到「设置 → 测试定时任务」验证。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text("自部署方案", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            var showCode by remember { mutableStateOf(false) }
            val scope2 = rememberCoroutineScope()
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "有服务器或电脑常开？复制下面代码保存为 XSYUOneKeySign.java，" +
                        "改学号密码后配合 cron 定时执行即可，无需 App。",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(8.dp))
                    if (!showCode) {
                        OutlinedButton(onClick = { showCode = true }) {
                            Text("查看源码")
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("XSYUOneKeySign.java", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            TextButton(onClick = {
                                val clip = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                clip.setPrimaryClip(android.content.ClipData.newPlainText("code", ONE_KEY_SIGN_CODE))
                                Toast.makeText(context, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("一键复制")
                            }
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                ONE_KEY_SIGN_CODE,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .heightIn(max = 400.dp)
                                    .verticalScroll(rememberScrollState()),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text("关于", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/hongchuwudi/xsyu-sign-mobile")))
                    }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Code, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("开源地址", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        Text("github.com/hongchuwudi/xsyu-sign-mobile",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline)
                        Spacer(Modifier.height(4.dp))
                        Text("如果觉得好用，欢迎给个 Star ⭐", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

private val ONE_KEY_SIGN_CODE = """
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 西安石油大学一键签到工具
 *
 * @author hongchuwudi
 * @since 2026.02.02
 *
 * 使用方法：直接运行 main 方法，输入学号密码，自动完成登录和签到。
 */
public class XSYUOneKeySign {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    // ---------- 用户名和密码 ----------
    // 用户名
    private static final String username = "my-username";
    // 密码
    private static final String password = "my-password";

    // ---------- 常量配置 ----------
    // User-agent
    private static final String MOBILE_UA = "Mozilla/5.0 (Linux; Android 13; SM-G991B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Mobile Safari/537.36";
    // HTTPClient
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    // 签到接口基础地址
    private static final String BASE_SIGN_URL = "https://gwxg.xsyu.edu.cn/sign/mobile/receive";
    // 签到列表接口
    private static final String GET_SIGN_LIST_URL = BASE_SIGN_URL + "/getMySignLogs?page=1&size=20";
    // 执行签到接口模板
    private static final String DO_SIGN_URL_TEMPLATE = BASE_SIGN_URL + "/doSignByArea?id=%s&signId=%s&schoolId=%s";

    // 签到位置信息（鄠邑校区）
    private static final String IN_AREA = "1";
    private static final String AREA_JSON = "{\\\"id\\\":\\\"170002\\\",\\\"name\\\":\\\"鄠邑校区\\\"}";
    private static final String LATITUDE = "34.098273";
    private static final String LONGITUDE = "108.656693";

    // cas-login-常量
    private static final String SERVICE_CAS_LOGIN = "https://gwxg.xsyu.edu.cn/basicinfo/mobile/login/casLogin";
    private static final String CAS_LOGIN_URL = "https://ids.xsyu.edu.cn/authserver/login";
    private static final String CAS_HOST = "ids.xsyu.edu.cn";
    private static final String UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36";

    public static void main(String[] args) {
        try {
            boolean success = oneKeySign(username, password);
            System.out.println(success ? "\n--> 一键签到全部完成！" : "\n--> 签到过程中出现问题，请查看上方日志。");
        } catch (Exception e) {
            System.err.println("--> 发生异常：");
            e.printStackTrace();
        }
    }

    /**
     * 一键签到主流程
     * @return true 表示所有可签到项均成功，false 表示有失败或异常
     */
    public static boolean oneKeySign(String username, String password) throws Exception {
        // 1. 登录获取 JWSESSION
        System.out.println("\n--> 正在登录统一认证系统...");
        String jwsession = XSYULoginUtil.login(username, password);
        if (jwsession == null || jwsession.isEmpty()) {
            System.err.println("--> 登录失败，无法获取 JWSESSION");
            return false;
        }
        System.out.println("--> 登录成功，JWSESSION: " + jwsession);

        // 2. 获取签到列表
        System.out.println("\n--> 正在获取签到列表...");
        List<SignItemCore> signItems = fetchSignList(jwsession);
        if (signItems.isEmpty()) {
            System.out.println("--> 没有需要签到的项目（可能均已签到或不在签到时间内）");
            return true;
        }
        System.out.println("--> 共获取到 " + signItems.size() + " 个待签到项：");
        for (SignItemCore item : signItems) {
            System.out.printf("    - %s (id=%s, signId=%s, schoolId=%s)\n",
                    item.signName, item.id, item.signId, item.schoolId);
        }

        // 3. 逐个执行签到
        System.out.println("\n--> 开始执行签到...");
        int successCount = 0;
        int total = signItems.size();
        for (SignItemCore item : signItems) {
            System.out.printf("\n--> 正在签到：%s\n", item.signName);
            boolean ok = executeSign(jwsession, item);
            if (ok) {
                successCount++;
                System.out.println("    [成功]");
            } else {
                System.out.println("    [失败]");
            }
        }

        System.out.printf("\n--> 签到结束：成功 %d / %d\n", successCount, total);
        return successCount == total;
    }

    /**
     * 调用签到列表接口，过滤出待签到且有效的项
     */
    private static List<SignItemCore> fetchSignList(String jwsession) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GET_SIGN_LIST_URL))
                .header("User-Agent", MOBILE_UA)
                .header("Accept", "application/json, text/plain, */*")
                .header("JWSESSION", jwsession)
                .GET()
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            System.err.println("--> 获取签到列表失败，HTTP状态码: " + response.statusCode());
            return Collections.emptyList();
        }

        JsonNode root = MAPPER.readTree(response.body());
        int code = root.path("code").asInt();
        String message = root.path("message").asText();
        if (code != 0 && code != 200) {
            System.err.println("--> 接口返回错误：code=" + code + ", message=" + message);
            return Collections.emptyList();
        }

        JsonNode dataNode = root.path("data");
        if (!dataNode.isArray()) {
            System.err.println("--> data 字段不是数组或不存在");
            return Collections.emptyList();
        }

        List<SignItemCore> validItems = new ArrayList<>();
        long now = System.currentTimeMillis();

        for (JsonNode itemNode : dataNode) {
            String id = itemNode.path("id").asText();
            String signId = itemNode.path("signId").asText();
            String schoolId = itemNode.path("schoolId").asText();
            String signName = itemNode.path("signTitle").asText();
            int signStatus = itemNode.path("signStatus").asInt();
            long start = itemNode.path("start").asLong();
            long end = itemNode.path("end").asLong();

            boolean isUnsigned = (signStatus == 1);
            boolean inTime = (now >= start && now <= end);

            if (isUnsigned && inTime) {
                SignItemCore core = new SignItemCore();
                core.id = id;
                core.signId = signId;
                core.schoolId = schoolId;
                core.signName = signName;
                validItems.add(core);
            } else {
                System.out.printf("    [跳过] %s（状态=%d, 有效期内=%b）\n", signName, signStatus, inTime);
            }
        }
        return validItems;
    }

    /**
     * 执行单个签到（POST 请求）
     */
    private static boolean executeSign(String jwsession, SignItemCore item) throws IOException, InterruptedException {
        String url = String.format(DO_SIGN_URL_TEMPLATE, item.id, item.signId, item.schoolId);

        String requestBody = String.format(
                "{\"inArea\":%s,\"areaJSON\":\"%s\",\"latitude\":%s,\"longitude\":%s}",
                IN_AREA, AREA_JSON, LATITUDE, LONGITUDE
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", MOBILE_UA)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json, text/plain, */*")
                .header("JWSESSION", jwsession)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            System.err.println("--> 签到请求失败，HTTP状态码: " + response.statusCode());
            return false;
        }

        JsonNode root = MAPPER.readTree(response.body());
        int code = root.path("code").asInt();
        String message = root.path("message").asText();

        boolean success = (code == 0 || code == 200);
        if (!success) {
            System.err.println("--> 签到接口返回错误：code=" + code + ", message=" + message);
        }
        return success;
    }

    // ---------- 内部数据类 ----------
    static class SignItemCore {
        String id;          // id
        String signId;      // 签到id
        String schoolId;    // 学校id
        String signName;    // 签到记录名称
    }

    /**
     * 西安石油大学统一认证登录工具类
     * 负责模拟 CAS 登录流程，最终获取 JWSESSION
     */
    static class XSYULoginUtil {
        /**
         * Apply a full set of browser-like headers to minimize server-side captcha triggers.
         */
        private static void applyBrowserHeaders(HttpURLConnection conn, String referer) {
            conn.setRequestProperty("User-Agent", UA);
            conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8");
            conn.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
            conn.setRequestProperty("Accept-Encoding", "identity"); // let us handle it manually
            conn.setRequestProperty("Connection", "keep-alive");
            if (referer != null && !referer.isEmpty()) {
                conn.setRequestProperty("Referer", referer);
            }
        }

        /**
         * 执行西安石油大学统一认证登录，获取JWSESSION。
         * CookieManager 管理 domain/path 匹配的 cookie，手动跟随重定向以检测循环。
         * 失败时重新走完整 CAS 流程（全新 CookieManager），最多3次。
         */
        public static String login(String username, String password) {
            for (int attempt = 1; attempt <= 3; attempt++) {
                CookieManager cm = new CookieManager();
                CookieHandler old = CookieHandler.getDefault();
                CookieHandler.setDefault(cm);

                try {
                    if (attempt > 1) {
                        long delay = 3000 + (long) (Math.random() * 2000);
                        System.out.println("=== 第 " + attempt + " 次重新尝试登录 (等待 " + delay + "ms) ===");
                        Thread.sleep(delay);
                    }

                    String result = doLogin(username, password);
                    if (result != null) {
                        System.out.println("登录成功! 尝试次数: " + attempt);
                        return result;
                    }
                    System.out.println("第 " + attempt + " 次尝试失败");

                } catch (Exception e) {
                    System.out.println("第 " + attempt + " 次尝试异常: " + e.getMessage());
                } finally {
                    CookieHandler.setDefault(old);
                }
            }
            return null;
        }

        private static String doLogin(String username, String password) throws IOException, InterruptedException {
            // 第一步：访问服务端，获取CAS重定向URL
            String casLoginUrl = getCasLoginUrl();
            if (casLoginUrl == null) {
                System.out.println("无法获取CAS登录URL");
                return null;
            }
            System.out.println("CAS登录URL: " + casLoginUrl);

            // 第二步：从CAS登录页提取execution
            String execution = getExecutionFromCas(casLoginUrl);
            if (execution == null) {
                System.out.println("无法从CAS获取execution参数");
                return null;
            }
            System.out.println("获取到execution: " + execution);

            // 短暂延迟，模拟浏览器行为，降低被风控识别概率
            Thread.sleep(800 + (long) (Math.random() * 400));

            // 第三步：提交CAS登录表单，获取ticket + CASTGC
            String ticket = submitCasLogin(username, password, execution);
            if (ticket == null) {
                System.out.println("CAS登录失败");
                return null;
            }
            System.out.println("获取到ticket: " + ticket);

            // 第四步：用ticket获取JWSESSION（手动跟随重定向链）
            return getJWSessionWithTicket(ticket);
        }

        // ==================== 第一步 ====================
        private static String getCasLoginUrl() throws IOException {
            HttpURLConnection conn = open(SERVICE_CAS_LOGIN, false);
            int code = conn.getResponseCode();
            System.out.println("初始访问响应码: " + code);
            if (code == 302) {
                String location = conn.getHeaderField("Location");
                System.out.println("重定向到: " + location);
                return location;
            }
            return null;
        }

        // ==================== 第二步 ====================
        private static String getExecutionFromCas(String casUrl) throws IOException {
            HttpURLConnection conn = open(casUrl, false);
            String html = readBody(conn);
            return extractExecution(html);
        }

        // ==================== 第三步 ====================
        private static String submitCasLogin(String username, String password, String execution) throws IOException {
            HttpURLConnection conn = (HttpURLConnection) new URL(CAS_LOGIN_URL).openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setInstanceFollowRedirects(false);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);

            // Set browser-like headers to avoid server-side captcha trigger
            conn.setRequestProperty("User-Agent", UA);
            conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8");
            conn.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Origin", "https://" + CAS_HOST);
            conn.setRequestProperty("Referer", "https://" + CAS_HOST + "/authserver/login?service=" + URLEncoder.encode(SERVICE_CAS_LOGIN, StandardCharsets.UTF_8));
            conn.setRequestProperty("Sec-Ch-Ua", "\"Google Chrome\";v=\"125\", \"Chromium\";v=\"125\", \"Not.A/Brand\";v=\"24\"");
            conn.setRequestProperty("Sec-Ch-Ua-Mobile", "?0");
            conn.setRequestProperty("Sec-Ch-Ua-Platform", "\"Windows\"");
            conn.setRequestProperty("Sec-Fetch-Dest", "document");
            conn.setRequestProperty("Sec-Fetch-Mode", "navigate");
            conn.setRequestProperty("Sec-Fetch-Site", "same-origin");
            conn.setRequestProperty("Upgrade-Insecure-Requests", "1");

            String body = "username=" + URLEncoder.encode(username, StandardCharsets.UTF_8) +
                    "&password=" + URLEncoder.encode(password, StandardCharsets.UTF_8) +
                    "&execution=" + URLEncoder.encode(execution, StandardCharsets.UTF_8) +
                    "&_eventId=submit" +
                    "&loginType=1" +
                    "&rememberMe=true" +
                    "&service=" + URLEncoder.encode(SERVICE_CAS_LOGIN, StandardCharsets.UTF_8);

            System.out.println("提交CAS登录请求体");

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            System.out.println("CAS登录响应码: " + code);

            if (code == 302) {
                String location = conn.getHeaderField("Location");
                System.out.println("CAS登录后重定向到: " + location);
                return extractTicket(location);
            }

            if (code == 200) {
                String html = readBody(conn);
                System.out.println("CAS返回200，响应前500字符: " +
                        html.substring(0, Math.min(500, html.length())));

                if (html.contains("验证码") || html.contains("captcha")) {
                    throw new RuntimeException("登录需要验证码，请手动登录");
                }

                String ticket = extractTicket(html);
                if (ticket != null) {
                    System.out.println("从响应页面提取到ticket: " + ticket);
                    return ticket;
                }
            }

            return null;
        }

        // ==================== 第四步：手动跟随重定向链 ====================
        // 不使用 setInstanceFollowRedirects(true)，因为CAS↔service可能形成死循环
        // 手动跟随可精确检测循环（depth>5 即停止），CookieManager 自动管理cookie
        private static String getJWSessionWithTicket(String ticket) throws IOException {
            String url = SERVICE_CAS_LOGIN + "?ticket=" +
                    URLEncoder.encode(ticket, StandardCharsets.UTF_8);
            System.out.println("使用ticket访问URL: " + url);
            return followRedirect(url, 0);
        }

        private static String followRedirect(String url, int depth) throws IOException {
            if (depth > 5) {
                System.out.println("重定向链过长(" + depth + "次)，停止跟随");
                return null;
            }

            System.out.println((depth == 0 ? "访问" : "跟随重定向[" + depth + "]") + ": " + url);

            HttpURLConnection conn = open(url, false);
            int code = conn.getResponseCode();

            // CookieManager 已自动解析 Set-Cookie 并存入 cookie store
            // 检查是否已拿到 JWSESSION
            String jws = findJWSession();
            if (jws != null) {
                System.out.println("成功获取JWSESSION!");
                return jws;
            }

            System.out.println("响应码[" + depth + "]: " + code);

            if (code == 302) {
                String location = conn.getHeaderField("Location");
                if (location != null) return followRedirect(location, depth + 1);
            }

            return null;
        }

        // ==================== 工具方法 ====================

        private static HttpURLConnection open(String url, boolean followRedirects) throws IOException {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setInstanceFollowRedirects(followRedirects);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            applyBrowserHeaders(conn, url.contains("authserver") ? null : "https://" + CAS_HOST + "/");
            return conn;
        }

        private static String readBody(HttpURLConnection conn) throws IOException {
            try (BufferedReader r = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = r.readLine()) != null) sb.append(line);
                return sb.toString();
            }
        }

        private static String findJWSession() {
            CookieManager cm = (CookieManager) CookieHandler.getDefault();
            if (cm == null) return null;
            for (HttpCookie c : cm.getCookieStore().getCookies()) {
                if ("JWSESSION".equals(c.getName()) && c.getValue() != null && !c.getValue().isEmpty())
                    return c.getValue();
            }
            return null;
        }

        private static String extractTicket(String text) {
            if (text == null) return null;
            Matcher m = Pattern.compile("ticket=([^\"&\\s]+)").matcher(text);
            if (m.find()) return m.group(1);
            return null;
        }

        private static String extractExecution(String html) {
            Matcher m = Pattern.compile("name=\"execution\" value=\"([^\"]+)\"").matcher(html);
            if (m.find()) return m.group(1);
            m = Pattern.compile("execution\" value=\"([^\"]+)\"").matcher(html);
            if (m.find()) return m.group(1);
            return null;
        }
    }
}
""".trimIndent()
