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
import com.xsyusign.mobile.data.entity.User
import com.xsyusign.mobile.data.repository.SignLogRepository
import com.xsyusign.mobile.data.repository.UserRepository
import com.xsyusign.mobile.worker.WorkerScheduler
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserListScreen(
    onBack: () -> Unit,
    onEditUser: (Long?) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val userRepo = remember { UserRepository(db.userDao()) }
    val logRepo = remember { SignLogRepository(db.signLogDao()) }

    var users by remember { mutableStateOf<List<User>>(emptyList()) }
    var showDeleteDialog by remember { mutableStateOf<User?>(null) }

    LaunchedEffect(Unit) {
        userRepo.observeAll().collect { users = it }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("用户管理") },
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
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onEditUser(null) },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "添加用户")
            }
        }
    ) { padding ->
        if (users.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.PersonAdd,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("还没有添加用户", style = MaterialTheme.typography.bodyLarge)
                    Text("点击右下角 + 添加学号和密码", style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(users) { user ->
                    UserDetailCard(
                        user = user,
                        onEdit = { onEditUser(user.id) },
                        onDelete = { showDeleteDialog = user },
                        onSignNow = {
                            WorkerScheduler.signNow(context, userId = user.id)
                        }
                    )
                }
            }
        }

        // 删除确认对话框
        showDeleteDialog?.let { user ->
            AlertDialog(
                onDismissRequest = { showDeleteDialog = null },
                title = { Text("删除用户") },
                text = { Text("确定要删除「${user.name.ifEmpty { user.username }}」吗？此操作不可撤销。") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            kotlinx.coroutines.MainScope().launch {
                                userRepo.delete(user)
                            }
                            showDeleteDialog = null
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) { Text("删除") }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = null }) { Text("取消") }
                }
            )
        }
    }
}

@Composable
private fun UserDetailCard(
    user: User,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSignNow: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.AccountCircle,
                    contentDescription = null,
                    modifier = Modifier.size(44.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = user.name.ifEmpty { user.username },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(user.username, style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(Modifier.height(12.dp))

            // 自动签到设置
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val daysLabels = listOf("日", "一", "二", "三", "四", "五", "六")
                val activeDays = user.signDays.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    daysLabels.forEachIndexed { index, label ->
                        FilterChip(
                            selected = index in activeDays,
                            onClick = {},
                            label = { Text(label, style = MaterialTheme.typography.bodySmall) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "签到 ${user.signStartTime}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onSignNow) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("签到")
                    }
                    Button(onClick = onEdit) {
                        Text("编辑")
                    }
                }
            }
        }
    }
}
