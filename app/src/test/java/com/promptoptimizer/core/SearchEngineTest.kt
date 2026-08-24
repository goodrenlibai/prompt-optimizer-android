package com.promptoptimizer.core

import com.promptoptimizer.model.FavoriteItem
import com.promptoptimizer.model.PromptRecord
import com.promptoptimizer.model.Template
import com.promptoptimizer.model.TemplateType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchEngineTest {

    private val sampleTemplates = listOf(
        Template.fromSimple("t1", "通用优化", "通用系统提示词优化模板", TemplateType.optimize),
        Template.fromSimple("t2", "结构化分析优化", "深入分析并输出结构化提示词", TemplateType.optimize),
        Template.fromSimple("t3", "用户提示词-精准描述", "转换泛泛而谈的提示词为精准描述", TemplateType.userOptimize),
        Template.fromSimple("t4", "文生图-通用优化", "文生图关键视觉锚点优化", TemplateType.text2imageOptimize),
        Template.fromSimple("t5", "对话消息优化", "多轮对话单条消息上下文优化", TemplateType.conversationMessageOptimize)
    )

    private val sampleRecords = listOf(
        PromptRecord(id = "r1", operation = "optimize", modeLabel = "基础-系统", templateName = "通用优化", input = "客服助手", output = "专业客服角色定义"),
        PromptRecord(id = "r2", operation = "iterate", modeLabel = "基础-用户", templateName = "迭代", input = "增加约束", output = "增加禁止交互约束"),
        PromptRecord(id = "r3", operation = "test", modeLabel = "测试", templateName = "测试提示词", input = "用户提问测试", output = "测试执行输出成功")
    )

    private val sampleFavorites = listOf(
        FavoriteItem(id = "f1", name = "客服专业版", category = "工作", content = "你是一个专业客服", tags = listOf("客服", "客服支持")),
        FavoriteItem(id = "f2", name = "文案润色大师", category = "创作", content = "重写并润色以下文案", tags = listOf("写作", "营销")),
        FavoriteItem(id = "f3", name = "Python代码专家", category = "技术", content = "编写高质量Kotlin与Python代码", tags = listOf("编程", "代码"))
    )

    @Test
    fun searchTemplatesByExactChinese() {
        val res = SearchEngine.searchTemplates("通用优化", sampleTemplates)
        assertTrue(res.isNotEmpty())
        assertEquals("t1", res[0].id)
    }

    @Test
    fun searchTemplatesByPinyinInitials() {
        // "xt" 匹配 "系统"
        val res = SearchEngine.searchTemplates("xt", sampleTemplates)
        assertTrue(res.isNotEmpty())
        assertTrue(res.any { it.id == "t1" || it.id == "t2" })
    }

    @Test
    fun searchTemplatesByKeywordTokenAnd() {
        val res = SearchEngine.searchTemplates("用户 精准", sampleTemplates)
        assertEquals(1, res.size)
        assertEquals("t3", res[0].id)
    }

    @Test
    fun searchTemplatesWithFilter() {
        val res = SearchEngine.searchTemplates("优化", sampleTemplates, typeFilter = TemplateType.text2imageOptimize)
        assertEquals(1, res.size)
        assertEquals("t4", res[0].id)
    }

    @Test
    fun searchHistoryByInputAndOutput() {
        val res1 = SearchEngine.searchHistory("客服助手", sampleRecords)
        assertEquals(1, res1.size)
        assertEquals("r1", res1[0].id)

        val res2 = SearchEngine.searchHistory("禁止交互", sampleRecords)
        assertEquals(1, res2.size)
        assertEquals("r2", res2[0].id)
    }

    @Test
    fun searchHistoryWithOpFilter() {
        val res = SearchEngine.searchHistory("", sampleRecords, operationFilter = "test")
        assertEquals(1, res.size)
        assertEquals("r3", res[0].id)
    }

    @Test
    fun searchFavoritesByCategoryAndName() {
        val res1 = SearchEngine.searchFavorites("代码", sampleFavorites)
        assertEquals(1, res1.size)
        assertEquals("f3", res1[0].id)

        val res2 = SearchEngine.searchFavorites("", sampleFavorites, categoryFilter = "创作")
        assertEquals(1, res2.size)
        assertEquals("f2", res2[0].id)
    }

    @Test
    fun emptyQueryReturnsFullList() {
        assertEquals(sampleTemplates.size, SearchEngine.searchTemplates("", sampleTemplates).size)
        assertEquals(sampleRecords.size, SearchEngine.searchHistory("   ", sampleRecords).size)
        assertEquals(sampleFavorites.size, SearchEngine.searchFavorites("", sampleFavorites).size)
    }
}
