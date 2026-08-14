package com.promptoptimizer

import android.app.Application
import com.promptoptimizer.data.Repository

/**
 * Application 入口：持有全局数据仓库（单例）。
 */
class PromptOptimizerApp : Application() {
    val repository: Repository by lazy {
        Repository(this).also { it.init() }
    }
}
