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
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.net.http.*;
import java.util.*;
import java.util.regex.*;

/**
 * 西安石油大学一键签到（单文件，Java 11+，零依赖）
 * 用法: javac XSYUOneKeySign.java && java XSYUOneKeySign
 */
public class XSYUOneKeySign {

    // ==================== 在这里改成你的学号和密码 ====================
    static String USERNAME = "my-username";
    static String PASSWORD = "my-password";
    // ===============================================================

    static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .followRedirects(HttpClient.Redirect.NEVER).build();
    static final String UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/125.0.0.0 Safari/537.36";
    static final String MOBILE_UA = "Mozilla/5.0 (Linux; Android 13; SM-G991B) AppleWebKit/537.36 Chrome/112.0.0.0 Mobile Safari/537.36";
    static final String SERVICE = "https://gwxg.xsyu.edu.cn/basicinfo/mobile/login/casLogin";
    static final String CAS_URL = "https://ids.xsyu.edu.cn/authserver/login";

    public static void main(String[] args) throws Exception {
        String jws = casLogin(USERNAME, PASSWORD);
        if (jws == null) { System.err.println("登录失败"); return; }
        System.out.println("JWSESSION: " + jws);

        String signJson = getSignList(jws);
        for (SignItem item : parseSignItems(signJson)) {
            System.out.printf("签到: %s ... %s%n", item.title,
                doSign(jws, item.id, item.signId, item.schoolId) ? "成功" : "失败");
        }
    }

    // === CAS 登录 ===
    static String casLogin(String user, String pass) throws Exception {
        // 1. 获取 CAS 登录页 URL
        HttpURLConnection c = openGet(SERVICE, false);
        if (c.getResponseCode() != 302) return null;
        String casUrl = c.getHeaderField("Location");
        readBody(c); // consume

        // 2. 提取 execution
        c = openGet(casUrl, false);
        String html = readBody(c);
        String exec = extract(html, "name=\"execution\" value=\"([^\"]+)\"");
        if (exec == null) exec = extract(html, "execution\" value=\"([^\"]+)\"");
        System.out.println("execution=" + exec);
        Thread.sleep(800 + (long)(Math.random() * 400));

        // 3. POST 登录
        HttpURLConnection pc = (HttpURLConnection) new URL(CAS_URL).openConnection();
        pc.setRequestMethod("POST"); pc.setDoOutput(true);
        pc.setInstanceFollowRedirects(false);
        pc.setConnectTimeout(15000); pc.setReadTimeout(15000);
        pc.setRequestProperty("User-Agent", UA);
        pc.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        pc.setRequestProperty("Origin", "https://ids.xsyu.edu.cn");
        String encoded = URLEncoder.encode(SERVICE, StandardCharsets.UTF_8);
        pc.setRequestProperty("Referer", "https://ids.xsyu.edu.cn/authserver/login?service=" + encoded);
        String body = "username=" + URLEncoder.encode(user, StandardCharsets.UTF_8)
            + "&password=" + URLEncoder.encode(pass, StandardCharsets.UTF_8)
            + "&execution=" + URLEncoder.encode(exec, StandardCharsets.UTF_8)
            + "&_eventId=submit&loginType=1&rememberMe=true&service=" + encoded;
        try (OutputStream os = pc.getOutputStream()) { os.write(body.getBytes(StandardCharsets.UTF_8)); }
        String ticket = null;
        if (pc.getResponseCode() == 302) {
            ticket = extract(pc.getHeaderField("Location"), "ticket=([^\"&\\\\s]+)");
        } else if (pc.getResponseCode() == 200) {
            String h = readBody(pc);
            if (h.contains("验证码")) throw new RuntimeException("需要验证码");
            ticket = extract(h, "ticket=([^\"&\\\\s]+)");
        }
        if (ticket == null) return null;
        System.out.println("ticket=" + ticket);

        // 4. 用 ticket 获取 JWSESSION
        return followRedirect(SERVICE + "?ticket=" + URLEncoder.encode(ticket, StandardCharsets.UTF_8), 0);
    }

