package com.marvis.agentskills.engine

import kotlinx.serialization.Serializable

/**
 * 内置 Skill 的注册信息。
 * 每个 skill 对应 assets/skills/<id>/ 下的一个目录，
 * 目录内包含 SKILL.md（指令）与可选 scripts/、references/ 等资源。
 */
@Serializable
data class Skill(
    val id: String,
    val name: String,
    val description: String,
    val source: String,   // 来源：modelscope / cocoloop / skillsmp / builtin
    val keywords: List<String> = emptyList(),
    val requiresNetwork: Boolean = true,
    val requiresApiKey: Boolean = false,
    val scriptLang: String? = null // 可执行脚本语言（kotlin/python/node/js/...）
)
