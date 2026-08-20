package com.marvis.agentskills.llm

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * 兼容 OpenAI Chat Completions 协议的流式对话客户端。
 * 用户可在设置中自定义 BaseUrl 与 API Key（如 OpenAI / DeepSeek / 兼容中转）。
 */
class LLMClient(
    private val baseUrl: String,
    private val apiKey: String,
    private val model: String
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    data class ChatMessage(val role: String, val content: String)

    fun interface StreamCallback {
        fun onDelta(delta: String)
        fun onDone(reason: String)
        fun onError(message: String)
    }

    fun chatStream(messages: List<ChatMessage>, system: String, callback: StreamCallback) {
        val body = JSONObject()
        body.put("model", model)
        body.put("stream", true)
        body.put("temperature", 0.7)
        val msgs = JSONArray()
        msgs.put(JSONObject().put("role", "system").put("content", system))
        for (m in messages) {
            msgs.put(JSONObject().put("role", m.role).put("content", m.content))
        }
        body.put("messages", msgs)

        val req = Request.Builder()
            .url(baseUrl.trimEnd('/') + "/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val factory = EventSources.createFactory(client)
        factory.newEventSource(req, object : EventSourceListener() {
            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                if (data == "[DONE]") {
                    callback.onDone("done")
                    return
                }
                try {
                    val json = JSONObject(data)
                    val choices = json.optJSONArray("choices")
                    if (choices != null && choices.length() > 0) {
                        val delta = choices.getJSONObject(0).optJSONObject("delta")
                        val content = delta?.optString("content", "") ?: ""
                        if (content.isNotEmpty()) callback.onDelta(content)
                    }
                } catch (e: Exception) {
                    // 忽略非 JSON 行
                }
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: okhttp3.Response?) {
                val msg = when {
                    t is IOException -> "网络错误：${t.message}"
                    response != null -> "HTTP ${response.code}：${response.body?.string()?.take(200)}"
                    else -> "未知错误"
                }
                callback.onError(msg)
            }
        })
    }
}
