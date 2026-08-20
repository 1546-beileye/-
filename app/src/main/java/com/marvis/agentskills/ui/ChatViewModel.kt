package com.marvis.agentskills.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.marvis.agentskills.AppConfig
import com.marvis.agentskills.engine.SkillEngine
import com.marvis.agentskills.llm.LLMClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class ChatViewModel(app: Application) : AndroidViewModel(app) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    var messages by mutableStateOf<List<ChatMessageUi>>(
        listOf(
            ChatMessageUi(
                "assistant",
                "你好，我是 AgentSkills 助手。你可以让我做搜索、文本处理、文档总结、代码生成等任务，我会自动调用内置 skill 完成。"
            )
        )
    )
        private set

    var isLoading by mutableStateOf(false)
        private set

    var baseUrl by mutableStateOf(AppConfig.getBaseUrl(app))
    var apiKey by mutableStateOf(AppConfig.getApiKey(app))
    var model by mutableStateOf(AppConfig.getModel(app))

    private var job: Job? = null

    fun saveSettings() {
        val ctx = getApplication<Application>()
        AppConfig.setBaseUrl(ctx, baseUrl.trim())
        AppConfig.setApiKey(ctx, apiKey.trim())
        AppConfig.setModel(ctx, model.trim())
    }

    fun send(text: String, engine: SkillEngine) {
        if (text.isBlank() || isLoading) return
        val ctx = getApplication<Application>()
        saveSettings()

        // 意图路由：找匹配的 skill
        val matched = engine.match(text, topK = 1)
        val matchedSkill = matched.firstOrNull()

        messages = messages + ChatMessageUi("user", text)
        isLoading = true
        val assistantMsg = ChatMessageUi("assistant", "")
        messages = messages + assistantMsg

        val client = AppConfig.createClient(ctx)
        val history = buildList {
            // 保留最近 12 条历史
            val recent = messages.dropLast(1).takeLast(12)
            for (m in recent) {
                if (m.content.isNotEmpty() || m === assistantMsg) add(LLMClient.ChatMessage(m.role, m.content))
            }
        }

        // 构建系统提示：skill 注册表 + 命中 skill 的具体指令
        var system = engine.buildSkillSystemPrompt()
        if (matchedSkill != null) {
            val instr = engine.getInstruction(matchedSkill.id)
            if (instr != null) {
                system += "\n\n==== 用户请求命中的 skill: ${matchedSkill.name} ====\n以下是该 skill 的完整指令，请严格按指令执行：\n$instr"
            }
        }

        job = scope.launch {
            client.chatStream(history, system, object : LLMClient.StreamCallback {
                override fun onDelta(delta: String) {
                    val idx = messages.indexOf(assistantMsg)
                    if (idx >= 0) {
                        val updated = messages.toMutableList()
                        updated[idx] = ChatMessageUi(
                            "assistant",
                            updated[idx].content + delta,
                            matchedSkill?.name
                        )
                        messages = updated
                    }
                }

                override fun onDone(reason: String) {
                    isLoading = false
                }

                override fun onError(message: String) {
                    isLoading = false
                    val idx = messages.indexOf(assistantMsg)
                    if (idx >= 0) {
                        val updated = messages.toMutableList()
                        updated[idx] = ChatMessageUi(
                            "assistant",
                            if (updated[idx].content.isEmpty()) "⚠️ $message" else updated[idx].content + "\n\n⚠️ $message",
                            matchedSkill?.name
                        )
                        messages = updated
                    }
                }
            })
        }
    }

    override fun onCleared() {
        job?.cancel()
        scope.cancel()
        super.onCleared()
    }
}
