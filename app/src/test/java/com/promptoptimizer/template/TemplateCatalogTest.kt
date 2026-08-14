package com.promptoptimizer.template

import com.promptoptimizer.model.TemplateType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TemplateCatalogTest {

    private val renderer = MustacheRenderer.default()

    @Test
    fun builtinsNonEmpty() {
        val builtins = TemplateCatalog.builtins()
        assertTrue(builtins.isNotEmpty())
        assertTrue(builtins.size >= 19)
    }

    @Test
    fun idsAreUnique() {
        val ids = TemplateCatalog.builtins().map { it.id }
        assertEquals(ids.size, ids.distinct().size)
    }

    @Test
    fun everyTemplateHasContent() {
        TemplateCatalog.builtins().forEach { t ->
            assertTrue("template ${t.id} must have content or messages", t.isSimple || t.messages.isNotEmpty())
        }
    }

    @Test
    fun everyTemplateCoversExpectedTypes() {
        val types = TemplateCatalog.builtins().map { it.type }.toSet()
        assertTrue(types.contains(TemplateType.optimize))
        assertTrue(types.contains(TemplateType.userOptimize))
        assertTrue(types.contains(TemplateType.iterate))
        assertTrue(types.contains(TemplateType.conversationMessageOptimize))
        assertTrue(types.contains(TemplateType.variableExtraction))
        assertTrue(types.contains(TemplateType.variableValueGeneration))
        assertTrue(types.contains(TemplateType.test))
        assertTrue(types.contains(TemplateType.evaluation))
        assertTrue(types.contains(TemplateType.text2imageOptimize))
        assertTrue(types.contains(TemplateType.image2imageOptimize))
        assertTrue(types.contains(TemplateType.multiimageOptimize))
        assertTrue(types.contains(TemplateType.imageIterate))
    }

    @Test
    fun everyTemplateRendersWithoutCrash() {
        // 为每种模板提供一份通用的上下文，确保任意模板都能渲染出非空文本
        val genericContext = mapOf(
            "originalPrompt" to "写一首关于春天的诗",
            "lastOptimizedPrompt" to "你是一位写作助手",
            "iterateInput" to "更专业一些",
            "promptContent" to "写一首关于春天的诗",
            "variablesText" to "- 主题",
            "variableCount" to 1,
            "hasContextVariables" to false,
            "contextVariablesText" to "",
            "contextVariableCount" to 0,
            "conversationContext" to "user: 你好",
            "toolsContext" to "",
            "systemPrompt" to "你是一位助手",
            "userInput" to "你好",
            "workspacePrompt" to "你是一位助手",
            "testResult" to "结果",
            "testCaseInput" to "你好",
            "baselinePrompt" to "旧",
            "baselineResult" to "旧结果",
            "optimizedPrompt" to "新",
            "optimizedResult" to "新结果",
            "conversationMessages" to listOf(
                mapOf("index" to 1, "roleLabel" to "USER", "content" to "你好", "isSelected" to true)
            ),
            "selectedMessage" to mapOf("index" to 1, "roleLabel" to "USER", "content" to "你好", "contentTooLong" to false)
        )

        TemplateCatalog.builtins().forEach { t ->
            val text = if (t.isSimple) {
                renderer.render(t.content ?: "", genericContext)
            } else {
                t.messages.joinToString("\n") { renderer.render(it.content, genericContext) }
            }
            assertTrue("template ${t.id} must render non-blank", text.isNotBlank())
        }
    }

    @Test
    fun generalOptimizeContainsStructure() {
        val t = TemplateCatalog.generalOptimize
        assertEquals("general-optimize", t.id)
        assertTrue(t.isSimple)
        assertTrue(t.content!!.contains("# Role:"))
        assertTrue(t.content!!.contains("## Skills"))
    }

    @Test
    fun variablePlaceholdersPreservedByToJson() {
        // 核心保证：待优化提示词里的 {{变量}} 在生成的提示词中逐字保留
        val ctx = mapOf("originalPrompt" to "使用 {{颜色}} 描绘 {{地点}}")
        val t = TemplateCatalog.analyticalOptimize
        val rendered = t.messages.joinToString("\n") { renderer.render(it.content, ctx) }
        assertTrue(rendered.contains("{{颜色}}"))
        assertTrue(rendered.contains("{{地点}}"))
    }
}
