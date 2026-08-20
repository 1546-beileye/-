package com.marvis.agentskills

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.marvis.agentskills.engine.SkillEngine
import com.marvis.agentskills.ui.AgentSkillsApp

class MainActivity : ComponentActivity() {
    lateinit var skillEngine: SkillEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        skillEngine = SkillEngine(applicationContext).also { it.load() }
        setContent {
            AgentSkillsApp(skillEngine)
        }
    }
}
