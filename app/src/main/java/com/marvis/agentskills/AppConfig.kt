package com.marvis.agentskills

import android.content.Context
import com.marvis.agentskills.llm.LLMClient

/** 会话级配置（持久化保存） */
object AppConfig {
    private const val PREFS = "agent_skills_config"
    const val DEFAULT_BASE_URL = "https://api.openai.com/v1"
    const val DEFAULT_MODEL = "gpt-4o-mini"

    fun getBaseUrl(ctx: Context): String =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString("base_url", DEFAULT_BASE_URL)!!

    fun setBaseUrl(ctx: Context, v: String) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("base_url", v).apply()
    }

    fun getApiKey(ctx: Context): String =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString("api_key", "")!!

    fun setApiKey(ctx: Context, v: String) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("api_key", v).apply()
    }

    fun getModel(ctx: Context): String =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString("model", DEFAULT_MODEL)!!

    fun setModel(ctx: Context, v: String) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("model", v).apply()
    }

    fun createClient(ctx: Context): LLMClient =
        LLMClient(getBaseUrl(ctx), getApiKey(ctx), getModel(ctx))
}
