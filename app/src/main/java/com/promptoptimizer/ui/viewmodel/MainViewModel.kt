package com.promptoptimizer.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.promptoptimizer.PromptOptimizerApp
import com.promptoptimizer.core.PromptEngine
import com.promptoptimizer.data.Repository
import com.promptoptimizer.model.Role
import com.promptoptimizer.model.SessionState
import com.promptoptimizer.model.Template
import com.promptoptimizer.model.TemplateType

/**
 * 单一 ViewModel：持有仓库，并管理各工作区（优化/迭代/对话/变量/测试/评估）的状态。
 *
 * 人工发送模式的统一流程：
 *  1. 用户点击按钮 → [xxxSentPrompt] 生成待复制的提示词文本（存于 [sentPrompt]）
 *  2. UI 弹出对话框，用户复制后发给任意在线免费 AI
 *  3. 用户把 AI 回复粘贴进对话框 → [recordXxxResult] 保存结果与历史
 */
class MainViewModel(app: Application) : AndroidViewModel(app) {

    val repo: Repository = (app as PromptOptimizerApp).repository

    // ===== 工作台（基础系统/用户 + 图像）=====
    var workspaceInput by mutableStateOf("")
    var workspaceTemplateId by mutableStateOf("general-optimize")
    var workspaceResult by mutableStateOf("")
    var workspaceMode by mutableStateOf("system") // system / user / text2image / image2image / multiimage
    var workspaceSentPrompt by mutableStateOf<String?>(null)

    // ===== 迭代 =====
    var iterateInput by mutableStateOf("")
    var iterateSentPrompt by mutableStateOf<String?>(null)
    var iterateResult by mutableStateOf("")

    // ===== 专业-多轮对话 =====
    data class ConvMsg(val id: String, val role: Role, val content: String)
    var conversationMessages by mutableStateOf<List<ConvMsg>>(emptyList())
    var selectedMessageId by mutableStateOf("")
    var conversationSentPrompt by mutableStateOf<String?>(null)
    var conversationResult by mutableStateOf("")

    // ===== 专业-变量 =====
    var variablePrompt by mutableStateOf("")
    var variableList by mutableStateOf<List<String>>(emptyList())
    var variableValues by mutableStateOf<Map<String, String>>(emptyMap())
    var variableSentPrompt by mutableStateOf<String?>(null)
    var variableResult by mutableStateOf("")

    // ===== 测试 / 评估 =====
    var testSystemPrompt by mutableStateOf("")
    var testUserInput by mutableStateOf("")
    var testSentPrompt by mutableStateOf<String?>(null)
    var testResult by mutableStateOf("")
    var evalSentPrompt by mutableStateOf<String?>(null)
    var evalResult by mutableStateOf("")

    fun template(): Template? = repo.getTemplate(workspaceTemplateId)
    fun templateFor(type: TemplateType): Template? =
        repo.getTemplates(type).firstOrNull { it.isBuiltin }
            ?: repo.getTemplates(type).firstOrNull()

    fun workspaceTemplates(): List<Template> {
        val type = when (workspaceMode) {
            "user" -> TemplateType.userOptimize
            "text2image" -> TemplateType.text2imageOptimize
            "image2image" -> TemplateType.image2imageOptimize
            "multiimage" -> TemplateType.multiimageOptimize
            else -> TemplateType.optimize
        }
        return repo.getTemplates(type)
    }

    // ===== 工作台操作 =====

    fun generateWorkspacePrompt() {
        val template = template() ?: return
        workspaceSentPrompt = PromptEngine.optimizeSentPrompt(template, workspaceInput)
    }

    fun setWorkspaceTemplateId(id: String) {
        workspaceTemplateId = id
    }

    fun recordWorkspaceResult() {
        val sent = workspaceSentPrompt ?: return
        if (workspaceResult.isBlank()) return
        repo.addRecord(
            operation = "optimize",
            input = workspaceInput,
            sentPrompt = sent,
            output = workspaceResult,
            modeLabel = workspaceModeLabel(),
            templateId = template()?.id ?: "",
            templateName = template()?.name ?: ""
        )
        workspaceSentPrompt = null
        workspaceResult = ""
    }

    private fun workspaceModeLabel(): String = when (workspaceMode) {
        "user" -> "基础-用户"
        "text2image" -> "图像-文生图"
        "image2image" -> "图像-图生图"
        "multiimage" -> "图像-多图"
        else -> "基础-系统"
    }

    // ===== 迭代 =====

    fun generateIteratePrompt() {
        val base = workspaceResult.ifBlank { workspaceSentPrompt ?: workspaceInput }
        val t = repo.getTemplate("iterate") ?: return
        iterateSentPrompt = PromptEngine.iterateSentPrompt(t, base, iterateInput)
    }

