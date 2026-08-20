# AgentSkills — 内置 Skills 的 AI 对话助手（开源）

一个**开源空白骨架**基础上整合了来自 **ModelScope Skills / ModelScope MCP / Cocoloop Hub / SkillsMP** 生态的精选 Skills，并提供 **ChatGPT 风格对话栏**，由内置的 **SkillEngine** 根据用户意图自动路由并调用对应 skill，最终打包为 **兼容安卓 15（targetSdk 35）** 的 APK。

## 核心特性

- **Skill 引擎**：启动时扫描 `app/src/main/assets/skills/` 下的 `SKILL.md`，构建注册表；基于关键词对用户输入做意图路由，命中后把 skill 指令注入 LLM 上下文执行。
- **ChatGPT 风格对话栏**：流式对话 UI，兼容任意 OpenAI 协议（/v1/chat/completions）的大模型服务（OpenAI / DeepSeek / 各类中转）。
- **内置 17 个精选 Skill**：覆盖搜索研究、摘要、代码、办公、地图、网盘、浏览器自动化、任务管理等（来源标注在 `index.json` 的 `source` 字段）。
- **可扩展**：把任意 `SKILL.md` 目录丢进 `assets/skills/<id>/`，并在 `index.json` 登记一行即可新增技能，无需改代码。
- **安卓 15 兼容**：`compileSdk 35 / targetSdk 35`，ABI 覆盖 arm64-v8a / armeabi-v7a / x86_64。

## 技术栈

- Kotlin 2.0 + Jetpack Compose（Material3）
- OkHttp 流式 SSE 对话（OpenAI 兼容协议）
- kotlinx-serialization / coroutines
- Gradle 8.9 + Android Gradle Plugin 8.5.2

## 目录结构

```
AgentSkills-Android/
├── app/
│   ├── build.gradle.kts            # targetSdk 35、多 ABI
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── assets/skills/          # 内置 skill 包（index.json + <id>/SKILL.md）
│       ├── java/com/marvis/agentskills/
│       │   ├── MainActivity.kt
│       │   ├── AppConfig.kt
│       │   ├── engine/             # SkillEngine（注册表 + 意图路由）
│       │   ├── llm/                # LLMClient（流式对话）
│       │   └── ui/                 # Compose 对话界面 + 设置页
│       └── res/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── gradle/wrapper/
```

## 快速构建（本地）

> 需要 JDK 17+ 与 Android SDK（`ANDROID_HOME` 指向 SDK 根目录）。

```bash
# 1. 生成/补齐 gradle wrapper（若 gradle-wrapper.jar 缺失）
gradle wrapper --gradle-version 8.9

# 2. 构建 debug APK（会自动下载依赖）
./gradlew assembleDebug

# 3. 产物路径
#    app/build/outputs/apk/debug/app-debug.apk
```

要打 release 签名包：

```bash
./gradlew assembleRelease
# 产物：app/build/outputs/apk/release/app-release-unsigned.apk
```

## 一键构建脚本

项目提供 `scripts/build-apk.sh`，自动检测并安装缺失的 JDK/Android SDK，然后出包：

```bash
bash scripts/build-apk.sh
```

## CI 构建（免本地环境）

项目内已含 `.github/workflows/build-apk.yml`。推送到 GitHub 后，Actions 会自动装好 Android SDK 并构建 APK，产物可在 workflow 的 Artifacts 中下载，无需本机装任何工具。

## 使用

1. 打开 App，点右上角 ⚙ 进入设置；
2. 填入你的 API Base URL（默认 `https://api.openai.com/v1`）、API Key、模型名；
3. 保存后回到对话栏，直接输入需求，App 会自动匹配并调用内置 skill。

## Skill 来源说明

内置技能来自以下平台生态（详见各条目 `source` 字段）：

| 来源 | 说明 |
|------|------|
| `cocoloop` | Cocoloop Hub 精选榜（tavily-search-pro、summarize、docker-sandbox、agent-browser 等） |
| `modelscope` | ModelScope Skills 官方合集（skill-creator、GLM、MiniMax Office、高德、百度网盘等）与编程类 MCP（Hyperskill、Code Task Manager） |
| `skillsmp` | SkillsMP 导航站索引的公开 GitHub skills 生态（作为补充来源） |

> 受 APK 体积与运行环境限制，本版内置精选子集；`SKILL.md` 为指令式定义，联网/API 类 skill 的执行依赖对应服务。

## 开源协议

MIT License。欢迎 Fork、扩充 skill 包、提交 PR。
