package com.promptoptimizer.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.promptoptimizer.model.PromptRecord
import com.promptoptimizer.ui.viewmodel.MainViewModel

@Composable
fun HistoryScreen(viewModel: MainViewModel) {
    var selected by remember { mutableStateOf<PromptRecord?>(null) }
    val history = viewModel.repo.getHistory()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("历史记录", style = MaterialTheme.typography.headlineSmall)
                if (history.isNotEmpty()) {
                    TextButton(onClick = { viewModel.repo.clearHistory() }) { Text("清空") }
                }
            }
        }
        if (history.isEmpty()) {
            item { Text("暂无历史记录", color = MaterialTheme.colorScheme.outline) }
        } else {
            items(history) { rec ->
                Card(onClick = { selected = rec }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(rec.modeLabel + " · " + rec.templateName, style = MaterialTheme.typography.titleSmall)
                            IconButton(onClick = { viewModel.repo.deleteRecord(rec.id) }) {
                                androidx.compose.material3.Icon(Icons.Filled.Delete, contentDescription = "删除")
                            }
                        }
                        Text(java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(java.util.Date(rec.timestamp)),
                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Text(rec.output.take(160).let { if (rec.output.length > 160) "$it…" else it },
                            style = MaterialTheme.typography.bodySmall, maxLines = 3)
                    }
                }
            }
        }
    }

    selected?.let { rec ->
        var edited by remember(rec.id) { mutableStateOf(rec.output) }
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text(rec.modeLabel + " · " + rec.templateName) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("输入：" + rec.input, style = MaterialTheme.typography.bodySmall)
                    Text("生成的提示词：", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                    Text(rec.sentPrompt, style = MaterialTheme.typography.bodySmall, maxLines = 6)
                    OutlinedTextField(value = edited, onValueChange = { edited = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("AI 回复") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    // 更新输出
                    val idx = viewModel.repo.data.history.indexOfFirst { it.id == rec.id }
                    if (idx >= 0) {
                        viewModel.repo.data.history[idx] = rec.copy(output = edited)
                        viewModel.repo.persist()
                    }
                    selected = null
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { selected = null }) { Text("关闭") } }
        )
    }
}
