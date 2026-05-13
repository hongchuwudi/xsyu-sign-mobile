package com.xsyusign.mobile.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xsyusign.mobile.util.SettingsManager

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
        }
    }
}
