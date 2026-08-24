package com.promptoptimizer.core

import com.promptoptimizer.model.FavoriteItem
import com.promptoptimizer.model.PromptRecord
import com.promptoptimizer.model.Template
import com.promptoptimizer.model.TemplateType
import java.util.Locale

/**
 * 高性能本地智能搜索引擎。
 *
 * 特性：
 * 1. **分词与组合匹配**：支持以空格分隔的多个查询词（AND 逻辑）；
 * 2. **模糊与拼音匹配**：
 *    - 中文关键词：高精度连续子串匹配（避免跨句远距离假阳性）；
 *    - 纯字母/拼音缩写：拼音首字母匹配（如 "xt" 命中 "系统优化"）与子序列匹配；
 * 3. **多字段加权相关性排序（Relevance Scoring）**：
 *    - 名称/标题匹配（高权重：10x）
 *    - 标签/分类匹配（中高权重：8x）
 *    - 描述信息匹配（中等权重：4x）
 *    - 正文内容匹配（基础权重：1.5x）
 *    - 完全匹配与前缀匹配享有最高优先级加分；
 * 4. **零依赖与毫秒级执行**。
 */
object SearchEngine {

    data class SearchResult<T>(
        val item: T,
        val score: Double
    )

    // ===== 常用汉字拼音首字母对照表 =====
    private val PINYIN_HEADS = mapOf(
        '系' to "x", '统' to "t", '提' to "t", '示' to "s", '词' to "c",
        '优' to "y", '化' to "h", '用' to "y", '户' to "h", '迭' to "d",
        '代' to "d", '多' to "d", '轮' to "l", '对' to "d", '话' to "h",
        '消' to "x", '息' to "x", '变' to "b", '量' to "l", '提' to "t",
        '取' to "q", '值' to "z", '生' to "s", '成' to "c", '测' to "c",
        '试' to "s", '结' to "j", '果' to "g", '评' to "p", '估' to "g",
        '分' to "f", '析' to "x", '图' to "t", '像' to "x", '文' to "w",
        '模' to "m", '板' to "b", '历' to "l", '史' to "s", '收' to "s",
        '藏' to "c", '基' to "j", '础' to "c", '精' to "j", '准' to "z",
        '规' to "g", '划' to "h", '专' to "z", '业' to "y", '结' to "j",
        '构' to "g", '输' to "s", '出' to "c", '格' to "g", '式' to "s"
    )

    private fun isAllAscii(str: String): Boolean {
        return str.all { it.code < 128 }
    }

    /** 计算单个文本与查询词列表的匹配分值。 */
    fun calculateScore(text: String, queryTokens: List<String>): Double {
        if (text.isBlank() || queryTokens.isEmpty()) return 0.0
        val lowerText = text.lowercase(Locale.getDefault())
        val pinyinInitials = getPinyinInitials(text)

        var totalScore = 0.0

        for (token in queryTokens) {
            val lowerToken = token.lowercase(Locale.getDefault())
            var tokenMatched = false
            var tokenScore = 0.0

            // 1. 完全匹配
            if (lowerText == lowerToken) {
                tokenScore += 10.0
                tokenMatched = true
            }
            // 2. 前缀匹配
            else if (lowerText.startsWith(lowerToken)) {
                tokenScore += 6.0
                tokenMatched = true
            }
            // 3. 连续子串包含
            else if (lowerText.contains(lowerToken)) {
                tokenScore += 4.0
                tokenMatched = true
            }
            // 4. 针对纯字母查询：支持拼音首字母匹配与字母子序列
            else if (isAllAscii(lowerToken)) {
                if (pinyinInitials.contains(lowerToken)) {
                    tokenScore += 3.5
                    tokenMatched = true
                } else if (isSubsequence(lowerToken, lowerText)) {
                    tokenScore += 1.5
                    tokenMatched = true
                }
            }

            if (!tokenMatched) {
                return 0.0
            }

            totalScore += tokenScore
        }

        return totalScore
    }

    private fun isSubsequence(pattern: String, text: String): Boolean {
        if (pattern.length > text.length) return false
        var pIdx = 0
        var tIdx = 0
        while (pIdx < pattern.length && tIdx < text.length) {
            if (pattern[pIdx] == text[tIdx]) {
                pIdx++
            }
            tIdx++
        }
        return pIdx == pattern.length
    }

