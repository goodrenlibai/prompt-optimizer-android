package com.promptoptimizer.core

import com.promptoptimizer.model.Role
import com.promptoptimizer.template.TemplateCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptEngineTest {

    @Test
    fun optimizeSystemSimpleTemplate() {
        val t = TemplateCatalog.generalOptimize
        val out = PromptEngine.optimizeSentPrompt(t, "你是一个客服")
        assertTrue(out.contains("你是一个客服"))       // 原始提示词被拼入
        assertTrue(out.contains("AI提示词优化专家"))    // 模板内容存在
    }

    @Test
    fun optimizeAnalyticalPreservesVariables() {
        val t = TemplateCatalog.analyticalOptimize
        val out = PromptEngine.optimizeSentPrompt(t, "帮我写{{类型}}文章")
        assertTrue(out.contains("{{类型}}"))
    }

    @Test
    fun optimizeUserPromptBasic() {
        val t = TemplateCatalog.userPromptBasic
        val out = PromptEngine.optimizeSentPrompt(t, "写好看的文章")
        assertTrue(out.contains("写好看的文章"))
        assertTrue(out.contains("基础优化"))
    }

    @Test
    fun iterateBuildsBothEvidenceFields() {
        val t = TemplateCatalog.iterateTemplate
        val out = PromptEngine.iterateSentPrompt(t, "你是写作助手", "更专业")
        assertTrue(out.contains("更专业"))
        assertTrue(out.contains("你是写作助手"))
    }

    @Test
    fun contextIterateIncludesConversationWhenPresent() {
        val t = TemplateCatalog.contextIterate
        val out = PromptEngine.iterateSentPrompt(t, "你是一位助手", "加规则", conversationContext = "user: 帮我写代码")
        assertTrue(out.contains("帮我写代码"))
    }

    @Test
    fun messageOptimizeRendersSelectedMessage() {
        val t = TemplateCatalog.messageOptimize
        val msgs = listOf(
            PromptEngine.ConversationMessage("m1", Role.user, "帮我写代码"),
            PromptEngine.ConversationMessage("m2", Role.assistant, "好的")
        )
        val out = PromptEngine.messageOptimizeSentPrompt(t, msgs, "m1")
        assertTrue(out.contains("帮我写代码"))
        assertTrue(out.contains("好的"))
    }

    @Test
    fun variableExtractionRendersPrompt() {
        val t = TemplateCatalog.variableExtraction
        val out = PromptEngine.variableExtractionSentPrompt(t, "写一篇关于{{主题}}的文章")
        assertTrue(out.contains("{{主题}}"))
    }

    @Test
    fun variableValueGenerationRendersVariables() {
        val t = TemplateCatalog.variableValueGeneration
        val out = PromptEngine.variableValueGenerationSentPrompt(t, "写一篇关于主题的文章", listOf("主题", "字数"))
        assertTrue(out.contains("主题"))
        assertTrue(out.contains("字数"))
    }

    @Test
    fun testTemplateRendersSystemAndInput() {
        val t = TemplateCatalog.testPrompt
        val out = PromptEngine.testSentPrompt(t, "你是客服", "我要退款")
        assertTrue(out.contains("你是客服"))
        assertTrue(out.contains("我要退款"))
    }

    @Test
    fun evalResultRendersEvidence() {
        val t = TemplateCatalog.evalResult
        val out = PromptEngine.evalResultSentPrompt(t, "你是一位助手", "这是输出", "测试输入")
        assertTrue(out.contains("这是输出"))
        assertTrue(out.contains("测试输入"))
    }

    @Test
    fun evalCompareRendersBothSides() {
        val t = TemplateCatalog.evalCompare
        val out = PromptEngine.evalCompareSentPrompt(t, "旧提示词", "旧结果", "新提示词", "新结果", "输入")
        assertTrue(out.contains("旧提示词"))
        assertTrue(out.contains("新提示词"))
        assertTrue(out.contains("新结果"))
    }

    @Test
    fun evalPromptOnlyRendersWorkspace() {
        val t = TemplateCatalog.evalPromptOnly
        val out = PromptEngine.evalPromptOnlySentPrompt(t, "你是一位专业助手")
        assertTrue(out.contains("你是一位专业助手"))
        assertTrue(out.contains("目标清晰度"))
    }

    @Test
    fun imageOptimizeRenders() {
        val t = TemplateCatalog.imageGeneralOptimize
        val out = PromptEngine.optimizeSentPrompt(t, "一只在星空下漂浮的图书馆")
        assertTrue(out.contains("星空下漂浮的图书馆"))
    }

    @Test
    fun imageIterateRenders() {
        val t = TemplateCatalog.imageIterate
        val out = PromptEngine.iterateSentPrompt(t, "一只猫", "增加赛博朋克风格")
        assertTrue(out.contains("赛博朋克风格"))
    }

    // ===== 结构化 AI 回复提取与解析测试 =====

    @Test
    fun extractJsonFromMarkdownCodeBlock() {
        val raw = """
            这是 AI 返回的引导语：
            ```json
            {
              "variables": [
                { "name": "season", "value": "春天" }
              ]
            }
            ```
            以上是变量提取结果。
        """.trimIndent()

        val json = PromptEngine.extractJsonString(raw)
        assertNotNull(json)
        assertTrue(json!!.contains("\"season\""))
    }

    @Test
    fun parseExtractedVariablesFromJson() {
        val raw = """
            ```json
            {
              "variables": [
                { "name": "style", "value": "科技风", "reason": "主题风格" },
                { "name": "length", "value": "500字", "reason": "长度约束" }
              ],
              "summary": "提取了2个变量"
            }
            ```
        """.trimIndent()

        val vars = PromptEngine.parseExtractedVariables(raw)
        assertEquals(2, vars.size)
        assertEquals("style", vars[0].name)
        assertEquals("科技风", vars[0].value)
        assertEquals("length", vars[1].name)
    }

    @Test
    fun parseVariableValuesFromJson() {
        val raw = """
            ```json
            {
              "values": [
                { "name": "风格", "value": "浪漫主义" },
                { "name": "字数", "value": "800" }
              ]
            }
            ```
        """.trimIndent()

        val values = PromptEngine.parseVariableValues(raw)
        assertEquals(2, values.size)
        assertEquals("浪漫主义", values["风格"])
        assertEquals("800", values["字数"])
    }

    @Test
    fun parseEvaluationReport() {
        val raw = """
            ```json
            {
              "score": {
                "overall": 92.5,
                "dimensions": [
                  { "key": "goalAchievement", "label": "目标达成度", "score": 95.0 },
                  { "key": "outputQuality", "label": "输出质量", "score": 90.0 }
                ]
              },
              "improvements": ["建议进一步补充约束"],
              "summary": "表现优异"
            }
            ```
        """.trimIndent()

        val eval = PromptEngine.parseEvaluationReport(raw)
        assertNotNull(eval)
        assertEquals(92.5, eval!!.overallScore, 0.01)
        assertEquals(2, eval.dimensions.size)
        assertEquals("目标达成度", eval.dimensions[0].label)
        assertEquals("表现优异", eval.summary)
    }
}
