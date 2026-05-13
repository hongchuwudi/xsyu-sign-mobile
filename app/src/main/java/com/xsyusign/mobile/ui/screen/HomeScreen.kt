package com.xsyusign.mobile.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xsyusign.mobile.data.db.AppDatabase
import com.xsyusign.mobile.data.entity.SignLog
import com.xsyusign.mobile.data.entity.User
import com.xsyusign.mobile.data.repository.SignLogRepository
import com.xsyusign.mobile.data.repository.UserRepository
import com.xsyusign.mobile.network.CasLoginService
import com.xsyusign.mobile.network.SignApiService
import com.xsyusign.mobile.network.dto.SignItem
import com.xsyusign.mobile.util.SettingsManager
import com.xsyusign.mobile.worker.WorkerScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToUsers: () -> Unit,
    onNavigateToLogs: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()

    val db = remember { AppDatabase.getInstance(context) }
    val userRepo = remember { UserRepository(db.userDao()) }
    val logRepo = remember { SignLogRepository(db.signLogDao()) }

    var users by remember { mutableStateOf<List<User>>(emptyList()) }
    var recentLogs by remember { mutableStateOf<List<SignLog>>(emptyList()) }
    var isSigning by remember { mutableStateOf(false) }
    var showSetupGuide by remember { mutableStateOf(SettingsManager.isShowGuide(context)) }
    var showStarDialog by remember { mutableStateOf(SettingsManager.isFirstLaunch(context)) }
    var logDialogUser by remember { mutableStateOf<User?>(null) }

    // 首次启动弹窗：求 Star
    if (showStarDialog) {
        AlertDialog(
            onDismissRequest = { showStarDialog = false },
            icon = { Icon(Icons.Filled.Code, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("hongchu-sign", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("本项目完全开源，代码托管在 GitHub。")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "github.com/hongchuwudi/xsyu-sign-mobile",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("如果对你有帮助，欢迎给个 Star ⭐")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showStarDialog = false
                    val intent = android.content.Intent(
                        android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse("https://github.com/hongchuwudi/xsyu-sign-mobile")
                    )
                    context.startActivity(intent)
                }) { Text("去 Star") }
            },
            dismissButton = {
                TextButton(onClick = { showStarDialog = false }) { Text("稍后") }
            }
        )
    }

    // 用户日志弹窗
    logDialogUser?.let { user ->
        UserLogDialog(
            user = user,
            logRepo = logRepo,
            onDismiss = { logDialogUser = null }
        )
    }

    LaunchedEffect(Unit) {
        userRepo.observeAll().collect { users = it }
    }

    LaunchedEffect(Unit) {
        logRepo.observeRecent(10).collect { recentLogs = it }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("hongchu-sign") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "设置")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 快捷操作卡片
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // 全部签到
                        FilledIconButton(
                            onClick = {
                                if (!isSigning) {
                                    isSigning = true
                                    scope.launch {
                                        WorkerScheduler.signAllNow(context)
                                        kotlinx.coroutines.delay(1000)
                                        isSigning = false
                                    }
                                }
                            },
                            enabled = !isSigning,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = "全部签到")
                        }
                        Text("全部签到", style = MaterialTheme.typography.labelLarge)

                        // 管理用户
                        FilledIconButton(
                            onClick = onNavigateToUsers,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Icon(Icons.Filled.People, contentDescription = "用户管理")
                        }
                        Text("用户管理", style = MaterialTheme.typography.labelLarge)

                        // 签到日志
                        FilledIconButton(
                            onClick = onNavigateToLogs,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Icon(Icons.Filled.History, contentDescription = "签到日志")
                        }
                        Text("签到日志", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            // 后台保活指南
            if (showSetupGuide) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Filled.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "后台保活设置",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        showSetupGuide = false
                                        SettingsManager.setShowGuide(context, false)
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = "关闭",
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "定时签到依赖系统后台调度，请完成以下设置防止被清理：",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Spacer(Modifier.height(10.dp))
                            SetupStep("1", "电池优化 → 设为「无限制」", "设置 → 应用 → hongchu-sign → 电池")
                            Spacer(Modifier.height(6.dp))
                            SetupStep("2", "启动管理 → 开启「自启动」", "设置 → 应用 → 启动管理 → 允许")
                            Spacer(Modifier.height(6.dp))
                            SetupStep("3", "多任务界面 → 锁定应用", "划出多任务 → 长按本应用 → 锁定")
                        }
                    }
                }
            }

            // 用户列表
            item {
                Text(
                    "用户 (${users.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (users.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Filled.PersonAdd,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            Text("暂无用户", style = MaterialTheme.typography.bodyLarge)
                            Text("点击右下角 + 添加", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            } else {
                items(users) { user ->
                    UserCard(
                        user = user,
                        onClick = { logDialogUser = user },
                        onToggleAuto = {
                            scope.launch {
                                val updated = user.copy(
                                    autoSign = !user.autoSign,
                                    updatedAt = System.currentTimeMillis()
                                )
                                userRepo.update(updated)
                            }
                        }
                    )
                }
            }

            // 最近签到日志
            if (recentLogs.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "最近签到",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(recentLogs) { log ->
                    LogItem(log)
                }
            }
        }
    }
}