    private fun getPinyinInitials(chineseText: String): String {
        val sb = StringBuilder(chineseText.length)
        for (ch in chineseText) {
            val initial = PINYIN_HEADS[ch]
            if (initial != null) {
                sb.append(initial)
            } else if (ch in 'a'..'z' || ch in 'A'..'Z' || ch in '0'..'9') {
                sb.append(ch.lowercaseChar())
            }
        }
        return sb.toString()
    }

    private fun tokenize(query: String): List<String> {
        return query.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    }

    // ===== 业务对象搜索 API =====

    /**
     * 搜索模板。
     * 权重：名称(10.0) > 类别(6.0) > 描述(4.0) > 内容(1.5)
     */
    fun searchTemplates(
        query: String,
        templates: List<Template>,
        typeFilter: TemplateType? = null
    ): List<Template> {
        var baseList = templates
        if (typeFilter != null) {
            baseList = baseList.filter { it.type == typeFilter }
        }
        val tokens = tokenize(query)
        if (tokens.isEmpty()) return baseList

        return baseList
            .map { tpl ->
                val nameScore = calculateScore(tpl.name, tokens) * 10.0
                val typeScore = calculateScore(tpl.type.zhName, tokens) * 6.0
                val descScore = calculateScore(tpl.description, tokens) * 4.0
                val contentScore = calculateScore(tpl.content ?: tpl.messages.joinToString("\n") { it.content }, tokens) * 1.5
                val total = nameScore + typeScore + descScore + contentScore
                SearchResult(tpl, total)
            }
            .filter { it.score > 0.0 }
            .sortedByDescending { it.score }
            .map { it.item }
    }

    /**
     * 搜索历史记录。
     * 权重：模式/标签(10.0) > 模板名称(8.0) > 操作(6.0) > 原始输入(4.0) > 输出回复(2.0)
     */
    fun searchHistory(
        query: String,
        records: List<PromptRecord>,
        operationFilter: String? = null
    ): List<PromptRecord> {
        var baseList = records
        if (!operationFilter.isNullOrBlank()) {
            baseList = baseList.filter { it.operation == operationFilter }
        }
        val tokens = tokenize(query)
        if (tokens.isEmpty()) return baseList

        return baseList
            .map { rec ->
                val modeScore = calculateScore(rec.modeLabel, tokens) * 10.0
                val tplScore = calculateScore(rec.templateName, tokens) * 8.0
                val opScore = calculateScore(rec.operation, tokens) * 6.0
                val inputScore = calculateScore(rec.input, tokens) * 4.0
                val outputScore = calculateScore(rec.output, tokens) * 2.0
                val total = modeScore + tplScore + opScore + inputScore + outputScore
                SearchResult(rec, total)
            }
            .filter { it.score > 0.0 }
            .sortedByDescending { it.score }
            .map { it.item }
    }

    /**
     * 搜索收藏资产。
     * 权重：名称(10.0) > 分类(8.0) > 标签(6.0) > 备注(4.0) > 内容(2.0)
     */
    fun searchFavorites(
        query: String,
        favorites: List<FavoriteItem>,
        categoryFilter: String? = null
    ): List<FavoriteItem> {
        var baseList = favorites
        if (!categoryFilter.isNullOrBlank()) {
            baseList = baseList.filter { it.category == categoryFilter }
        }
        val tokens = tokenize(query)
        if (tokens.isEmpty()) return baseList

        return baseList
            .map { fav ->
                val nameScore = calculateScore(fav.name, tokens) * 10.0
                val catScore = calculateScore(fav.category, tokens) * 8.0
                val tagScore = calculateScore(fav.tags.joinToString(" "), tokens) * 6.0
                val noteScore = calculateScore(fav.note, tokens) * 4.0
                val contentScore = calculateScore(fav.content, tokens) * 2.0
                val total = nameScore + catScore + tagScore + noteScore + contentScore
                SearchResult(fav, total)
            }
            .filter { it.score > 0.0 }
            .sortedByDescending { it.score }
            .map { it.item }
    }
}
