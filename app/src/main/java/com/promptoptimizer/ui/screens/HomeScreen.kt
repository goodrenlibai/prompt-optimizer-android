package com.promptoptimizer.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.promptoptimizer.ui.viewmodel.MainViewModel

@Composable
fun HomeScreen(viewModel: MainViewModel, onOpenTemplates: () -> Unit, onOpenHistory: () -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("提示词优化器", style = MaterialTheme.typography.headlineMedium)
            Text("人工发送模式 · 无需配置任何 API", style = MaterialTheme.typography.bodyMedium)
        }

        item {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Info, contentDescription = null)
                        Text(" 如何使用（三步）", style = MaterialTheme.typography.titleMedium)
                    }
                    Text("① 输入提示词，点击「生成提示词」。")
                    Text("② 复制生成的提示词，发送给任意在线免费 AI（如 DeepSeek、Kimi、豆包、ChatGPT 等）。")
                    Text("③ 把 AI 的回复粘贴回来，保存为优化/测试/评估结果。")
                }
            }
        }

        item {
            Card(onClick = onOpenTemplates, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CollectionsBookmark, contentDescription = null)
                    Text("  模板管理", style = MaterialTheme.typography.titleMedium)
                }
            }
        }

        item {
            Card(onClick = onOpenHistory, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.History, contentDescription = null)
                    Text("  历史记录", style = MaterialTheme.typography.titleMedium)
                }
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
        item {
            Text("最近历史", style = MaterialTheme.typography.titleMedium)
        }
        val history = viewModel.repo.getHistory()
        if (history.isEmpty()) {
            item { Text("暂无历史记录", color = MaterialTheme.colorScheme.outline) }
        } else {
            items(history.take(20)) { rec ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(rec.modeLabel, style = MaterialTheme.typography.labelLarge)
                            Text(java.text.SimpleDateFormat("MM-dd HH:mm").format(java.util.Date(rec.timestamp)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline)
                        }
                        Text(rec.operationLabel + " · " + rec.templateName, style = MaterialTheme.typography.bodySmall)
                        Text(
                            rec.output.take(120).let { if (rec.output.length > 120) "$it…" else it },
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2
                        )
                    }
                }
            }
        }
    }
}

private val com.promptoptimizer.model.PromptRecord.operationLabel: String
    get() = when (operation) {
        "optimize" -> "优化"
        "iterate" -> "迭代"
        "test" -> "测试"
        "evaluate" -> "评估"
        "messageOptimize" -> "消息优化"
        "variable" -> "变量"
        else -> operation
    }
