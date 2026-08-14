package com.promptoptimizer.template

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MustacheRendererTest {

    private val renderer = MustacheRenderer.default()

    @Test
    fun simpleInterpolation() {
        val out = renderer.render("你好 {{name}}!", mapOf("name" to "世界"))
        assertEquals("你好 世界!", out)
    }

    @Test
    fun missingVariableRendersEmpty() {
        val out = renderer.render("[{{nope}}]", emptyMap())
        assertEquals("[]", out)
    }

    @Test
    fun tripleBracesAreNotEscaped() {
        val out = renderer.render("{{{html}}}", mapOf("html" to "<b>x</b>"))
        assertEquals("<b>x</b>", out)
    }

    @Test
    fun doubleBracesAreHtmlEscaped() {
        val out = renderer.render("{{html}}", mapOf("html" to "<b>x</b>"))
        assertEquals("&lt;b&gt;x&lt;/b&gt;", out)
    }

    @Test
    fun ampUnescapes() {
        val out = renderer.render("{{&html}}", mapOf("html" to "<b>x</b>"))
        assertEquals("<b>x</b>", out)
    }

    @Test
    fun booleanSectionRendersWhenTrue() {
        val t = "{{#flag}}YES{{/flag}}"
        assertEquals("YES", renderer.render(t, mapOf("flag" to true)))
        assertEquals("", renderer.render(t, mapOf("flag" to false)))
        assertEquals("", renderer.render(t, mapOf("flag" to null)))
    }

    @Test
    fun invertedSectionRendersWhenMissing() {
        val t = "{{^flag}}NO{{/flag}}"
        assertEquals("", renderer.render(t, mapOf("flag" to true)))
        assertEquals("NO", renderer.render(t, mapOf("flag" to false)))
        assertEquals("NO", renderer.render(t, emptyMap()))
    }

    @Test
    fun listIteration() {
        val t = "{{#items}}<{{name}}>{{/items}}"
        val out = renderer.render(t, mapOf("items" to listOf(mapOf("name" to "a"), mapOf("name" to "b"))))
        assertEquals("<a><b>", out)
    }

    @Test
    fun nestedSections() {
        val t = "{{#a}}{{#b}}x{{/b}}{{/a}}"
        val ctx = mapOf("a" to mapOf("b" to true))
        assertEquals("x", renderer.render(t, ctx))
    }

    @Test
    fun dottedPathLookup() {
        val out = renderer.render("{{user.name}}", mapOf("user" to mapOf("name" to "Tom")))
        assertEquals("Tom", out)
    }

    @Test
    fun helpersToJsonEncodes() {
        val ctx = mapOf("originalPrompt" to "写一首 {{风格}} 的诗")
        val t = "{\"originalPrompt\": {{#helpers.toJson}}{{{originalPrompt}}}{{/helpers.toJson}}}"
        val out = renderer.render(t, ctx)
        // 变量占位符必须被逐字保留，且被 JSON 编码
        assertEquals("{\"originalPrompt\": \"写一首 {{风格}} 的诗\"}", out)
    }

    @Test
    fun helpersToJsonEscapesQuotesAndNewlines() {
        val ctx = mapOf("x" to "含\"引号\"\n换行")
        val t = "{{#helpers.toJson}}{{{x}}}{{/helpers.toJson}}"
        val out = renderer.render(t, ctx)
        assertTrue(out.contains("\\\""))
        assertTrue(out.contains("\\n"))
        assertFalse(out.contains("\n"))
    }

    @Test
    fun jsonEncodeStringRoundTrip() {
        val s = "a\"b\\c\nd"
        val enc = MustacheRenderer.jsonEncodeString(s)
        assertTrue(enc.startsWith("\""))
        assertTrue(enc.endsWith("\""))
    }

    @Test
    fun rendersWholeTemplatePreservingLiteralVariableLikeText() {
        // 三重花括号插值中的 {{变量}} 内容不会二次求值
        val t = "保持:{{{originalPrompt}}}"
        val out = renderer.render(t, mapOf("originalPrompt" to "请用 {{主题}} 写"))
        assertEquals("保持:请用 {{主题}} 写", out)
    }

    @Test
    fun sectionOverCollectionOfScalars() {
        val t = "{{#arr}}[{{.}}]{{/arr}}"
        val out = renderer.render(t, mapOf("arr" to listOf(1, 2, 3)))
        assertEquals("[1][2][3]", out)
    }
}
