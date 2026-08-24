package com.promptoptimizer.template

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MustacheRendererTest {

    private lateinit var renderer: MustacheRenderer

    @Before
    fun setup() {
        MustacheRenderer.clearCache()
        renderer = MustacheRenderer.default()
    }

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
    fun commentTagsAreIgnored() {
        val out = renderer.render("Hello {{! this is a comment }}World", emptyMap())
        assertEquals("Hello World", out)
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
        val out = renderer.render("{{user.profile.name}}", mapOf("user" to mapOf("profile" to mapOf("name" to "Tom"))))
        assertEquals("Tom", out)
    }

    @Test
    fun sectionOverCollectionOfScalars() {
        val t = "{{#arr}}[{{.}}]{{/arr}}"
        val out = renderer.render(t, mapOf("arr" to listOf(1, 2, 3)))
        assertEquals("[1][2][3]", out)
    }

    @Test
    fun helpersToJsonEncodes() {
        val ctx = mapOf("originalPrompt" to "写一首 {{风格}} 的诗")
        val t = "{\"originalPrompt\": {{#helpers.toJson}}{{{originalPrompt}}}{{/helpers.toJson}}}"
        val out = renderer.render(t, ctx)
        assertEquals("{\"originalPrompt\": \"写一首 {{风格}} 的诗\"}", out)
    }

    @Test
    fun helpersToJsonEscapesQuotesAndNewlines() {
        val ctx = mapOf("x" to "含\"引号\"\n换行\t制表符")
        val t = "{{#helpers.toJson}}{{{x}}}{{/helpers.toJson}}"
        val out = renderer.render(t, ctx)
        assertTrue(out.contains("\\\""))
        assertTrue(out.contains("\\n"))
        assertTrue(out.contains("\\t"))
        assertFalse(out.contains("\n"))
    }

    @Test
    fun helpersUpperAndLower() {
        val t1 = "{{#helpers.upper}}hello{{/helpers.upper}}"
        assertEquals("HELLO", renderer.render(t1, emptyMap()))

        val t2 = "{{#helpers.lower}}WORLD{{/helpers.lower}}"
        assertEquals("world", renderer.render(t2, emptyMap()))
    }

    @Test
    fun unclosedTagsGracefullyDegrade() {
        val t = "这是 {{未闭合标签"
        val out = renderer.render(t, emptyMap())
        assertEquals("这是 {{未闭合标签", out)
    }

    @Test
    fun cachedAstRendersIdentically() {
        val template = "用户: {{name}}, 年龄: {{age}}"
        val r1 = renderer.render(template, mapOf("name" to "Alice", "age" to 20))
        val r2 = renderer.render(template, mapOf("name" to "Bob", "age" to 25))
        assertEquals("用户: Alice, 年龄: 20", r1)
        assertEquals("用户: Bob, 年龄: 25", r2)
    }
}
