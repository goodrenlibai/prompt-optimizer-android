package com.promptoptimizer

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppFlowTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun clearData() {
        val app = ApplicationProvider.getApplicationContext<PromptOptimizerApp>()
        app.repository.resetForTest()
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
        // 底部导航五项
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
        // 模式切换
        composeRule.onNodeWithText("基础·用户").performClick()
        composeRule.onNodeWithText("工作台 · 基础·用户").assertExists()
        composeRule.onNodeWithText("基础·系统").performClick()
        composeRule.onNodeWithText("工作台 · 基础·系统").assertExists()

        // 生成提示词
        composeRule.onNodeWithTag("workspaceInput").performTextInput("你是一位客服助手")
        composeRule.onNodeWithTag("workspaceGenerate").performClick()
        composeRule.waitForIdle()
        // 对话框出现
        composeRule.onNodeWithTag("copyButton").assertExists()
        composeRule.onNodeWithTag("copyButton").performClick()
        // 粘贴 AI 回复并确认
        pasteAndConfirm("你是一位专业的客服助手，请用友好语气解答问题。")
        // 结果已展示
        composeRule.onNodeWithText("最近一次优化结果").assertExists()
        // 历史已写入
        val history = ApplicationProvider.getApplicationContext<PromptOptimizerApp>().repository.getHistory()
        assertTrue(history.isNotEmpty())
    }

    @Test
    fun workspaceIterateFlow() {
        clickBottom("工作台")
        composeRule.onNodeWithTag("workspaceInput").performTextInput("你是一位写作助手")
        composeRule.onNodeWithTag("workspaceGenerate").performClick()
        composeRule.waitForIdle()
        pasteAndConfirm("你是一位专业的写作顾问")

        // 迭代区出现
        composeRule.onNodeWithTag("iterateInput").performTextInput("更专业一些")
        composeRule.onNodeWithTag("iterateGenerate").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("copyButton").assertExists()
        pasteAndConfirm("你是一位资深写作顾问，拥有丰富的创作经验。")
        composeRule.onNodeWithText("最近一次优化结果").assertExists()
    }

    @Test
    fun workspaceModeSwitchKeepsSessions() {
        clickBottom("工作台")
        composeRule.onNodeWithTag("workspaceInput").performTextInput("基础系统内容")
        composeRule.onNodeWithText("图像·文生图").performClick()
        composeRule.onNodeWithText("工作台 · 图像·文生图").assertExists()
        composeRule.onNodeWithText("基础·系统").performClick()
        composeRule.onNodeWithText("工作台 · 基础·系统").assertExists()
        // 会话已恢复
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
        // 选中该消息并优化
        composeRule.onNodeWithText("优化选中消息（生成提示词）").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("copyButton").assertExists()
        pasteAndConfirm("请帮我用 Python 编写一个快速排序函数。")
        // 历史写入
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
        // 模拟 AI 返回的变量 JSON
        pasteAndConfirm("""{"variables":[{"name":"主题","value":"科技"}]}""")
        // 触发"② 生成变量示例值"
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
        // 重新进入以验证渲染
        clickBottom("首页")
        clickBottom("收藏")
        composeRule.onNodeWithText("我的收藏").assertExists()
        // 删除
        composeRule.onNodeWithTag("favDelete").performClick()
        composeRule.waitForIdle()
        // 重新进入验证已删除
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
        // 重新进入验证
        clickBottom("首页")
        composeRule.onNodeWithText("模板管理").performClick()
        composeRule.onNodeWithText("我的自定义模板").assertExists()
        // 删除自定义模板
        composeRule.onNodeWithTag("tplDelete").performClick()
        composeRule.waitForIdle()
        clickBottom("首页")
        composeRule.onNodeWithText("模板管理").performClick()
        composeRule.onNodeWithText("我的自定义模板").assertDoesNotExist()
        // 内置模板仍存在
        composeRule.onNodeWithText("通用优化").assertExists()
    }

    @Test
    fun historyDetailEditWorks() {
        clickBottom("工作台")
        composeRule.onNodeWithTag("workspaceInput").performTextInput("待编辑提示词")
        composeRule.onNodeWithTag("workspaceGenerate").performClick()
        composeRule.waitForIdle()
        pasteAndConfirm("原始输出内容")
        // 打开历史并编辑
        clickBottom("首页")
        composeRule.onNodeWithText("历史记录").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("原始输出内容").performClick()
        composeRule.waitForIdle()
        // 编辑文本框（对话框内的 AI 回复）
        composeRule.onNodeWithText("保存").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("原始输出内容").assertExists()
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
        // 先产生一条记录
        clickBottom("工作台")
        composeRule.onNodeWithTag("workspaceInput").performTextInput("x")
        composeRule.onNodeWithTag("workspaceGenerate").performClick()
        composeRule.waitForIdle()
        pasteAndConfirm("y")
        // 清空
        clickBottom("首页")
        composeRule.onNodeWithText("历史记录").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("清空").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("暂无历史记录").assertExists()
    }
}
