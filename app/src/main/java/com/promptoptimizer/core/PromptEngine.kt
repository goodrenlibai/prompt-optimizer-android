package com.promptoptimizer.core

import com.promptoptimizer.model.ChatMessage
import com.promptoptimizer.model.Role
import com.promptoptimizer.model.Template
import com.promptoptimizer.template.MustacheRenderer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * 提示词与智能解析引擎 —— 人工发送模式的核心中枢。
 *
 * 职责：
 * 1. **提示词渲染（Prompt Rendering）**：基于 [MustacheRenderer] 将各模式模板与上下文精确拼接为待复制的结构化提示词；
 * 2. **AI 回复智能解析（AI Response Parser）**：当用户把 AI 回复粘贴回应用后，自动容错提取 JSON、Markdown 代码块、变量列表、变量值以及评估维度打分。
 */
object PromptEngine {

    private val renderer: MustacheRenderer get() = MustacheRenderer.default()

    private val lenientJson = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    // ===== 结构化解析数据模型 =====

    @Serializable
    data class ExtractedVariable(
        val name: String = "",
        val value: String = "",
        val reason: String = "",
        val category: String = ""
    )

    @Serializable
    data class EvaluationDimension(
        val key: String = "",
        val label: String = "",
        val score: Double = 0.0
    )

    @Serializable
    data class ParsedEvaluation(
        val overallScore: Double = 0.0,
        val dimensions: List<EvaluationDimension> = emptyList(),
        val improvements: List<String> = emptyList(),
        val summary: String = ""
    )

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
                val trimmed = content.trimEnd()
                if (userPrompt.isBlank()) trimmed else "$trimmed\n\n${userPrompt.trim()}"
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

    // ===== 各优化操作生成 =====

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

    // ===== 智能 AI 回复解析（AI Response Parser） =====

    /** 从文本中智能提取最外层有效 JSON 子串（支持过滤 Markdown ```json 代码块及前后引导语）。 */
    fun extractJsonString(rawText: String): String? {
        val trimmed = rawText.trim()
        // 1. 优先提取 ```json ... ``` 或 ``` ... ``` 代码块
        val codeBlockRegex = Regex("```(?:json)?\\s*([\\s\\S]*?)\\s*```", RegexOption.IGNORE_CASE)
        val match = codeBlockRegex.find(trimmed)
        if (match != null) {
            val inner = match.groupValues[1].trim()
            if ((inner.startsWith("{") && inner.endsWith("}")) || (inner.startsWith("[") && inner.endsWith("]"))) {
                return inner
            }
        }

        // 2. 查找首个 '{' 或 '[' 到对应最外层闭合字符
        val firstObj = trimmed.indexOf('{')
        val firstArr = trimmed.indexOf('[')
        val startIdx = when {
            firstObj != -1 && firstArr != -1 -> minOf(firstObj, firstArr)
            firstObj != -1 -> firstObj
            firstArr != -1 -> firstArr
            else -> -1
        }
        if (startIdx == -1) return null

        val isObject = trimmed[startIdx] == '{'
        val openChar = if (isObject) '{' else '['
        val closeChar = if (isObject) '}' else ']'

        var depth = 0
        var inString = false
        var escape = false

        for (i in startIdx until trimmed.length) {
            val ch = trimmed[i]
            if (escape) {
                escape = false
                continue
            }
            if (ch == '\\') {
                if (inString) escape = true
                continue
            }
            if (ch == '"') {
                inString = !inString
                continue
            }
            if (!inString) {
                if (ch == openChar) {
                    depth++
                } else if (ch == closeChar) {
                    depth--
                    if (depth == 0) {
                        return trimmed.substring(startIdx, i + 1)
                    }
                }
            }
        }

        return null
    }