    static String followRedirect(String url, int depth) throws Exception {
        if (depth > 5) return null;
        String jws = findJws();
        if (jws != null) return jws;
        openGet(url, false).disconnect();
        jws = findJws();
        if (jws != null) return jws;
        HttpURLConnection c = openGet(url, false);
        if (c.getResponseCode() == 302) {
            String loc = c.getHeaderField("Location");
            readBody(c);
            if (loc != null) return followRedirect(loc, depth + 1);
        }
        return null;
    }

    // === 签到 API ===
    static String getSignList(String jws) throws Exception {
        return httpGet("https://gwxg.xsyu.edu.cn/sign/mobile/receive/getMySignLogs?page=1&size=50", jws);
    }

    static boolean doSign(String jws, String id, String signId, String schoolId) throws Exception {
        String url = "https://gwxg.xsyu.edu.cn/sign/mobile/receive/doSignByArea?id=" + id + "&signId=" + signId + "&schoolId=" + schoolId;
        String reqBody = "{\"inArea\":1,\"areaJSON\":\"{\\\"id\\\":\\\"170002\\\",\\\"name\\\":\\\"鄠邑校区\\\"}\",\"latitude\":34.098273,\"longitude\":108.656693}";
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url))
            .header("User-Agent", MOBILE_UA).header("Content-Type", "application/json")
            .header("JWSESSION", jws)
            .POST(HttpRequest.BodyPublishers.ofString(reqBody)).build();
        HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        return resp.body().contains("\"code\":0") || resp.body().contains("\"code\":200");
    }

    // === 工具方法 ===
    static HttpURLConnection openGet(String url, boolean follow) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setRequestMethod("GET");
        c.setInstanceFollowRedirects(follow);
        c.setConnectTimeout(15000); c.setReadTimeout(15000);
        c.setRequestProperty("User-Agent", UA);
        return c;
    }

    static String readBody(HttpURLConnection c) throws Exception {
        try (BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder(); String line;
            while ((line = r.readLine()) != null) sb.append(line);
            return sb.toString();
        } catch (Exception e) { return ""; }
    }

    static String httpGet(String url, String jws) throws Exception {
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url))
            .header("User-Agent", MOBILE_UA).header("JWSESSION", jws)
            .header("Accept", "application/json").GET().build();
        return HTTP.send(req, HttpResponse.BodyHandlers.ofString()).body();
    }

    static String findJws() {
        CookieManager cm = (CookieManager) CookieHandler.getDefault();
        if (cm == null) return null;
        for (HttpCookie ck : cm.getCookieStore().getCookies())
            if ("JWSESSION".equals(ck.getName()) && !ck.getValue().isEmpty()) return ck.getValue();
        return null;
    }

    static String extract(String text, String regex) {
        if (text == null) return null;
        Matcher m = Pattern.compile(regex).matcher(text);
        return m.find() ? m.group(1) : null;
    }

    static List<SignItem> parseSignItems(String json) {
        List<SignItem> items = new ArrayList<>();
        long now = System.currentTimeMillis();
        Matcher m = Pattern.compile("\"id\":\"([^\"]+)\"[^}]*\"signId\":\"([^\"]+)\"[^}]*\"schoolId\":\"([^\"]+)\"[^}]*\"signTitle\":\"([^\"]+)\"[^}]*\"signStatus\":(\\\\d+)[^}]*\"start\":(\\\\d+)[^}]*\"end\":(\\\\d+)").matcher(json);
        while (m.find()) {
            int status = Integer.parseInt(m.group(5));
            long start = Long.parseLong(m.group(6));
            long end = Long.parseLong(m.group(7));
            if (status == 1 && now >= start && now <= end)
                items.add(new SignItem(m.group(1), m.group(2), m.group(3), m.group(4)));
        }
        return items;
    }

    static class SignItem {
        String id, signId, schoolId, title;
        SignItem(String a, String b, String c, String d) { id=a; signId=b; schoolId=c; title=d; }
    }
}
""".trimIndent()
