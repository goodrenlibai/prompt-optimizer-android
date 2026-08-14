package com.promptoptimizer.template

/**
 * 一个精简但足够忠实的 Mustache 渲染器。
 *
 * 支持原项目模板引擎用到的全部特性：
 * - `{{name}}`         双花括号插值（HTML 转义）
 * - `{{{name}}}` / `{{&name}}`  三重花括号（不转义）
 * - `{{#name}}...{{/name}}`  区块（布尔 / 对象 / 列表迭代）
 * - `{{^name}}...{{/name}}`  反向区块（值为空时渲染）
 * - 点号路径，例如 `{{helpers.toJson}}`
 * - 内置 lambda 辅助：`{{#helpers.toJson}}...{{/helpers.toJson}}`
 *   （渲染内部内容后做 JSON 编码，用于把「待优化提示词」作为 JSON 证据正文注入，
 *    这样提示词里的 `{{变量}}` 占位符会被逐字保留，而不是被二次求值。）
 *
 * 上下文是一个 Map<String, Any?>。值可为 String / Number / Boolean / List / Map。
 */
class MustacheRenderer(private val helpers: Map<String, (String) -> String> = emptyMap()) {

    private var context: MutableList<Map<String, Any?>> = mutableListOf()

    /** 渲染模板。@param root 顶层上下文 @return 渲染结果文本 */
    fun render(template: String, root: Map<String, Any?>): String {
        context = mutableListOf(root)
        return renderInternal(template)
    }

    /** 使用当前上下文栈渲染（不重置上下文，供嵌套区块调用）。 */
    private fun renderInternal(template: String): String {
        val sb = StringBuilder()
        parseInto(template, 0, sb)
        return sb.toString()
    }

    // ===== 解析 ===

    private fun parseInto(template: String, startIdx: Int, out: StringBuilder): Int {
        var i = startIdx
        val n = template.length
        while (i < n) {
            val open = template.indexOf("{{", i)
            if (open == -1) {
                out.append(template, i, n)
                return n
            }
            out.append(template, i, open)

            var raw = false
            var sigilIdx = open + 2
            var endSigil = "}}"
            if (sigilIdx < n && template[sigilIdx] == '{') {
                raw = true
                sigilIdx++
                endSigil = "}}}"
            }

            val close = template.indexOf(endSigil, sigilIdx)
            if (close == -1) {
                out.append(template.substring(open))
                return n
            }

            var inner = template.substring(sigilIdx, close).trim()
            if (!raw && inner.startsWith("&")) {
                raw = true
                inner = inner.substring(1).trim()
            }

            if (inner.isEmpty()) {
                i = close + endSigil.length
                continue
            }

            val tag = inner[0]
            val name = inner.substring(1).trim()

            when (tag) {
                '#' -> {
                    val sectionBody = StringBuilder()
                    i = parseSection(template, close + endSigil.length, name, sectionBody)
                    out.append(sectionBody)
                }
                '^' -> {
                    val sectionBody = StringBuilder()
                    i = parseInvertedSection(template, close + endSigil.length, name, sectionBody)
                    out.append(sectionBody)
                }
                '/' -> {
                    out.append("{{$name}}")
                    i = close + endSigil.length
                }
                else -> {
                    val value = lookup(inner)
                    val rendered = when (value) {
                        is String -> value
                        is Number -> value.toString()
                        is Boolean -> value.toString()
                        else -> value?.toString() ?: ""
                    }
                    out.append(if (raw) rendered else htmlEscape(rendered))
                    i = close + endSigil.length
                }
            }
        }
        return n
    }

    /** 解析区块主体，直到找到匹配的 {{/name}}，返回其后的索引，并把渲染结果写入 bodyOut。 */
    private fun parseSection(
        template: String,
        startIdx: Int,
        name: String,
        bodyOut: StringBuilder
    ): Int {
        if (helpers.containsKey(name)) {
            val body = StringBuilder()
            val after = collectBody(template, startIdx, name, body)
            val rendered = renderInternal(body.toString())
            bodyOut.append(helpers[name]!!.invoke(rendered))
            return after
        }

        val body = StringBuilder()
        val after = collectBody(template, startIdx, name, body)
        val value = lookup(name)

        when {
            value is List<*> -> {
                for (item in value) {
                    if (item == null) continue
                    if (item is Map<*, *>) {
                        context.add(item as Map<String, Any?>)
                        bodyOut.append(renderInternal(body.toString()))
                        context.removeAt(context.size - 1)
                    } else {
                        context.add(mapOf("." to item))
                        bodyOut.append(renderInternal(body.toString()))
                        context.removeAt(context.size - 1)
                    }
                }
            }
            isTruthy(value) -> {
                if (value is Map<*, *>) {
                    context.add(value as Map<String, Any?>)
                    bodyOut.append(renderInternal(body.toString()))
                    context.removeAt(context.size - 1)
                } else {
                    bodyOut.append(renderInternal(body.toString()))
                }
            }
        }
        return after
    }

