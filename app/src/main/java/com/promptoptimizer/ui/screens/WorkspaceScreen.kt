package com.promptoptimizer.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.promptoptimizer.ui.components.CopyablePromptDialog
import com.promptoptimizer.ui.viewmodel.MainViewModel

/**
 * 工作台：基础（系统/用户）与图像模式的提示词优化 + 迭代。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceScreen(viewModel: MainViewModel) {
    val modes = listOf(
        "system" to "基础·系统",
        "user" to "基础·用户",
        "text2image" to "图像·文生图",
        "image2image" to "图像·图生图",
        "multiimage" to "图像·多图"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("工作台 · ${modes.first { it.first == viewModel.workspaceMode }.second}",
            style = MaterialTheme.typography.headlineSmall)

        // 模式切换
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            modes.forEach { (mode, label) ->
                FilterChip(
                    selected = viewModel.workspaceMode == mode,
                    onClick = { viewModel.setMode(mode) },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors()
                )
            }
        }

        // 模板选择
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("优化模板", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    viewModel.workspaceTemplates().forEach { t ->
                        FilterChip(
                            selected = viewModel.workspaceTemplateId == t.id,
                            onClick = { viewModel.workspaceTemplateId = t.id },
                            label = { Text(t.name) }
                        )
                    }
                }
            }
        }

        // 输入
        OutlinedTextField(
            value = viewModel.workspaceInput,
            onValueChange = { viewModel.workspaceInput = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .testTag("workspaceInput"),
            label = { Text("原始提示词") },
            placeholder = { Text("输入你想优化的提示词…") }
        )

        Button(
            onClick = { viewModel.generateWorkspacePrompt() },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("workspaceGenerate"),
            enabled = viewModel.workspaceInput.isNotBlank()
        ) {
            Text("生成提示词（复制后发给 AI）")
        }

        // 生成的提示词对话框
        val sent = viewModel.workspaceSentPrompt
        if (sent != null) {
            CopyablePromptDialog(
                title = "优化结果",
                sentPrompt = sent,
                onDismiss = { viewModel.workspaceSentPrompt = null },
                onConfirm = { pasted ->
                    viewModel.workspaceResult = pasted
                    viewModel.recordWorkspaceResult()
                }
            )
        }

        // 迭代区
        if (viewModel.workspaceResult.isNotBlank()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("最近一次优化结果", style = MaterialTheme.typography.titleSmall)
                    Text(viewModel.workspaceResult, style = MaterialTheme.typography.bodySmall, maxLines = 6)
                }
            }

            Text("迭代优化", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = viewModel.iterateInput,
                onValueChange = { viewModel.iterateInput = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("iterateInput"),
                label = { Text("迭代需求") },
                placeholder = { Text("例如：更专业一些 / 输出 JSON 格式…") }
            )
            Button(onClick = { viewModel.generateIteratePrompt() },
                enabled = viewModel.iterateInput.isNotBlank(),
                modifier = Modifier.testTag("iterateGenerate")) {
                Text("生成迭代提示词")
            }

            val iterSent = viewModel.iterateSentPrompt
            if (iterSent != null) {
                CopyablePromptDialog(
                    title = "迭代结果",
                    sentPrompt = iterSent,
                    onDismiss = { viewModel.iterateSentPrompt = null },
                    onConfirm = { pasted ->
                        viewModel.iterateResult = pasted
                        viewModel.recordIterateResult()
                    }
                )
            }
        }
    }
}
