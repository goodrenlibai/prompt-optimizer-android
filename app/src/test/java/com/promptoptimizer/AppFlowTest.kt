package com.promptoptimizer

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.promptoptimizer.ui.nav.NavGraph
import com.promptoptimizer.ui.theme.PromptOptimizerTheme
import com.promptoptimizer.ui.viewmodel.MainViewModel
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 基于 Robolectric 的 Compose UI 端到端测试。
 *
 * 覆盖：每个页面渲染、每个导航、以及所有交互（生成可复制提示词 → 复制 →
 * 粘贴 AI 回复 → 保存结果）。在 JVM 上运行，无需模拟器。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppFlowTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Before
    fun setUp() {
        val app = ApplicationProvider.getApplicationContext<PromptOptimizerApp>()
        app.repository.resetForTest()
        val viewModel = MainViewModel(app)
        composeRule.setContent {
            PromptOptimizerTheme {
                NavGraph(viewModel)
            }
        }
        composeRule.waitForIdle()
    }

    private fun clickBottom(label: String) {
        composeRule.onNodeWithText(label).performClick()
        composeRule.waitForIdle()
    }

    private fun pasteAndConfirm(output: String) {
        composeRule.onNodeWithTag("pastedOutput").performTextInput(output)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("confirmButton").performClick()
        composeRule.waitForIdle()
    }

    // ============ 首页渲染 ============

    @Test
    fun homeScreenRenders() {
        composeRule.onNodeWithText("提示词优化器").assertExists()
        composeRule.onNodeWithText("人工发送模式 · 无需配置任何 API").assertExists()
        composeRule.onNodeWithText("模板管理").assertExists()
        composeRule.onNodeWithText("历史记录").assertExists()
        composeRule.onNodeWithText("如何使用（三步）").assertExists()
        for (label in listOf("首页", "工作台", "专业", "测试评估", "收藏")) {
            composeRule.onNodeWithText(label).assertExists()
        }
    }

    @Test
    fun homeNavigatesToTemplates() {
        composeRule.onNodeWithText("模板管理").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("新增自定义模板").assertExists()
    }

    @Test
    fun homeNavigatesToHistory() {
        composeRule.onNodeWithText("历史记录").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("暂无历史记录").assertExists()
    }

    // ============ 工作台：优化 + 迭代全流程 ============

    @Test
    fun workspaceOptimizeFlow() {
        clickBottom("工作台")
        composeRule.onNodeWithText("工作台 · 基础·系统").assertExists()
        composeRule.onNodeWithText("基础·用户").performClick()
        composeRule.onNodeWithText("工作台 · 基础·用户").assertExists()
        composeRule.onNodeWithText("基础·系统").performClick()
        composeRule.onNodeWithText("工作台 · 基础·系统").assertExists()

        composeRule.onNodeWithTag("workspaceInput").performTextInput("你是一位客服助手")
        composeRule.onNodeWithTag("workspaceGenerate").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("copyButton").assertExists()
        composeRule.onNodeWithTag("copyButton").performClick()
        pasteAndConfirm("你是一位专业的客服助手，请用友好语气解答问题。")
        composeRule.onNodeWithText("最近一次优化结果").assertExists()

        val history = ApplicationProvider.getApplicationContext<PromptOptimizerApp>().repository.getHistory()
        assertTrue(history.isNotEmpty())
        assertTrue(history.any { it.operation == "optimize" })
    }

    @Test
    fun workspaceIterateFlow() {
        clickBottom("工作台")
        composeRule.onNodeWithTag("workspaceInput").performTextInput("你是一位写作助手")
        composeRule.onNodeWithTag("workspaceGenerate").performClick()
        composeRule.waitForIdle()
        pasteAndConfirm("你是一位专业的写作顾问")

        composeRule.onNodeWithTag("iterateInput").performTextInput("更专业一些")
        composeRule.onNodeWithTag("iterateGenerate").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("copyButton").assertExists()
        pasteAndConfirm("你是一位资深写作顾问，拥有丰富的创作经验。")
        composeRule.onNodeWithText("最近一次优化结果").assertExists()

        val history = ApplicationProvider.getApplicationContext<PromptOptimizerApp>().repository.getHistory()
        assertTrue(history.any { it.operation == "iterate" })
    }

    @Test
    fun workspaceModeSwitchKeepsSessions() {
        clickBottom("工作台")
        composeRule.onNodeWithTag("workspaceInput").performTextInput("基础系统内容")
        composeRule.onNodeWithText("图像·文生图").performClick()
        composeRule.onNodeWithText("工作台 · 图像·文生图").assertExists()
        composeRule.onNodeWithText("基础·系统").performClick()
        composeRule.onNodeWithText("工作台 · 基础·系统").assertExists()
        composeRule.onNodeWithTag("workspaceInput").assertExists()
    }

    // ============ 专业：多轮对话 ============

    @Test
    fun proConversationFlow() {
        clickBottom("专业")
        composeRule.onNodeWithText("多轮对话").performClick()
        composeRule.onNodeWithText("添加用户消息").performClick()
        composeRule.waitForIdle()
        composeRule.onAllNodesWithTag("msgContent").onFirst().performTextInput("帮我写一个排序函数")
        composeRule.onNodeWithText("优化选中消息（生成提示词）").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("copyButton").assertExists()
        pasteAndConfirm("请帮我用 Python 编写一个快速排序函数。")
        val history = ApplicationProvider.getApplicationContext<PromptOptimizerApp>().repository.getHistory()
        assertTrue(history.any { it.operation == "messageOptimize" })
    }

    // ============ 专业：变量模式 ============

    @Test
    fun proVariableFlow() {
        clickBottom("专业")
        composeRule.onNodeWithText("变量模式").performClick()
        composeRule.onNodeWithTag("variablePrompt").performTextInput("写一篇关于{{主题}}的文章，要求{{字数}}字")
        composeRule.onNodeWithTag("variableExtract").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("copyButton").assertExists()
        pasteAndConfirm("""{"variables":[{"name":"主题","value":"科技"}]}""")
        composeRule.onNodeWithTag("variableValue").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("copyButton").assertExists()
        pasteAndConfirm("""{"values":[{"name":"主题","value":"AI"}]}""")
    }

    // ============ 测试评估 ============

    @Test
    fun testevalTestFlow() {
        clickBottom("测试评估")
        composeRule.onNodeWithText("测试").performClick()
        composeRule.onNodeWithTag("testSystemPrompt").performTextInput("你是客服助手")
        composeRule.onNodeWithTag("testUserInput").performTextInput("我要退货")
        composeRule.onNodeWithTag("testGenerate").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("copyButton").assertExists()
        pasteAndConfirm("好的，已为您安排退货流程。")
    }

    @Test
    fun testevalResultEvalFlow() {
        clickBottom("测试评估")
        composeRule.onNodeWithText("结果评估").performClick()
        composeRule.onNodeWithTag("testSystemPrompt").performTextInput("你是客服助手")
        composeRule.onNodeWithTag("testResult").performTextInput("这是输出")
        composeRule.onNodeWithTag("evalGenerate").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("copyButton").assertExists()
        pasteAndConfirm("""{"score":{"overall":90,"dimensions":[]},"summary":"表现良好"}""")
    }

    @Test
    fun testevalCompareFlow() {
        clickBottom("测试评估")
        composeRule.onNodeWithText("对比评估").performClick()
        composeRule.onNodeWithTag("testSystemPrompt").performTextInput("旧提示词")
        composeRule.onNodeWithTag("workspaceResult").performTextInput("新提示词")
        composeRule.onNodeWithTag("testResult").performTextInput("新输出")
        composeRule.onNodeWithTag("evalGenerate").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("copyButton").assertExists()
        pasteAndConfirm("""{"score":{"overall":85,"dimensions":[]},"summary":"有提升"}""")
    }

    @Test
    fun testevalPromptOnlyFlow() {
        clickBottom("测试评估")
        composeRule.onNodeWithText("提示词分析").performClick()
        composeRule.onNodeWithTag("testSystemPrompt").performTextInput("你是助手")
        composeRule.onNodeWithTag("evalGenerate").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("copyButton").assertExists()
        pasteAndConfirm("""{"score":{"overall":80,"dimensions":[]},"summary":"设计良好"}""")
    }

    // ============ 收藏 ============

    @Test
    fun favoritesAddAndDelete() {
        clickBottom("收藏")
        composeRule.onNodeWithTag("favName").performTextInput("我的收藏")
        composeRule.onNodeWithTag("favContent").performTextInput("你是一位专家")
        composeRule.onNodeWithTag("favSave").performClick()
        composeRule.waitForIdle()
        clickBottom("首页")
        clickBottom("收藏")
        composeRule.onNodeWithText("我的收藏").assertExists()
        composeRule.onNodeWithTag("favDelete").performClick()
        composeRule.waitForIdle()
        clickBottom("首页")
        clickBottom("收藏")
        composeRule.onNodeWithText("我的收藏").assertDoesNotExist()
    }

    // ============ 模板管理 ============

    @Test
    fun templatesAddAndDeleteCustom() {
        clickBottom("首页")
        composeRule.onNodeWithText("模板管理").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tplName").performTextInput("我的自定义模板")
        composeRule.onNodeWithTag("tplContent").performTextInput("你是专家助手")
        composeRule.onNodeWithTag("tplSave").performClick()
        composeRule.waitForIdle()
        clickBottom("首页")
        composeRule.onNodeWithText("模板管理").performClick()
        composeRule.onNodeWithText("我的自定义模板").assertExists()
        composeRule.onNodeWithTag("tplDelete").performClick()
        composeRule.waitForIdle()
        clickBottom("首页")
        composeRule.onNodeWithText("模板管理").performClick()
        composeRule.onNodeWithText("我的自定义模板").assertDoesNotExist()
        composeRule.onNodeWithText("通用优化").assertExists()
    }

    // ============ 历史记录 ============

    @Test
    fun historyRecordsAfterOptimize() {
        clickBottom("工作台")
        composeRule.onNodeWithTag("workspaceInput").performTextInput("历史测试提示词")
        composeRule.onNodeWithTag("workspaceGenerate").performClick()
        composeRule.waitForIdle()
        pasteAndConfirm("优化后的历史内容")

        clickBottom("首页")
        composeRule.onNodeWithText("历史记录").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("优化后的历史内容").assertExists()
    }

    @Test
    fun historyClearWorks() {
        clickBottom("工作台")
        composeRule.onNodeWithTag("workspaceInput").performTextInput("x")
        composeRule.onNodeWithTag("workspaceGenerate").performClick()
        composeRule.waitForIdle()
        pasteAndConfirm("y")
        clickBottom("首页")
        composeRule.onNodeWithText("历史记录").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("清空").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("暂无历史记录").assertExists()
    }

    @Test
    fun historyDetailEditWorks() {
        clickBottom("工作台")
        composeRule.onNodeWithTag("workspaceInput").performTextInput("待编辑提示词")
        composeRule.onNodeWithTag("workspaceGenerate").performClick()
        composeRule.waitForIdle()
        pasteAndConfirm("原始输出内容")
        clickBottom("首页")
        composeRule.onNodeWithText("历史记录").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("原始输出内容").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("保存").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("原始输出内容").assertExists()
    }
}
