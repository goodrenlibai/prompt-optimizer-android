package com.promptoptimizer.data

import android.content.Context
import android.util.Log
import com.promptoptimizer.model.AppData
import com.promptoptimizer.model.Category
import com.promptoptimizer.model.FavoriteItem
import com.promptoptimizer.model.Json
import com.promptoptimizer.model.PromptRecord
import com.promptoptimizer.model.SessionState
import com.promptoptimizer.model.Template
import com.promptoptimizer.template.TemplateCatalog
import java.io.File
import java.util.UUID

/**
 * 数据仓库：负责内置模板、用户模板、历史记录、收藏、分类与会话的加载与持久化。
 *
 * 与原项目一致，所有数据仅存本地（Android 内部存储的 JSON 文件），不上传任何服务器。
 * 纯本地架构，天然满足"删除所有与模型提供商相关的内容"的要求。
 */
class Repository(private val context: Context) {

    companion object {
        private const val TAG = "Repository"
        private const val DATA_FILE = "prompt_optimizer_data.json"
    }

    private val dataFile: File get() = File(context.filesDir, DATA_FILE)

    var data: AppData = AppData()
        private set

    /** 应用启动时初始化：加载持久化数据，若为空则注入内置模板与默认会话。 */
    fun init() {
        data = load() ?: AppData()
        if (data.templates.none { it.isBuiltin }) {
            data.templates.addAll(TemplateCatalog.builtins())
            // 让内置模板优先
            data.templates.sortBy { it.type.ordinal }
        }
        ensureDefaultSessions()
        persist()
    }

    private fun load(): AppData? {
        return try {
            if (!dataFile.exists()) return null
            val text = dataFile.readText()
            if (text.isBlank()) return null
            Json.json.decodeFromString<AppData>(text)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load data, starting fresh", e)
            null
        }
    }

    fun persist() {
        try {
            dataFile.parentFile?.mkdirs()
            dataFile.writeText(Json.json.encodeToString(AppData.serializer(), data))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist data", e)
        }
    }

    private fun ensureDefaultSessions() {
        val keys = listOf(
            SessionState("basic-system", "基础-系统"),
            SessionState("basic-user", "基础-用户"),
            SessionState("pro-multi", "专业-多轮对话"),
            SessionState("pro-variable", "专业-变量"),
            SessionState("image-text2image", "图像-文生图"),
            SessionState("image-image2image", "图像-图生图"),
            SessionState("image-multiimage", "图像-多图")
        )
        for (s in keys) {
            if (data.sessions.none { it.id == s.id }) data.sessions.add(s)
        }
    }

    // ===== 模板 =====

    fun getTemplates(type: com.promptoptimizer.model.TemplateType? = null): List<Template> {
        val all = data.templates.sortedBy { if (it.isBuiltin) 0 else 1 }
        return if (type == null) all else all.filter { it.type == type }
    }

    fun getTemplate(id: String): Template? = data.templates.firstOrNull { it.id == id }

    fun saveUserTemplate(template: Template) {
        val index = data.templates.indexOfFirst { it.id == template.id }
        val copy = template.copy(isBuiltin = false, lastModified = System.currentTimeMillis())
        if (index >= 0) data.templates[index] = copy else data.templates.add(copy)
        persist()
    }

    fun deleteTemplate(id: String): Boolean {
        val t = getTemplate(id) ?: return false
        if (t.isBuiltin) return false
        val removed = data.templates.removeAll { it.id == id }
        persist()
        return removed
    }

    // ===== 历史 =====

    fun getHistory(): List<PromptRecord> = data.history.sortedByDescending { it.timestamp }

    fun getChain(chainId: String): List<PromptRecord> =
        data.history.filter { it.chainId == chainId }.sortedBy { it.timestamp }

    fun addRecord(
        operation: String,
        input: String,
        sentPrompt: String,
        output: String,
        modeLabel: String,
        templateId: String = "",
        templateName: String = "",
        chainId: String = ""
    ): PromptRecord {
        val record = PromptRecord(
            id = UUID.randomUUID().toString(),
            chainId = if (chainId.isBlank()) UUID.randomUUID().toString() else chainId,
            operation = operation,
            input = input,
            sentPrompt = sentPrompt,
            output = output,
            modeLabel = modeLabel,
            templateId = templateId,
            templateName = templateName
        )
        data.history.add(record)
        persist()
        return record
    }

    fun deleteRecord(id: String) {
        data.history.removeAll { it.id == id }
        persist()
    }

    fun clearHistory() {
        data.history.clear()
        persist()
    }

    // ===== 收藏 =====

    fun getFavorites(): List<FavoriteItem> = data.favorites.sortedByDescending { it.timestamp }

    fun getCategories(): List<Category> = data.categories

    fun saveFavorite(item: FavoriteItem): FavoriteItem {
        val saved = item.copy(
            id = if (item.id.isBlank()) UUID.randomUUID().toString() else item.id,
            timestamp = System.currentTimeMillis()
        )
        val index = data.favorites.indexOfFirst { it.id == saved.id }
        if (index >= 0) data.favorites[index] = saved else data.favorites.add(saved)
        persist()
        return saved
    }

    fun deleteFavorite(id: String) {
        data.favorites.removeAll { it.id == id }
        persist()
    }

    fun addCategory(name: String): Category {
        val cat = Category(id = UUID.randomUUID().toString(), name = name)
        data.categories.add(cat)
        persist()
        return cat
    }

    // ===== 会话 =====

    fun getSession(id: String): SessionState =
        data.sessions.firstOrNull { it.id == id } ?: SessionState(id = id, label = id)

    fun saveSession(session: SessionState) {
        val index = data.sessions.indexOfFirst { it.id == session.id }
        if (index >= 0) data.sessions[index] = session else data.sessions.add(session)
        persist()
    }

    // ===== 全局模式选择 =====

    var selectedMode: String
        get() = data.selectedMode
        set(value) { data.selectedMode = value; persist() }

    var selectedSubMode: String
        get() = data.selectedSubMode
        set(value) { data.selectedSubMode = value; persist() }
}
