package com.marvis.agentskills.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.marvis.agentskills.engine.Skill
import com.marvis.agentskills.engine.SkillEngine

@Composable
fun AgentSkillsApp(skillEngine: SkillEngine) {
    MaterialTheme {
        val vm: ChatViewModel = viewModel()
        var showSettings by remember { mutableStateOf(false) }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("AgentSkills", fontWeight = FontWeight.Bold)
                            Text("已加载 ${skillEngine.skillCount()} 个 Skill", fontSize = 12.sp, color = Color.Gray)
                        }
                    },
                    actions = {
                        IconButton(onClick = { showSettings = !showSettings }) {
                            Icon(Icons.Default.Settings, contentDescription = "设置")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF161B22),
                        titleContentColor = Color.White
                    )
                )
            },
            containerColor = Color(0xFF0D1117)
        ) { padding ->
            Box(Modifier.padding(padding)) {
                if (showSettings) {
                    SettingsPanel(vm, skillEngine, onClose = { showSettings = false })
                } else {
                    ChatScreen(vm, skillEngine)
                }
            }
        }
    }
}

@Composable
private fun SettingsPanel(vm: ChatViewModel, engine: SkillEngine, onClose: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
            .padding(16.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("设置", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = "关闭", tint = Color.White) }
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = vm.baseUrl,
            onValueChange = { vm.baseUrl = it },
            label = { Text("API Base URL") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = vm.apiKey,
            onValueChange = { vm.apiKey = it },
            label = { Text("API Key") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = vm.model,
            onValueChange = { vm.model = it },
            label = { Text("模型 (Model)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = { vm.saveSettings() }, modifier = Modifier.fillMaxWidth()) {
            Text("保存设置")
        }
        Spacer(Modifier.height(24.dp))
        HorizontalDivider(color = Color(0xFF30363D))
        Spacer(Modifier.height(16.dp))
        Text("内置 Skills (${engine.allSkills().size})", color = Color.White, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        LazyColumn(Modifier.weight(1f)) {
            items(engine.allSkills()) { s ->
                SkillRow(s)
            }
        }
    }
}

@Composable
private fun SkillRow(s: Skill) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(s.name, color = Color.White, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(8.dp))
                Text("[$s.source]", color = Color(0xFF58A6FF), fontSize = 12.sp)
            }
            Spacer(Modifier.height(4.dp))
            Text(s.description, color = Color(0xFF8B949E), fontSize = 13.sp)
        }
    }
}

@Composable
private fun ChatScreen(vm: ChatViewModel, engine: SkillEngine) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }

    // 自动滚动到底部
    LaunchedEffect(vm.messages.size) {
        if (vm.messages.isNotEmpty()) listState.animateScrollToItem(vm.messages.size - 1)
    }

    Column(
        Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(vm.messages) { msg ->
                MessageBubble(msg)
            }
            if (vm.isLoading) {
                item { LoadingRow() }
            }
        }

        HorizontalDivider(color = Color(0xFF30363D))
        Row(
            Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text("输入消息，AgentSkills 会调用内置 skill 处理...", color = Color.Gray) },
                modifier = Modifier.weight(1f),
                maxLines = 4
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = {
                    val text = input.trim()
                    if (text.isNotEmpty() && !vm.isLoading) {
                        vm.send(text, engine)
                        input = ""
                    }
                },
                enabled = !vm.isLoading
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "发送",
                    tint = if (vm.isLoading) Color.Gray else Color(0xFF58A6FF)
                )
            }
        }
    }
}

@Composable
private fun MessageBubble(msg: ChatMessageUi) {
    val isUser = msg.role == "user"
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            Modifier
                .width(if (isUser) 0.8f * 320.dp else 0.9f * 320.dp)
                .background(
                    color = if (isUser) Color(0xFF1F6FEB) else Color(0xFF21262D),
                    shape = RoundedCornerShape(
                        topStart = 16.dp, topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .padding(12.dp)
        ) {
            if (msg.skillName != null) {
                Text(
                    "🛠 ${msg.skillName}",
                    color = Color(0xFF7EE787),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(4.dp))
            }
            Text(msg.content, color = Color.White, fontSize = 15.sp)
        }
    }
}

@Composable
private fun LoadingRow() {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            modifier = Modifier
                .width(18.dp)
                .height(18.dp),
            strokeWidth = 2.dp
        )
        Spacer(Modifier.width(8.dp))
        Text("思考中...", color = Color.Gray, fontSize = 13.sp)
    }
}