    /** 智能解析变量提取回复（支持 JSON、Markdown 列表与正则 fallback）。 */
    fun parseExtractedVariables(text: String): List<ExtractedVariable> {
        val result = mutableListOf<ExtractedVariable>()
        val jsonStr = extractJsonString(text)

        if (jsonStr != null) {
            try {
                val element = lenientJson.parseToJsonElement(jsonStr)
                if (element is JsonObject) {
                    val variablesArray = element["variables"]?.let { if (it is kotlinx.serialization.json.JsonArray) it else null }
                    if (variablesArray != null) {
                        for (item in variablesArray) {
                            if (item is JsonObject) {
                                val name = item["name"]?.jsonPrimitive?.content ?: ""
                                val value = item["value"]?.jsonPrimitive?.content ?: ""
                                val reason = item["reason"]?.jsonPrimitive?.content ?: ""
                                val category = item["category"]?.jsonPrimitive?.content ?: ""
                                if (name.isNotBlank()) {
                                    result.add(ExtractedVariable(name, value, reason, category))
                                }
                            }
                        }
                    }
                } else if (element is kotlinx.serialization.json.JsonArray) {
                    for (item in element) {
                        if (item is JsonObject) {
                            val name = item["name"]?.jsonPrimitive?.content ?: ""
                            val value = item["value"]?.jsonPrimitive?.content ?: ""
                            if (name.isNotBlank()) {
                                result.add(ExtractedVariable(name, value))
                            }
                        }
                    }
                }
            } catch (_: Exception) {
                // JSON 解析容错降级
            }
        }

        if (result.isEmpty()) {
            // 正则双重容错：匹配 "name": "xxx" 或 {{xxx}} 或列表
            val nameRegex = Regex("\"name\"\\s*:\\s*\"([^\"]+)\"")
            nameRegex.findAll(text).forEach { m ->
                val name = m.groupValues[1].trim()
                if (name.isNotBlank() && result.none { it.name == name }) {
                    result.add(ExtractedVariable(name = name))
                }
            }
        }

        if (result.isEmpty()) {
            val bracketRegex = Regex("\\{\\{([^{}]+)\\}\\}")
            bracketRegex.findAll(text).forEach { m ->
                val name = m.groupValues[1].trim()
                if (name.isNotBlank() && result.none { it.name == name }) {
                    result.add(ExtractedVariable(name = name))
                }
            }
        }

        return result
    }

    /** 智能解析变量值生成回复。 */
    fun parseVariableValues(text: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val jsonStr = extractJsonString(text)

        if (jsonStr != null) {
            try {
                val element = lenientJson.parseToJsonElement(jsonStr)
                if (element is JsonObject) {
                    val valuesArray = element["values"]?.let { if (it is kotlinx.serialization.json.JsonArray) it else null }
                    if (valuesArray != null) {
                        for (item in valuesArray) {
                            if (item is JsonObject) {
                                val name = item["name"]?.jsonPrimitive?.content ?: ""
                                val value = item["value"]?.jsonPrimitive?.content ?: ""
                                if (name.isNotBlank()) {
                                    result[name] = value
                                }
                            }
                        }
                    }
                }
            } catch (_: Exception) {
                // 忽略
            }
        }

        if (result.isEmpty()) {
            val regex = Regex("\"name\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*\"value\"\\s*:\\s*\"([^\"]+)\"")
            regex.findAll(text).forEach { m ->
                val name = m.groupValues[1].trim()
                val value = m.groupValues[2].trim()
                if (name.isNotBlank()) result[name] = value
            }
        }

        return result
    }

    /** 智能解析评估报告。 */
    fun parseEvaluationReport(text: String): ParsedEvaluation? {
        val jsonStr = extractJsonString(text) ?: return null
        return try {
            val element = lenientJson.parseToJsonElement(jsonStr) as? JsonObject ?: return null
            val scoreObj = element["score"] as? JsonObject
            val overall = scoreObj?.get("overall")?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0

            val dims = mutableListOf<EvaluationDimension>()
            val dimsArr = scoreObj?.get("dimensions")?.let { if (it is kotlinx.serialization.json.JsonArray) it else null }
            if (dimsArr != null) {
                for (d in dimsArr) {
                    if (d is JsonObject) {
                        val key = d["key"]?.jsonPrimitive?.content ?: ""
                        val label = d["label"]?.jsonPrimitive?.content ?: key
                        val score = d["score"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0
                        dims.add(EvaluationDimension(key, label, score))
                    }
                }
            }

            val improvements = mutableListOf<String>()
            val impsArr = element["improvements"]?.let { if (it is kotlinx.serialization.json.JsonArray) it else null }
            if (impsArr != null) {
                for (imp in impsArr) {
                    improvements.add(imp.jsonPrimitive.content)
                }
            }

            val summary = element["summary"]?.jsonPrimitive?.content ?: ""

            ParsedEvaluation(overall, dims, improvements, summary)
        } catch (_: Exception) {
            null
        }
    }

    // 辅助类型
    data class ConversationMessage(val id: String, val role: Role, val content: String)
}
