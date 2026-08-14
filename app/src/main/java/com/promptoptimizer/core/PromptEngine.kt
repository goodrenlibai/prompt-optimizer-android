package com.promptoptimizer.core

import com.promptoptimizer.model.ChatMessage
import com.promptoptimizer.model.Role
import com.promptoptimizer.model.Template
import com.promptoptimizer.template.MustacheRenderer

/**
 * 提示词引擎 —— 人工发送模式的核心。
 *
 * 原项目里，模板 + 用户输入会调用 LLM 直接得到结果；这里我们把"调用 LLM"替换为
 * "渲染出一段可复制的提示词文本"（[sentPrompt]），由用户复制后发给任意在线免费 AI。
 * 用户把 AI 的回复粘贴回来，作为该操作的结果（优化结果 / 测试结果 / 评估结果等）。
 */
object PromptEngine {

    private val renderer: MustacheRenderer get() = MustacheRenderer.default()

    // ===== 通用渲染 =====

    /**
     * 把模板渲染成一段可复制的提示词文本。
     *
     * - 简单模板：系统指令 + "\n\n" + 用户原始提示词（不改动模板内容，保留其中的 {{变量}} 字面量）。
     *   若模板声明 renderContent=true（如 test），则先对内容做 Mustache 渲染。
     * - 数组模板：逐条按 Mustache 渲染，再按角色拼接为完整文本。
     */
    fun renderSentPrompt(template: Template, context: Map<String, Any?>, originalPrompt: String): String {
        if (template.isSimple) {
            val content = template.content ?: ""
            return if (template.renderContent) {
                renderer.render(content, context)
            } else {
                val userPrompt = context["originalPrompt"] as? String ?: originalPrompt
                content.trimEnd() + "\n\n" + userPrompt.trim()
            }
        }
        return renderMessages(template.messages, context)
    }

    /** 数组模板渲染：逐条渲染后按角色拼接。 */
    private fun renderMessages(messages: List<ChatMessage>, context: Map<String, Any?>): String {
        val parts = messages.map { msg ->
            val rendered = renderer.render(msg.content, context)
            val label = when (msg.role) {
                Role.system -> "【系统提示】"
                Role.user -> "【用户请求】"
                Role.assistant -> "【助手】"
                Role.tool -> "【工具】"
            }
            "$label\n$rendered"
        }
        return parts.joinToString("\n\n")
    }

    private fun buildContext(vararg entries: Pair<String, Any?>): Map<String, Any?> {
        return entries.filter { it.second != null }.toMap()
    }

    // ===== 各操作 =====

    /** 优化（系统 / 用户提示词 / 图像）。 */
    fun optimizeSentPrompt(template: Template, originalPrompt: String): String {
        val ctx = buildContext("originalPrompt" to originalPrompt)
        return renderSentPrompt(template, ctx, originalPrompt)
    }

    /** 迭代。可携带上下文对话与工具文本。 */
    fun iterateSentPrompt(
        template: Template,
        lastOptimizedPrompt: String,
        iterateInput: String,
        conversationContext: String? = null,
        toolsText: String? = null
    ): String {
        val ctx = buildContext(
            "lastOptimizedPrompt" to lastOptimizedPrompt,
            "iterateInput" to iterateInput,
            "conversationContext" to conversationContext,
            "toolsContext" to toolsText
        )
        return renderSentPrompt(template, ctx, lastOptimizedPrompt)
    }

    /** 对话消息优化。 */
    fun messageOptimizeSentPrompt(
        template: Template,
        messages: List<ConversationMessage>,
        selectedId: String
    ): String {
        val conversation = messages.mapIndexed { idx, m ->
            mapOf(
                "index" to idx + 1,
                "roleLabel" to m.role.name,
                "content" to m.content,
                "isSelected" to (m.id == selectedId)
            )
        }
        val selected = messages.indexOfFirst { it.id == selectedId }
        val sel = messages.getOrNull(selected)
        val selectedMessage = if (sel != null) mapOf(
            "index" to selected + 1,
            "roleLabel" to sel.role.name,
            "content" to sel.content,
            "contentTooLong" to (sel.content.length > 200)
        ) else null

        val ctx = buildContext(
            "conversationMessages" to conversation,
            "selectedMessage" to selectedMessage
        )
        return renderSentPrompt(template, ctx, sel?.content ?: "")
    }

    /** 变量提取。 */
    fun variableExtractionSentPrompt(template: Template, promptContent: String): String {
        val ctx = buildContext("promptContent" to promptContent)
        return renderSentPrompt(template, ctx, promptContent)
    }

    /** 变量值生成。 */
    fun variableValueGenerationSentPrompt(
        template: Template,
        promptContent: String,
        variables: List<String>,
        contextVariables: Map<String, String> = emptyMap()
    ): String {
        val variablesText = variables.joinToString("\n") { "- $it" }
        val contextVariablesText = contextVariables.entries.joinToString("\n") { "- ${it.key}: ${it.value}" }
        val ctx = buildContext(
            "promptContent" to promptContent,
            "variablesText" to variablesText,
            "variableCount" to variables.size,
            "contextVariablesText" to contextVariablesText,
            "contextVariableCount" to contextVariables.size,
            "hasContextVariables" to (contextVariables.isNotEmpty())
        )
        return renderSentPrompt(template, ctx, promptContent)
    }

    /** 测试：给定系统提示词 + 用户输入，让 AI 执行并返回结果。 */
    fun testSentPrompt(template: Template, systemPrompt: String, userInput: String): String {
        val ctx = buildContext("systemPrompt" to systemPrompt, "userInput" to userInput)
        return renderSentPrompt(template, ctx, systemPrompt)
    }

    /** 结果评估。 */
    fun evalResultSentPrompt(
        template: Template,
        workspacePrompt: String,
        testResult: String,
        testCaseInput: String
    ): String {
        val ctx = buildContext(
            "workspacePrompt" to workspacePrompt,
            "testResult" to testResult,
            "testCaseInput" to testCaseInput
        )
        return renderSentPrompt(template, ctx, workspacePrompt)
    }

    /** 对比评估（原始 vs 优化）。 */
    fun evalCompareSentPrompt(
        template: Template,
        baselinePrompt: String,
        baselineResult: String,
        optimizedPrompt: String,
        optimizedResult: String,
        testCaseInput: String
    ): String {
        val ctx = buildContext(
            "baselinePrompt" to baselinePrompt,
            "baselineResult" to baselineResult,
            "optimizedPrompt" to optimizedPrompt,
            "optimizedResult" to optimizedResult,
            "testCaseInput" to testCaseInput
        )
        return renderSentPrompt(template, ctx, baselinePrompt)
    }

    /** 提示词分析（无需测试输出）。 */
    fun evalPromptOnlySentPrompt(template: Template, workspacePrompt: String): String {
        val ctx = buildContext("workspacePrompt" to workspacePrompt)
        return renderSentPrompt(template, ctx, workspacePrompt)
    }

    // 辅助类型
    data class ConversationMessage(val id: String, val role: Role, val content: String)
}
