package com.marvis.agentskills.ui

data class ChatMessageUi(
    val role: String,
    val content: String,
    val skillName: String? = null
)