    private fun parseInvertedSection(
        template: String,
        startIdx: Int,
        name: String,
        bodyOut: StringBuilder
    ): Int {
        val body = StringBuilder()
        val after = collectBody(template, startIdx, name, body)
        val value = lookup(name)
        if (!isTruthy(value)) {
            bodyOut.append(renderInternal(body.toString()))
        }
        return after
    }

    private fun collectBody(template: String, startIdx: Int, name: String, bodyOut: StringBuilder): Int {
        var i = startIdx
        var depth = 0
        val n = template.length
        while (i < n) {
            val open = template.indexOf("{{", i)
            if (open == -1) {
                bodyOut.append(template, i, n)
                return n
            }
            bodyOut.append(template, i, open)
            var sigil = open + 2
            var endSigil = "}}"
            if (sigil < n && template[sigil] == '{') { sigil++; endSigil = "}}}" }
            val close = template.indexOf(endSigil, sigil)
            if (close == -1) {
                bodyOut.append(template.substring(open))
                return n
            }
            var inner = template.substring(sigil, close).trim()
            if (inner.startsWith("{") || inner.startsWith("&")) {
                bodyOut.append(template.substring(open, close + endSigil.length))
                i = close + endSigil.length
                continue
            }
            val tag = inner[0]
            val tagName = inner.substring(1).trim()
            when (tag) {
                '#', '^' -> {
                    if (tagName == name) depth++
                    bodyOut.append(template.substring(open, close + endSigil.length))
                    i = close + endSigil.length
                }
                '/' -> {
                    if (tagName == name) {
                        if (depth == 0) return close + endSigil.length
                        depth--
                        bodyOut.append(template.substring(open, close + endSigil.length))
                        i = close + endSigil.length
                    } else {
                        bodyOut.append(template.substring(open, close + endSigil.length))
                        i = close + endSigil.length
                    }
                }
                else -> {
                    bodyOut.append(template.substring(open, close + endSigil.length))
                    i = close + endSigil.length
                }
            }
        }
        return n
    }

    // ===== 上下文查找 ===

    private fun currentContext(): Map<String, Any?> = context.last()

    private fun lookup(name: String): Any? {
        val parts = name.split(".")
        for (idx in context.indices.reversed()) {
            val ctx = context[idx]
            if (parts.size == 1 && ctx.containsKey(name)) return ctx[name]
            var cur: Any? = ctx
            var found = true
            for (p in parts) {
                if (cur is Map<*, *>) {
                    val v = (cur as Map<*, *>)[p]
                    if (v != null) { cur = v } else { found = false; break }
                } else {
                    found = false
                    break
                }
            }
            if (found) return cur
        }
        return null
    }

    private fun isTruthy(value: Any?): Boolean = when (value) {
        null -> false
        is Boolean -> value
        is String -> value.isNotEmpty()
        is List<*> -> value.isNotEmpty()
        else -> true
    }

    private fun htmlEscape(s: String): String = buildString {
        for (ch in s) {
            when (ch) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&#39;")
                else -> append(ch)
            }
        }
    }

    companion object {
        /** 生成一个 JSON 字符串字面量（含首尾引号），并转义引号/反斜杠/控制字符/换行。 */
        fun jsonEncodeString(value: String): String {
            val sb = StringBuilder("\"")
            for (ch in value) {
                when (ch) {
                    '"' -> sb.append("\\\"")
                    '\\' -> sb.append("\\\\")
                    '\n' -> sb.append("\\n")
                    '\r' -> sb.append("\\r")
                    '\t' -> sb.append("\\t")
                    '\b' -> sb.append("\\b")
                    '\u000C' -> sb.append("\\f")
                    else -> {
                        if (ch.code < 0x20) {
                            sb.append("\\u").append(ch.code.toString(16).padStart(4, '0'))
                        } else {
                            sb.append(ch)
                        }
                    }
                }
            }
            sb.append("\"")
            return sb.toString()
        }

        val defaultHelpers: Map<String, (String) -> String> = mapOf(
            "helpers.toJson" to { inner: String -> jsonEncodeString(inner) }
        )

        fun default(): MustacheRenderer = MustacheRenderer(defaultHelpers)
    }
}
