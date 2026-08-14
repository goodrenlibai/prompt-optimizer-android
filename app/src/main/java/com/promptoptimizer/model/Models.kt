package com.promptoptimizer.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * 消息角色，与原项目一致：system / user / assistant / tool。
 * 在人工发送模式下，模板会被渲染成一段「系统提示 + 用户请求」的文本，
 * 由用户复制后发给任意在线免费 AI；AI 的回复粘贴回来成为结果。
 */
@Serializable
enum class Role { system, user, assistant, tool }

@Serializable
data class ChatMessage(
    val role: Role = Role.user,
    val content: String = ""
)

/**
 * 模板类型（对应原项目的 TemplateType）。用于模板分类与各工作区筛选。
 */
@Serializable
enum class TemplateType {
    optimize,           // 系统提示词优化
    userOptimize,       // 用户提示词优化
    iterate,            // 迭代
    contextIterate,     // 上下文感知迭代
    conversationMessageOptimize, // 多轮对话单条消息优化
    variableExtraction, // 变量提取
    variableValueGeneration, // 变量值生成
    evaluation,         // 评估 / 分析 / 对比
    test,               // 测试
    text2imageOptimize, // 文生图提示词优化
    image2imageOptimize,// 图生图提示词优化
    multiimageOptimize, // 多图生图提示词优化
    imageIterate;       // 图像提示词迭代

    val zhName: String
        get() = when (this) {
            optimize -> "系统提示词优化"
            userOptimize -> "用户提示词优化"
            iterate -> "迭代"
            contextIterate -> "上下文迭代"
            conversationMessageOptimize -> "对话消息优化"
            variableExtraction -> "变量提取"
            variableValueGeneration -> "变量值生成"
            evaluation -> "评估"
            test -> "测试"
            text2imageOptimize -> "文生图优化"
            image2imageOptimize -> "图生图优化"
            multiimageOptimize -> "多图生图优化"
            imageIterate -> "图像迭代"
        }
}

/**
 * 模板。content 支持两种形态（与原项目一致）：
 * - 简单字符串：渲染时 = system 指令 + "\n\n" + 用户原始提示词
 * - 消息数组：按 Mustache 模板逐条渲染，最终拼成一段可复制文本
 */
@Serializable
data class Template(
    val id: String,
    val name: String,
    val description: String = "",
    val type: TemplateType = TemplateType.optimize,
    val content: String? = null,             // 简单字符串模板
    val messages: List<ChatMessage> = emptyList(), // 数组模板
    val isBuiltin: Boolean = true,
    val lastModified: Long = 0L,
    val renderContent: Boolean = false       // 简单模板是否需按 Mustache 渲染（如 test 模板）
) {
    val isSimple: Boolean get() = content != null

    companion object {
        fun fromSimple(id: String, name: String, content: String, type: TemplateType, desc: String = "", renderContent: Boolean = false): Template =
            Template(id = id, name = name, description = desc, type = type, content = content, isBuiltin = true, renderContent = renderContent)

        fun fromMessages(
            id: String, name: String, messages: List<ChatMessage>, type: TemplateType, desc: String = ""
        ): Template =
            Template(id = id, name = name, description = desc, type = type, messages = messages, isBuiltin = true)
    }
}

/** 提示词记录 / 历史链节点。 */
@Serializable
data class PromptRecord(
    val id: String = "",
    val chainId: String = "",
    val operation: String = "optimize", // optimize / iterate / test / evaluate / messageOptimize ...
    val input: String = "",             // 原始输入
    val sentPrompt: String = "",        // 生成后发给 AI 的那段提示词（人工发送模式）
    val output: String = "",            // 用户粘贴回来的 AI 回复
    val modeLabel: String = "",         // 例如：基础-系统
    val templateId: String = "",
    val templateName: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

/** 收藏（资源感知的提示词资产）。 */
@Serializable
data class FavoriteItem(
    val id: String = "",
    val name: String = "",
    val content: String = "",
    val originalContent: String = "",
    val note: String = "",
    val category: String = "",
    val tags: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

/** 会话状态（单会话真源，含优化链版本）。 */
@Serializable
data class SessionState(
    val id: String = "basic-system",
    val label: String = "基础-系统",
    val currentInput: String = "",
    val optimizedVersions: MutableList<String> = mutableListOf(),
    val currentVersionIndex: Int = -1
)

@Serializable
data class Category(
    val id: String = "",
    val name: String = ""
)

/** 持久化 JSON 配置对象。 */
@Serializable
data class AppData(
    val templates: MutableList<Template> = mutableListOf(),
    val history: MutableList<PromptRecord> = mutableListOf(),
    val favorites: MutableList<FavoriteItem> = mutableListOf(),
    val categories: MutableList<Category> = mutableListOf(),
    val sessions: MutableList<SessionState> = mutableListOf(),
    val selectedMode: String = "basic",
    val selectedSubMode: String = "system"
)

object Json {
    val json: Json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }
}
