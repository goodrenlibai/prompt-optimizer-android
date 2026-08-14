package com.promptoptimizer.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.promptoptimizer.model.Role
import com.promptoptimizer.ui.components.CopyablePromptDialog
import com.promptoptimizer.ui.viewmodel.MainViewModel

@Composable
fun ProScreen(viewModel: MainViewModel) {
    var tab by remember { mutableStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        ScrollableTabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("多轮对话") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("变量模式") })
        }
        if (tab == 0) ConversationTab(viewModel) else VariableTab(viewModel)
    }
}

@Composable
private fun ConversationTab(viewModel: MainViewModel) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("多轮对话消息优化", style = MaterialTheme.typography.headlineSmall)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { viewModel.addConversationMessage(Role.user) }) {
                Icon(Icons.Filled.Add, null, Modifier.padding(end = 4.dp)); Text("添加用户消息")
            }
            OutlinedButton(onClick = { viewModel.addConversationMessage(Role.assistant) }) {
                Icon(Icons.Filled.Add, null, Modifier.padding(end = 4.dp)); Text("添加助手消息")
            }
            OutlinedButton(onClick = { viewModel.addConversationMessage(Role.system) }) {
                Icon(Icons.Filled.Add, null, Modifier.padding(end = 4.dp)); Text("添加系统消息")
            }
        }

        // 消息列表（点击选中）
        viewModel.conversationMessages.forEach { msg ->
            Card(
                onClick = { viewModel.selectedMessageId = msg.id },
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.CardDefaults.cardColors(
                    containerColor = if (msg.id == viewModel.selectedMessageId)
                        MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(Modifier.padding(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(msg.role.name, style = MaterialTheme.typography.labelMedium)
                        if (msg.id == viewModel.selectedMessageId)
                            Text("已选中", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                    OutlinedTextField(
                        value = msg.content,
                        onValueChange = { viewModel.updateConversationMessage(msg.id, it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("消息内容…") }
                    )
                }
            }
        }

        Button(
            onClick = { viewModel.generateConversationPrompt() },
            modifier = Modifier.fillMaxWidth(),
            enabled = viewModel.selectedMessageId.isNotBlank()
        ) {
            Text("优化选中消息（生成提示词）")
        }

        val sent = viewModel.conversationSentPrompt
        if (sent != null) {
            CopyablePromptDialog(
                title = "对话消息优化",
                sentPrompt = sent,
                onDismiss = { viewModel.conversationSentPrompt = null },
                onConfirm = { viewModel.conversationResult = it; viewModel.recordConversationResult() }
            )
        }
    }
}

@Composable
private fun VariableTab(viewModel: MainViewModel) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("变量模式", style = MaterialTheme.typography.headlineSmall)

        OutlinedTextField(
            value = viewModel.variablePrompt,
            onValueChange = { viewModel.variablePrompt = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            label = { Text("提示词（含可参数化内容）") }
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { viewModel.generateVariableExtractionPrompt() },
                enabled = viewModel.variablePrompt.isNotBlank()) {
                Text("① 提取变量")
            }
        }

        val extractSent = viewModel.variableSentPrompt
        if (extractSent != null) {
            CopyablePromptDialog(
                title = "变量提取",
                sentPrompt = extractSent,
                onDismiss = { viewModel.variableSentPrompt = null },
                onConfirm = { pasted ->
                    viewModel.variableResult = pasted
                    viewModel.applyExtractionNamesFromResult()
                },
                extraActions = {
                    Button(onClick = { viewModel.applyExtractionNamesFromResult() }) { Text("解析变量名") }
                }
            )
        }

        if (viewModel.variableList.isNotEmpty()) {
            Text("识别到的变量：${viewModel.variableList.joinToString("、")}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary)

            Button(onClick = { viewModel.generateVariableValuePrompt() }) {
                Text("② 生成变量示例值")
            }
        }

        val valueSent = viewModel.variableSentPrompt
        if (valueSent != null && viewModel.variableResult.isNotBlank()) {
            CopyablePromptDialog(
                title = "变量值生成",
                sentPrompt = valueSent,
                onDismiss = { viewModel.variableSentPrompt = null },
                onConfirm = { pasted ->
                    viewModel.variableResult = pasted
                    viewModel.recordVariableResult()
                }
            )
        }
    }
}
