package com.promptoptimizer.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.promptoptimizer.PromptOptimizerApp
import com.promptoptimizer.core.PromptEngine
import com.promptoptimizer.core.SearchEngine
import com.promptoptimizer.data.Repository
import com.promptoptimizer.model.FavoriteItem
import com.promptoptimizer.model.PromptRecord
import com.promptoptimizer.model.Role
import com.promptoptimizer.model.SessionState
import com.promptoptimizer.model.Template
import com.promptoptimizer.model.TemplateType

/**
 * 统一 ViewModel：持有全局数据仓库、搜索与提示词引擎，并管理各工作区状态。
 */
class MainViewModel(app: Application) : AndroidViewModel(app) {

    val repo: Repository = (app as PromptOptimizerApp).repository

    // ===== 搜索状态 =====
    var templateSearchQuery by mutableStateOf("")
    var historySearchQuery by mutableStateOf("")
    var favoritesSearchQuery by mutableStateOf("")
    var favoritesSelectedCategory by mutableStateOf<String?>(null)

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
    var parsedEvalReport by mutableStateOf<PromptEngine.ParsedEvaluation?>(null)

    // ===== 搜索与过滤列表 =====

    fun searchTemplates(query: String = templateSearchQuery, type: TemplateType? = null): List<Template> {
        return SearchEngine.searchTemplates(query, repo.getTemplates(), type)
    }

    fun searchHistory(query: String = historySearchQuery, op: String? = null): List<PromptRecord> {
        return SearchEngine.searchHistory(query, repo.getHistory(), op)
    }

    fun searchFavorites(query: String = favoritesSearchQuery, category: String? = favoritesSelectedCategory): List<FavoriteItem> {
        return SearchEngine.searchFavorites(query, repo.getFavorites(), category)
    }

    fun template(): Template? = repo.getTemplate(workspaceTemplateId)

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
        val result = iterateResult
        repo.addRecord("iterate", iterateInput, sent, result, workspaceModeLabel(), "iterate", "通用迭代")
        iterateSentPrompt = null
        iterateResult = ""
        iterateInput = ""
        // 迭代结果回填到工作区
        workspaceResult = result
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
        // 使用 PromptEngine 的结构化解析器智能提取变量
        val extracted = PromptEngine.parseExtractedVariables(variableResult)
        if (extracted.isNotEmpty()) {
            variableList = extracted.map { it.name }.distinct()
            val initialValues = extracted.filter { it.value.isNotBlank() }.associate { it.name to it.value }
            if (initialValues.isNotEmpty()) {
                variableValues = variableValues + initialValues
            }
        }
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
        // 自动解析示例值
        val parsedValues = PromptEngine.parseVariableValues(variableResult)
        if (parsedValues.isNotEmpty()) {
            variableValues = variableValues + parsedValues
        }
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
        parsedEvalReport = PromptEngine.parseEvaluationReport(evalResult)
        repo.addRecord("evaluate", testSystemPrompt, sent, evalResult, "测试评估", "evaluation", "评估")
        evalSentPrompt = null
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
