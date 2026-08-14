package com.promptoptimizer.data

import androidx.test.core.app.ApplicationProvider
import com.promptoptimizer.PromptOptimizerApp
import com.promptoptimizer.model.FavoriteItem
import com.promptoptimizer.model.SessionState
import com.promptoptimizer.model.Template
import com.promptoptimizer.model.TemplateType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RepositoryTest {

    private lateinit var repo: Repository

    @Before
    fun setup() {
        val app = ApplicationProvider.getApplicationContext<PromptOptimizerApp>()
        repo = Repository(app)
        repo.resetForTest()
    }

    @After
    fun teardown() {
        repo.resetForTest()
    }

    @Test
    fun initInjectsBuiltinTemplates() {
        assertTrue(repo.getTemplates().isNotEmpty())
        assertTrue(repo.getTemplates().all { it.isBuiltin })
    }

    @Test
    fun getTemplatesByType() {
        val optimize = repo.getTemplates(TemplateType.optimize)
        assertTrue(optimize.any { it.id == "general-optimize" })
        val eval = repo.getTemplates(TemplateType.evaluation)
        assertTrue(eval.isNotEmpty())
    }

    @Test
    fun saveAndGetUserTemplate() {
        val t = Template(id = "custom-1", name = "我的模板", type = TemplateType.optimize,
            content = "你是专家", isBuiltin = false)
        repo.saveUserTemplate(t)
        val got = repo.getTemplate("custom-1")
        assertNotNull(got)
        assertFalse(got!!.isBuiltin)
        assertEquals("你是专家", got.content)
    }

    @Test
    fun cannotDeleteBuiltin() {
        val before = repo.getTemplates().size
        val removed = repo.deleteTemplate("general-optimize")
        assertFalse(removed)
        assertEquals(before, repo.getTemplates().size)
    }

    @Test
    fun canDeleteCustomTemplate() {
        repo.saveUserTemplate(Template(id = "custom-x", name = "x", type = TemplateType.optimize, content = "c", isBuiltin = false))
        assertTrue(repo.deleteTemplate("custom-x"))
        assertTrue(repo.getTemplate("custom-x") == null)
    }

    @Test
    fun addAndQueryHistory() {
        repo.addRecord("optimize", "in", "sp", "out", "基础-系统", "general-optimize", "通用优化")
        val history = repo.getHistory()
        assertEquals(1, history.size)
        assertEquals("optimize", history[0].operation)
        assertEquals("out", history[0].output)
    }

    @Test
    fun historyChainAndDelete() {
        val r1 = repo.addRecord("optimize", "a", "sp1", "o1", "m")
        val r2 = repo.addRecord("iterate", "b", "sp2", "o2", "m", chainId = r1.chainId)
        assertEquals(2, repo.getChain(r1.chainId).size)
        repo.deleteRecord(r1.id)
        assertEquals(1, repo.getHistory().size)
    }

    @Test
    fun clearHistory() {
        repo.addRecord("optimize", "a", "sp", "o", "m")
        repo.clearHistory()
        assertTrue(repo.getHistory().isEmpty())
    }

    @Test
    fun favoritesSaveDelete() {
        repo.saveFavorite(FavoriteItem(name = "f", content = "c"))
        assertEquals(1, repo.getFavorites().size)
        val id = repo.getFavorites()[0].id
        repo.deleteFavorite(id)
        assertTrue(repo.getFavorites().isEmpty())
    }

    @Test
    fun categoriesAdd() {
        repo.addCategory("默认")
        assertEquals(1, repo.getCategories().size)
    }

    @Test
    fun sessionSaveGet() {
        repo.saveSession(SessionState(id = "basic-system", label = "基础-系统", currentInput = "hello"))
        assertEquals("hello", repo.getSession("basic-system").currentInput)
    }

    @Test
    fun modeSelectionPersists() {
        repo.selectedMode = "pro"
        repo.selectedSubMode = "multi"
        assertEquals("pro", repo.selectedMode)
        assertEquals("multi", repo.selectedSubMode)
    }

    @Test
    fun resetForTestClearsData() {
        repo.addRecord("optimize", "a", "sp", "o", "m")
        repo.saveFavorite(FavoriteItem(name = "f", content = "c"))
        repo.resetForTest()
        assertTrue(repo.getHistory().isEmpty())
        assertTrue(repo.getFavorites().isEmpty())
        // 重新注入内置模板
        assertTrue(repo.getTemplates().isNotEmpty())
    }
}