    fun recordIterateResult() {
        val sent = iterateSentPrompt ?: return
        if (iterateResult.isBlank()) return
        repo.addRecord("iterate", iterateInput, sent, iterateResult, workspaceModeLabel(), "iterate", "通用迭代")
        iterateSentPrompt = null
        iterateResult = ""
        iterateInput = ""
        workspaceResult = iterateResult
    }

    // ===== 对话 =====

    fun addConversationMessage(role: Role) {
        val msg = ConvMsg("c${System.currentTimeMillis()}", role, "")
        conversationMessages = conversationMessages + msg
        if (selectedMessageId.isBlank()) selectedMessageId = msg.id
    }

    fun updateConversationMessage(id: String, content: String) {
        conversationMessages = conversationMessages.map {
            if (it.id == id) it.copy(content = content) else it
        }
    }

    fun generateConversationPrompt() {
        val t = repo.getTemplate("context-message-optimize") ?: return
        if (selectedMessageId.isBlank()) return
        val msgs = conversationMessages.map { PromptEngine.ConversationMessage(it.id, it.role, it.content) }
        conversationSentPrompt = PromptEngine.messageOptimizeSentPrompt(t, msgs, selectedMessageId)
    }

    fun recordConversationResult() {
        val sent = conversationSentPrompt ?: return
        if (conversationResult.isBlank()) return
        repo.addRecord("messageOptimize", "", sent, conversationResult, "专业-多轮对话", "context-message-optimize", "对话消息优化")
        conversationSentPrompt = null
        conversationResult = ""
    }

    // ===== 变量 =====

    fun generateVariableExtractionPrompt() {
        val t = repo.getTemplate("variable-extraction") ?: return
        variableSentPrompt = PromptEngine.variableExtractionSentPrompt(t, variablePrompt)
    }

    fun applyExtractionNamesFromResult() {
        // 简单解析：若用户粘贴的回复中包含 JSON "name" 字段，尝试提取变量名。
        val names = Regex("\"name\"\\s*:\\s*\"([^\"]+)\"").findAll(variableResult)
            .mapNotNull { it.groupValues.getOrNull(1)?.takeIf { n -> n.isNotBlank() } }
            .toList()
        if (names.isNotEmpty()) variableList = names.distinct()
    }

    fun generateVariableValuePrompt() {
        val t = repo.getTemplate("variable-value-generation") ?: return
        if (variableList.isEmpty()) return
        variableSentPrompt = PromptEngine.variableValueGenerationSentPrompt(
            t, variablePrompt, variableList, variableValues
        )
    }

    fun recordVariableResult() {
        val sent = variableSentPrompt ?: return
        if (variableResult.isBlank()) return
        repo.addRecord("variable", variablePrompt, sent, variableResult, "专业-变量", "variable-extraction", "变量")
        variableSentPrompt = null
        variableResult = ""
    }

    // ===== 测试 / 评估 =====

    fun generateTestPrompt() {
        val t = repo.getTemplate("test") ?: return
        testSentPrompt = PromptEngine.testSentPrompt(t, testSystemPrompt, testUserInput)
    }

    fun recordTestResult() {
        val sent = testSentPrompt ?: return
        if (testResult.isBlank()) return
        repo.addRecord("test", testUserInput, sent, testResult, "测试", "test", "测试提示词")
        testSentPrompt = null
        testResult = ""
    }

    fun generateEvalPrompt(type: String) {
        val t = when (type) {
            "compare" -> repo.getTemplate("evaluation-compare")
            "promptOnly" -> repo.getTemplate("evaluation-prompt-only")
            else -> repo.getTemplate("evaluation-result")
        } ?: return
        evalSentPrompt = when (type) {
            "compare" -> PromptEngine.evalCompareSentPrompt(
                t, testSystemPrompt, workspaceResult, testSystemPrompt, testResult, testUserInput
            )
            "promptOnly" -> PromptEngine.evalPromptOnlySentPrompt(t, testSystemPrompt)
            else -> PromptEngine.evalResultSentPrompt(t, testSystemPrompt, testResult, testUserInput)
        }
    }

    fun recordEvalResult() {
        val sent = evalSentPrompt ?: return
        if (evalResult.isBlank()) return
        repo.addRecord("evaluate", testSystemPrompt, sent, evalResult, "测试评估", "evaluation", "评估")
        evalSentPrompt = null
        evalResult = ""
    }

    // ===== 会话持久化（切换工作区时保存当前输入）=====

    fun saveWorkspaceSession() {
        val id = "basic-${if (workspaceMode == "user") "user" else "system"}"
        repo.saveSession(SessionState(id = id, label = id, currentInput = workspaceInput))
    }

    fun restoreWorkspaceSession() {
        val id = "basic-${if (workspaceMode == "user") "user" else "system"}"
        workspaceInput = repo.getSession(id).currentInput
    }

    fun setMode(mode: String) {
        saveWorkspaceSession()
        workspaceMode = mode
        val first = workspaceTemplates().firstOrNull()
        workspaceTemplateId = first?.id ?: workspaceTemplateId
        restoreWorkspaceSession()
    }
}
