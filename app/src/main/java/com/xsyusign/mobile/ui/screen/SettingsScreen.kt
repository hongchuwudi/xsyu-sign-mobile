package com.xsyusign.mobile.ui.screen

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.xsyusign.mobile.worker.KeepAliveService
import com.xsyusign.mobile.worker.SignAlarmReceiver
import com.xsyusign.mobile.worker.TestAlarmReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
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

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text("定时任务", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            var keepAlive by remember { mutableStateOf(SettingsManager.isKeepAliveEnabled(context)) }
            ListItem(
                headlineContent = { Text("前台服务保活") },
                supportingContent = {
                    Text(
                        if (keepAlive) "已开启 — 通知栏显示「定时签到运行中」，清理后台也不影响签到"
                        else "已关闭 — 清理后台后定时签到会失效"
                    )
                },
                leadingContent = {
                    Icon(
                        if (keepAlive) Icons.Filled.Shield else Icons.Filled.Shield,
                        contentDescription = null,
                        tint = if (keepAlive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingContent = {
                    Switch(
                        checked = keepAlive,
                        onCheckedChange = {
                            keepAlive = it
                            SettingsManager.setKeepAliveEnabled(context, it)
                            if (it) {
                                KeepAliveService.start(context)
                            } else {
                                KeepAliveService.stop(context)
                            }
                        }
                    )
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

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
                    Text(
                        if (isTesting) "30 秒后将收到通知…" else "保持 App 前台，30 秒后应收到通知（清后台无效）"
                    )
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
                                    Toast.makeText(context, "已调度，保持 App 前台，30 秒后应收到通知", Toast.LENGTH_LONG).show()
                                }
                            }
                        }) { Text("运行") }
                    }
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text("后台保活", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            ListItem(
                headlineContent = { Text("显示保活设置引导") },
                supportingContent = { Text("在首页显示电池优化、自启动等设置说明") },
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

            Text("网页版", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://xsyusign.hongchu.xyz"))
                        context.startActivity(intent)
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
                            Text("⚠ 部分手机定时可能失效", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
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

            Text("关于", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/hongchuwudi/xsyu-sign-mobile"))
                        context.startActivity(intent)
                    }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Code,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("开源地址", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        Text(
                            "github.com/hongchuwudi/xsyu-sign-mobile",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "如果觉得好用，欢迎给个 Star ⭐",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
