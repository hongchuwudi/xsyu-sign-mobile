package com.xsyusign.mobile.ui.screen

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xsyusign.mobile.data.db.AppDatabase
import com.xsyusign.mobile.data.entity.User
import com.xsyusign.mobile.data.repository.UserRepository
import com.xsyusign.mobile.network.CasLoginService
import com.xsyusign.mobile.util.CryptoUtil
import com.xsyusign.mobile.util.expandTimeWindow
import com.xsyusign.mobile.util.midpointTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserEditScreen(
    userId: Long?,
    onBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getInstance(context) }
    val userRepo = remember { UserRepository(db.userDao()) }

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var autoSign by remember { mutableStateOf(false) }
    var selectedDays by remember { mutableStateOf(setOf(1, 2, 3, 4, 5)) }
    var signTime by remember { mutableStateOf("18:30") }
    var isSaving by remember { mutableStateOf(false) }
    var isTesting by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

    val isEdit = userId != null && userId > 0

    // 加载现有用户数据
    LaunchedEffect(userId) {
        if (isEdit) {
            val user = userRepo.getById(userId!!)
            if (user != null) {
                username = user.username
                name = user.name
                autoSign = user.autoSign
                selectedDays = user.signDays.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
                signTime = midpointTime(user.signStartTime, user.signEndTime)
                // 密码解密
                val decrypted = CryptoUtil.decrypt(user.password)
                if (decrypted != null) password = decrypted
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEdit) "编辑用户" else "添加用户") },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ---- 账户信息 ----
            Text("账户信息", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("学号") },
                leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("密码") },
                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = null
                        )
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None
                else androidx.compose.ui.text.input.PasswordVisualTransformation()
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("姓名（选填）") },
                leadingIcon = { Icon(Icons.Filled.Badge, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // 测试登录按钮
            OutlinedButton(
                onClick = {
                    if (username.isNotBlank() && password.isNotBlank()) {
                        isTesting = true
                        scope.launch {
                            val encrypted = CryptoUtil.encrypt(password)
                            val jws = withContext(Dispatchers.IO) {
                                CasLoginService.login(username, encrypted)
                            }
                            isTesting = false
                            val logRepo = com.xsyusign.mobile.data.repository.SignLogRepository(
                                com.xsyusign.mobile.data.db.AppDatabase.getInstance(context).signLogDao()
                            )
                            if (jws != null) {
                                android.util.Log.d("XSYUSign", "登录测试成功: $username JWS=$jws")
                                Toast.makeText(context, "登录测试成功", Toast.LENGTH_SHORT).show()
                                logRepo.insert(com.xsyusign.mobile.data.entity.SignLog(
                                    userId = userId ?: 0, username = username,
                                    result = "登录测试成功"
                                ))
                            } else {
                                android.util.Log.e("XSYUSign", "登录测试失败: $username")
                                Toast.makeText(context, "登录测试失败，请检查学号和密码", Toast.LENGTH_LONG).show()
                                logRepo.insert(com.xsyusign.mobile.data.entity.SignLog(
                                    userId = userId ?: 0, username = username,
                                    result = "登录测试失败"
                                ))
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isTesting && username.isNotBlank() && password.isNotBlank()
            ) {
                if (isTesting) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                Text(if (isTesting) "测试中..." else "测试登录")
            }

            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

            // ---- 自动签到设置 ----
            Text("自动签到设置", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            // 启用开关
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("启用自动签到", style = MaterialTheme.typography.bodyLarge)
                Switch(checked = autoSign, onCheckedChange = { autoSign = it })
            }

            // 选择签到日
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("签到日", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "已选 ${selectedDays.size} 天",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selectedDays.isEmpty()) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary
                )
            }
            val daysLabels = listOf("日", "一", "二", "三", "四", "五", "六")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                daysLabels.forEachIndexed { index, label ->
                    val isSelected = index in selectedDays
                    val shape = MaterialTheme.shapes.small
                    Surface(
                        modifier = Modifier.clickable {
                            selectedDays = selectedDays.toMutableSet().apply {
                                if (index in this) remove(index) else add(index)
                            }
                        },
                        shape = shape,
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface,
                        border = if (isSelected)
                            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                        else
                            androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Text(
                            label,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // 签到时间点 — 内部自动 ±15 分钟容错窗口
            OutlinedTextField(
                value = signTime,
                onValueChange = { v ->
                    val filtered = v.replace('：', ':').filter { it.isDigit() || it == ':' }.take(5)
                    signTime = filtered
                },
                label = { Text("签到时间") },
                supportingText = { Text("系统会在此时前后 15 分钟内执行签到") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("18:30") },
                isError = signTime.isNotEmpty() && !signTime.matches(Regex("\\d{2}:\\d{2}"))
            )

            Spacer(Modifier.height(16.dp))

            // 保存按钮
            Button(
                onClick = {
                    if (username.isBlank() || password.isBlank()) return@Button

                    isSaving = true
                    scope.launch {
                        val encrypted = CryptoUtil.encrypt(password)
                        // 根据签到时间点计算 ±15 分钟窗口
                        val (startTime, endTime) = expandTimeWindow(signTime)
                        val user = if (isEdit) {
                            val existing = userRepo.getById(userId!!) ?: return@launch
                            existing.copy(
                                username = username,
                                password = encrypted,
                                name = name,
                                autoSign = autoSign,
                                signDays = selectedDays.sorted().joinToString(","),
                                signStartTime = startTime,
                                signEndTime = endTime,
                                updatedAt = System.currentTimeMillis()
                            )
                        } else {
                            User(
                                username = username,
                                password = encrypted,
                                name = name,
                                autoSign = autoSign,
                                signDays = selectedDays.sorted().joinToString(","),
                                signStartTime = startTime,
                                signEndTime = endTime
                            )
                        }

                        withContext(Dispatchers.IO) {
                            if (isEdit) userRepo.update(user)
                            else userRepo.save(user)
                        }

                        isSaving = false
                        Toast.makeText(context, "保存成功", Toast.LENGTH_SHORT).show()
                        onBack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = !isSaving && username.isNotBlank() && password.isNotBlank() && selectedDays.isNotEmpty()
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text("保存")
            }

            // 提示缺少的必填项
            if (!isSaving) {
                val hints = buildList {
                    if (username.isBlank()) add("学号")
                    if (password.isBlank()) add("密码")
                    if (selectedDays.isEmpty()) add("签到日")
                }
                if (hints.isNotEmpty()) {
                    Text(
                        text = "请填写：${hints.joinToString("、")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }
    }
}

