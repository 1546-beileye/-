package com.marvis.agentskills.engine

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Skill 引擎：
 * 1. 启动时扫描 assets/skills/ 下所有 SKILL.md，构建注册表；
 * 2. 基于用户输入做关键词/语义匹配，选出最合适的 skill；
 * 3. 提供把 SKILL.md 指令注入 LLM 上下文的格式化能力。
 */
class SkillEngine(private val context: Context) {

    private val skills = mutableListOf<Skill>()
    private val instructions = mutableMapOf<String, String>()

    /** 加载所有内置 skill */
    fun load() {
        skills.clear()
        instructions.clear()
        val indexJson = readAsset("skills/index.json")
        if (indexJson.isNullOrBlank()) return
        try {
            val arr = JSONArray(indexJson)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val s = Skill(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    description = obj.getString("description"),
                    source = obj.optString("source", "builtin"),
                    keywords = obj.optJSONArray("keywords")?.let { a ->
                        (0 until a.length()).map { a.getString(it) }
                    } ?: emptyList(),
                    requiresNetwork = obj.optBoolean("requiresNetwork", true),
                    requiresApiKey = obj.optBoolean("requiresApiKey", false),
                    scriptLang = obj.optString("scriptLang", null)
                )
                skills.add(s)
                // 读取该 skill 的 SKILL.md 指令文本
                val md = readAsset("skills/${s.id}/SKILL.md")
                if (md != null) instructions[s.id] = md
            }
        } catch (e: Exception) {
            android.util.Log.e("SkillEngine", "parse index failed", e)
        }
    }

    fun allSkills(): List<Skill> = skills

    fun skillCount(): Int = skills.size

    fun getInstruction(id: String): String? = instructions[id]

    /**
     * 意图路由：按关键词打分，返回最佳匹配 skill 列表。
     */
    fun match(query: String, topK: Int = 3): List<Skill> {
        val q = query.lowercase()
        val scored = skills.map { s ->
            var score = 0
            if (q in s.name.lowercase()) score += 10
            for (kw in s.keywords) {
                if (kw.lowercase() in q) score += 5
            }
            for (word in q.split(Regex("\\s+"))) {
                if (word.length > 1 && word in s.description.lowercase()) score += 3
            }
            s to score
        }
        return scored.sortedByDescending { it.second }
            .filter { it.second > 0 }
            .take(topK)
            .map { it.first }
    }

    /**
     * 生成注入到 LLM 的系统提示片段：列出所有可用 skill。
     */
    fun buildSkillSystemPrompt(): String {
        val sb = StringBuilder()
        sb.append("你是 AgentSkills 助手。以下是你可调用的内置技能列表（skill）：\n")
        sb.append("<available_skills>\n")
        for (s in skills) {
            sb.append("- [${s.name}] (id=${s.id}, 来源=${s.source}) ${s.description}\n")
        }
        sb.append("</available_skills>\n")
        sb.append("当用户请求与某个 skill 匹配时，先读取对应 skill 的指令，按其说明完成任务。")
        return sb.toString()
    }

    private fun readAsset(path: String): String? = try {
        context.assets.open(path).bufferedReader().use { it.readText() }
    } catch (e: Exception) {
        null
    }
}
