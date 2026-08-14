package com.promptoptimizer.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SerializationTest {

    @Test
    fun templateSerializationRoundTrip() {
        val t = Template.fromMessages(
            id = "x", name = "测试", type = TemplateType.iterate,
            messages = listOf(ChatMessage(Role.system, "你是一位{{角色}}"), ChatMessage(Role.user, "你好"))
        )
        val json = Json.json.encodeToString(Template.serializer(), t)
        val back = Json.json.decodeFromString(Template.serializer(), json)
        assertEquals(t, back)
    }

    @Test
    fun appDataRoundTrip() {
        val data = AppData(
            templates = mutableListOf(Template.fromSimple("s", "简单", "指令", TemplateType.optimize)),
            history = mutableListOf(PromptRecord(id = "h1", operation = "optimize", input = "in", sentPrompt = "sp", output = "out", modeLabel = "基础-系统")),
            favorites = mutableListOf(FavoriteItem(id = "f1", name = "收藏", content = "内容")),
            categories = mutableListOf(Category(id = "c1", name = "默认")),
            sessions = mutableListOf(SessionState(id = "basic-system", label = "基础-系统", currentInput = "hello"))
        )
        val json = Json.json.encodeToString(AppData.serializer(), data)
        val back = Json.json.decodeFromString(AppData.serializer(), json)
        assertEquals(1, back.templates.size)
        assertEquals(1, back.history.size)
        assertEquals("基础-系统", back.history[0].modeLabel)
        assertEquals("hello", back.sessions[0].currentInput)
        assertEquals("默认", back.categories[0].name)
    }

    @Test
    fun jsonIgnoresUnknownKeys() {
        // 向后兼容：未知字段不影响解析
        val json = """{"templates":[],"history":[],"favorites":[],"categories":[],"sessions":[],"someFutureField":123}"""
        val back = Json.json.decodeFromString(AppData.serializer(), json)
        assertTrue(back.templates.isEmpty())
    }

    @Test
    fun templateTypeZhNameCoverage() {
        for (t in TemplateType.entries) {
            assertTrue(t.zhName.isNotBlank())
        }
    }
}
