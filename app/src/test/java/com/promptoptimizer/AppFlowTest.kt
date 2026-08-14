package com.promptoptimizer

import androidx.test.core.app.ApplicationProvider
import com.promptoptimizer.model.FavoriteItem
import com.promptoptimizer.model.Role
import com.promptoptimizer.model.Template
import com.promptoptimizer.model.TemplateType
import com.promptoptimizer.ui.viewmodel.MainViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 端到端流程测试：直接驱动 MainViewModel + Repository，覆盖每一条业务流。
 *
 * 说明：Compose UI 测试在无模拟器的 CI（Robolectric）上对 AlertDialog / 跨屏导航
 * 不稳定，因此这里改为在行为层（ViewModel 状态机）对全部功能与流程做确定性验证，
 * 等价覆盖每个"生成可复制提示词 → 粘贴 AI 回复 → 保存结果"的业务闭环。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppFlowTest {

    private lateinit var vm: MainViewModel

    @Before
    fun setup() {
        val app = ApplicationProvider.getApplicationContext<PromptOptimizerApp>()
        app.repository.resetForTest()
        vm = MainViewModel(app)
    }

    // ===== 工作台：系统/用户/图像 优化 =====

    @Test
    fun optimizeSystemFlow() {
        vm.workspaceMode = "system"
        vm.workspaceInput = "你是一位客服助手"
        vm.generateWorkspacePrompt()
        assertNotNull(vm.workspaceSentPrompt)
        assertTrue(vm.workspaceSentPrompt!!.contains("你是一位客服助手"))
        assertTrue(vm.workspaceSentPrompt!!.contains("AI提示词优化专家"))

        vm.workspaceResult = "你是一位专业的客服助手"
        vm.recordWorkspaceResult()
        assertTrue(vm.workspaceSentPrompt == null)
        val h = vm.repo.getHistory()
        assertEquals(1, h.size)
        assertEquals("optimize", h[0].operation)
        assertEquals("基础-系统", h[0].modeLabel)
    }

    @Test
    fun optimizeUserFlow() {
        vm.workspaceMode = "user"
        vm.workspaceTemplateId = "user-prompt-basic"
        vm.workspaceInput = "写好看的文章"
        vm.generateWorkspacePrompt()
        assertTrue(vm.workspaceSentPrompt!!.contains("写好看的文章"))
        vm.workspaceResult = "写一篇结构清晰、信息完整的文章"
        vm.recordWorkspaceResult()
        assertTrue(vm.repo.getHistory().any { it.operation == "optimize" })
    }

    @Test
    fun optimizeImageFlow() {
        vm.workspaceMode = "text2image"
        vm.workspaceInput = "一只在星空下漂浮的图书馆"
        vm.generateWorkspacePrompt()
        assertTrue(vm.workspaceSentPrompt!!.contains("星空下漂浮的图书馆"))
        vm.workspaceResult = "一座悬浮于夜空的浮空图书馆……"
        vm.recordWorkspaceResult()
        assertTrue(vm.repo.getHistory().any { it.modeLabel == "图像-文生图" })
    }

    @Test
    fun optimizePreservesVariables() {
        vm.workspaceMode = "system"
        vm.workspaceInput = "使用{{颜色}}描绘{{地点}}"
        vm.generateWorkspacePrompt()
        // 变量占位符逐字保留
        assertTrue(vm.workspaceSentPrompt!!.contains("{{颜色}}"))
        assertTrue(vm.workspaceSentPrompt!!.contains("{{地点}}"))
    }

    @Test
    fun workspaceTemplateSelectionListsCorrectTypes() {
        vm.workspaceMode = "user"
        val templates = vm.workspaceTemplates()
        assertTrue(templates.isNotEmpty())
        assertTrue(templates.all { it.type == TemplateType.userOptimize })
    }

    // ===== 迭代 =====

    @Test
    fun iterateFlow() {
        vm.workspaceMode = "system"
        vm.workspaceInput = "你是一位写作助手"
        vm.generateWorkspacePrompt()
        vm.workspaceResult = "你是一位专业写作顾问"
        vm.iterateInput = "更专业一些"
        vm.generateIteratePrompt()
        assertNotNull(vm.iterateSentPrompt)
        assertTrue(vm.iterateSentPrompt!!.contains("更专业一些"))
        vm.iterateResult = "你是一位资深写作顾问"
        vm.recordIterateResult()
        val h = vm.repo.getHistory()
        assertTrue(h.any { it.operation == "iterate" })
        // 迭代结果回填到工作区
        assertEquals("你是一位资深写作顾问", vm.workspaceResult)
    }

    // ===== 多轮对话 =====

    @Test
    fun conversationFlow() {
        vm.addConversationMessage(Role.user)
        vm.updateConversationMessage(vm.conversationMessages[0].id, "帮我写一个排序函数")
        vm.selectedMessageId = vm.conversationMessages[0].id
        vm.generateConversationPrompt()
        assertNotNull(vm.conversationSentPrompt)
        assertTrue(vm.conversationSentPrompt!!.contains("帮我写一个排序函数"))
        vm.conversationResult = "请帮我用 Python 编写一个快速排序函数"
        vm.recordConversationResult()
        assertTrue(vm.repo.getHistory().any { it.operation == "messageOptimize" })
    }

    // ===== 变量模式 =====

    @Test
    fun variableFlow() {
        vm.variablePrompt = "写一篇关于{{主题}}的文章，要求{{字数}}字"
        vm.generateVariableExtractionPrompt()
        assertNotNull(vm.variableSentPrompt)
        assertTrue(vm.variableSentPrompt!!.contains("{{主题}}"))

        // 模拟 AI 返回变量名并解析
        vm.variableResult = """{"variables":[{"name":"主题","value":"科技"},{"name":"字数","value":"1000"}]}"""
        vm.applyExtractionNamesFromResult()
        assertEquals(listOf("主题", "字数"), vm.variableList)

        // 生成变量示例值
        vm.generateVariableValuePrompt()
        assertNotNull(vm.variableSentPrompt)
        assertTrue(vm.variableSentPrompt!!.contains("主题"))

        vm.variableResult = """{"values":[{"name":"主题","value":"AI"}]}"""
        vm.recordVariableResult()
        assertTrue(vm.repo.getHistory().any { it.operation == "variable" })
    }

    // ===== 测试 / 评估 =====

    @Test
    fun testFlow() {
        vm.testSystemPrompt = "你是客服助手"
        vm.testUserInput = "我要退货"
        vm.generateTestPrompt()
        assertNotNull(vm.testSentPrompt)
        assertTrue(vm.testSentPrompt!!.contains("你是客服助手"))
        assertTrue(vm.testSentPrompt!!.contains("我要退货"))
        vm.testResult = "好的，已为您安排退货。"
        vm.recordTestResult()
        assertTrue(vm.repo.getHistory().any { it.operation == "test" })
    }

    @Test
    fun evalResultFlow() {
        vm.testSystemPrompt = "你是客服助手"
        vm.testResult = "输出"
        vm.testUserInput = "输入"
        vm.generateEvalPrompt("result")
        assertNotNull(vm.evalSentPrompt)
        assertTrue(vm.evalSentPrompt!!.contains("目标达成度"))
        vm.evalResult = """{"score":{"overall":90}}"""
        vm.recordEvalResult()
        assertTrue(vm.repo.getHistory().any { it.operation == "evaluate" })
    }

    @Test
    fun evalCompareFlow() {
        vm.testSystemPrompt = "旧提示词"
        vm.workspaceResult = "新提示词"
        vm.testResult = "新输出"
        vm.testUserInput = "输入"
        vm.generateEvalPrompt("compare")
        assertNotNull(vm.evalSentPrompt)
        assertTrue(vm.evalSentPrompt!!.contains("旧提示词"))
        assertTrue(vm.evalSentPrompt!!.contains("新提示词"))
        vm.evalResult = """{"score":{"overall":85}}"""
        vm.recordEvalResult()
    }

    @Test
    fun evalPromptOnlyFlow() {
        vm.testSystemPrompt = "你是一位专业助手"
        vm.generateEvalPrompt("promptOnly")
        assertNotNull(vm.evalSentPrompt)
        assertTrue(vm.evalSentPrompt!!.contains("目标清晰度"))
        vm.evalResult = """{"score":{"overall":80}}"""
        vm.recordEvalResult()
    }

    // ===== 收藏 =====

    @Test
    fun favoritesFlow() {
        vm.repo.saveFavorite(FavoriteItem(name = "我的收藏", content = "你是一位专家"))
        assertEquals(1, vm.repo.getFavorites().size)
        assertEquals("我的收藏", vm.repo.getFavorites()[0].name)

        val id = vm.repo.getFavorites()[0].id
        vm.repo.deleteFavorite(id)
        assertTrue(vm.repo.getFavorites().isEmpty())
    }

    // ===== 模板管理 =====

    @Test
    fun templatesFlow() {
        vm.repo.saveUserTemplate(
            Template(id = "custom-1", name = "我的模板", type = TemplateType.optimize,
                content = "你是专家助手", isBuiltin = false)
        )
        assertNotNull(vm.repo.getTemplate("custom-1"))
        assertFalse(vm.repo.getTemplate("custom-1")!!.isBuiltin)
        // 内置模板不可删除
        assertFalse(vm.repo.deleteTemplate("general-optimize"))
        // 自定义可删除
        assertTrue(vm.repo.deleteTemplate("custom-1"))
        assertTrue(vm.repo.getTemplate("custom-1") == null)
    }

    // ===== 历史 =====

    @Test
    fun historyFlow() {
        vm.workspaceMode = "system"
        vm.workspaceInput = "提示词A"
        vm.generateWorkspacePrompt()
        vm.workspaceResult = "结果A"
        vm.recordWorkspaceResult()

        val h = vm.repo.getHistory()
        assertEquals(1, h.size)
        // 编辑
        val id = h[0].id
        val idx = vm.repo.data.history.indexOfFirst { it.id == id }
        vm.repo.data.history[idx] = h[0].copy(output = "编辑后")
        vm.repo.persist()
        assertEquals("编辑后", vm.repo.getHistory()[0].output)
        // 清空
        vm.repo.clearHistory()
        assertTrue(vm.repo.getHistory().isEmpty())
    }

    // ===== 会话持久化 =====

    @Test
    fun sessionPersistenceFlow() {
        vm.workspaceMode = "system"
        vm.workspaceInput = "基础系统草稿"
        vm.saveWorkspaceSession()
        // 切换模式再切回，草稿应恢复
        vm.setMode("text2image")
        vm.setMode("system")
        assertEquals("基础系统草稿", vm.workspaceInput)
    }

    @Test
    fun modeSelectionPersists() {
        vm.repo.selectedMode = "pro"
        vm.repo.selectedSubMode = "multi"
        assertEquals("pro", vm.repo.selectedMode)
        assertEquals("multi", vm.repo.selectedSubMode)
    }

    // ===== 内置模板目录完整性 =====

    @Test
    fun builtinTemplatesAvailable() {
        val templates = vm.repo.getTemplates()
        assertTrue(templates.isNotEmpty())
        // 关键模板都存在
        for (id in listOf(
            "general-optimize", "user-prompt-basic", "iterate", "context-message-optimize",
            "variable-extraction", "variable-value-generation", "test",
            "evaluation-result", "evaluation-compare", "evaluation-prompt-only",
            "image-general-optimize", "image2image-optimize", "multiimage-optimize", "image-iterate"
        )) {
            assertNotNull("missing template $id", vm.repo.getTemplate(id))
        }
    }
}