@Composable
private fun UserCard(user: User, onClick: () -> Unit, onToggleAuto: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.AccountCircle,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.name.ifEmpty { user.username },
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = user.username,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            AssistChip(
                onClick = onToggleAuto,
                label = {
                    Text(if (user.autoSign) "自动" else "手动", style = MaterialTheme.typography.labelLarge)
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (user.autoSign) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    }
}

@Composable
private fun SetupStep(num: String, title: String, desc: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier.size(22.dp),
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.primary
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    num,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(
                desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun UserLogDialog(
    user: User,
    logRepo: SignLogRepository,
    onDismiss: () -> Unit
) {
    var logs by remember { mutableStateOf<List<SignLog>>(emptyList()) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var serverItems by remember { mutableStateOf<List<SignItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(user.id) {
        logRepo.observeByUser(user.id, 10).collect { logs = it }
    }

    // 切换到服务器记录时自动拉取
    LaunchedEffect(selectedTab) {
        if (selectedTab == 1 && serverItems.isEmpty() && !isLoading) {
            isLoading = true
            loadError = null
            val items = withContext(Dispatchers.IO) {
                try {
                    val jws = CasLoginService.login(user.username, user.password)
                    if (jws == null) return@withContext null to "CAS 登录失败"
                    val resp = SignApiService.getSignList(jws, page = 1, size = 50)
                    if (resp.code != 0 && resp.code != 200) return@withContext null to (resp.message ?: "请求失败")
                    resp.data to null
                } catch (e: Exception) {
                    null to e.message
                }
            }
            isLoading = false
            if (items.second != null) loadError = items.second
            else serverItems = items.first ?: emptyList()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(user.name.ifEmpty { user.username }, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                        Text("本地日志", modifier = Modifier.padding(12.dp))
                    }
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                        Text("服务器记录", modifier = Modifier.padding(12.dp))
                    }
                }
            }
        },
        text = {
            when (selectedTab) {
                0 -> {
                    if (logs.isEmpty()) {
                        Text("暂无本地日志", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            logs.forEach { log ->
                                val dateFormat = DateTimeFormatter.ofPattern("MM-dd HH:mm")
                                val isSuccess = log.result.contains("成功")
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        if (isSuccess) Icons.Filled.CheckCircle else Icons.Filled.ErrorOutline,
                                        contentDescription = null,
                                        tint = if (isSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(log.signTitle.ifEmpty { log.result }, style = MaterialTheme.typography.bodySmall)
                                        Text(
                                            java.time.LocalDateTime.ofInstant(Instant.ofEpochMilli(log.createdAt), ZoneId.systemDefault()).format(dateFormat),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    when {
                        isLoading -> {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(32.dp))
                            }
                        }
                        loadError != null -> Text("加载失败: $loadError", color = MaterialTheme.colorScheme.error)
                        serverItems.isEmpty() -> Text("服务器无签到记录")
                        else -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                serverItems.forEach { item ->
                                    val isSigned = item.signStatus == 2
                                    val dateStr = if (item.date != null) {
                                        try {
                                            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                                            val fmt = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
                                            fmt.format(sdf.parse(item.date)!!)
                                        } catch (e: Exception) { item.date ?: "" }
                                    } else ""
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            if (isSigned) Icons.Filled.CheckCircle else Icons.Filled.Schedule,
                                            contentDescription = null,
                                            tint = if (isSigned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                item.signTitle ?: "签到",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Text(
                                                    if (isSigned) "已签" else "未签",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = if (isSigned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                if (dateStr.isNotEmpty()) {
                                                    Text(dateStr, style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}

@Composable
private fun LogItem(log: SignLog) {
    val dateFormat = remember { DateTimeFormatter.ofPattern("MM-dd HH:mm") }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (log.result.contains("成功")) Icons.Filled.CheckCircle else Icons.Filled.Error,
                contentDescription = null,
                tint = if (log.result.contains("成功")) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(log.username, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(log.signTitle.ifEmpty { log.result }, style = MaterialTheme.typography.bodySmall)
            }
            Text(
                java.time.LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(log.createdAt), ZoneId.systemDefault()
                ).format(dateFormat),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
