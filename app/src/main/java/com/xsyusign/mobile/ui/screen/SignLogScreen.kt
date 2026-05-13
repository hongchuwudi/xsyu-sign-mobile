package com.xsyusign.mobile.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xsyusign.mobile.data.db.AppDatabase
import com.xsyusign.mobile.data.entity.SignLog
import com.xsyusign.mobile.data.repository.SignLogRepository
import android.widget.Toast
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignLogScreen(onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getInstance(context) }
    val logRepo = remember { SignLogRepository(db.signLogDao()) }

    var logs by remember { mutableStateOf<List<SignLog>>(emptyList()) }

    LaunchedEffect(Unit) {
        logRepo.observeRecent(100).collect { logs = it }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("签到日志") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            logRepo.cleanAllLogs()
                            Toast.makeText(context, "已清空全部日志", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(Icons.Filled.DeleteSweep, contentDescription = "清空日志")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        if (logs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.History,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("暂无签到记录", style = MaterialTheme.typography.bodyLarge)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(logs) { log ->
                    LogDetailItem(log)
                }
            }
        }
    }
}

@Composable
private fun LogDetailItem(log: SignLog) {
    val dateFormat = remember { DateTimeFormatter.ofPattern("MM-dd HH:mm:ss") }
    val isSuccess = log.result.contains("成功")
    val isWarning = !isSuccess && log.result.contains("无可签")

    val (icon, iconTint, textColor) = when {
        isSuccess -> Triple(Icons.Filled.CheckCircle, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary)
        isWarning -> Triple(Icons.Filled.Warning, MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.onSurfaceVariant)
        else -> Triple(Icons.Filled.ErrorOutline, MaterialTheme.colorScheme.error, MaterialTheme.colorScheme.error)
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    log.username,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                if (log.signTitle.isNotEmpty()) {
                    Text(
                        log.signTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    log.result,
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor
                )
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
