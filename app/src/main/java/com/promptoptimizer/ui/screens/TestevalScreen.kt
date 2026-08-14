package com.promptoptimizer.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.promptoptimizer.ui.components.CopyablePromptDialog
import com.promptoptimizer.ui.viewmodel.MainViewModel

@Composable
fun TestevalScreen(viewModel: MainViewModel) {
    var tab by remember { mutableStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        ScrollableTabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("测试") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("结果评估") })
            Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text("对比评估") })
            Tab(selected = tab == 3, onClick = { tab = 3 }, text = { Text("提示词分析") })
        }
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when (tab) {
                0 -> TestTab(viewModel)
                1 -> EvalTab(viewModel, "result")
                2 -> EvalTab(viewModel, "compare")
                else -> EvalTab(viewModel, "promptOnly")
            }
        }
    }
}

@Composable
private fun TestTab(viewModel: MainViewModel) {
    Text("测试提示词（让 AI 按提示词执行）", style = MaterialTheme.typography.headlineSmall)
    OutlinedTextField(
        value = viewModel.testSystemPrompt,
        onValueChange = { viewModel.testSystemPrompt = it },
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .testTag("testSystemPrompt"),
        label = { Text("系统提示词（待测试的优化结果）") }
    )
    OutlinedTextField(
        value = viewModel.testUserInput,
        onValueChange = { viewModel.testUserInput = it },
        modifier = Modifier
            .fillMaxWidth()
            .testTag("testUserInput"),
        label = { Text("用户输入 / 测试用例") }
    )
    Button(onClick = { viewModel.generateTestPrompt() },
        enabled = viewModel.testSystemPrompt.isNotBlank(),
        modifier = Modifier.testTag("testGenerate")) {
        Text("生成测试提示词")
    }

    val sent = viewModel.testSentPrompt
    if (sent != null) {
        CopyablePromptDialog(
            title = "测试",
            sentPrompt = sent,
            onDismiss = { viewModel.testSentPrompt = null },
            onConfirm = { viewModel.testResult = it; viewModel.recordTestResult() }
        )
    }
    if (viewModel.testResult.isNotBlank()) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text("测试结果", style = MaterialTheme.typography.titleSmall)
                Text(viewModel.testResult, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun EvalTab(viewModel: MainViewModel, type: String) {
    val title = when (type) {
        "compare" -> "对比评估"
        "promptOnly" -> "提示词分析"
        else -> "结果评估"
    }
    Text(title + "（生成提示词后发给 AI 评估）", style = MaterialTheme.typography.headlineSmall)

    if (type == "compare") {
        OutlinedTextField(
            value = viewModel.testSystemPrompt,
            onValueChange = { viewModel.testSystemPrompt = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .testTag("testSystemPrompt"),
            label = { Text("原始提示词") }
        )
        OutlinedTextField(
            value = viewModel.workspaceResult,
            onValueChange = { viewModel.workspaceResult = it },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("workspaceResult"),
            label = { Text("优化后提示词") }
        )
    } else if (type == "result") {
        OutlinedTextField(
            value = viewModel.testSystemPrompt,
            onValueChange = { viewModel.testSystemPrompt = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .testTag("testSystemPrompt"),
            label = { Text("被评估的提示词") }
        )
    } else {
        OutlinedTextField(
            value = viewModel.testSystemPrompt,
            onValueChange = { viewModel.testSystemPrompt = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .testTag("testSystemPrompt"),
            label = { Text("被分析的提示词") }
        )
    }

    OutlinedTextField(
        value = viewModel.testUserInput,
        onValueChange = { viewModel.testUserInput = it },
        modifier = Modifier
            .fillMaxWidth()
            .testTag("testUserInput"),
        label = { Text("测试用例输入（可留空）") }
    )

    if (type != "promptOnly") {
        OutlinedTextField(
            value = viewModel.testResult,
            onValueChange = { viewModel.testResult = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .testTag("testResult"),
            label = { Text("测试输出结果") }
        )
    }

    Button(onClick = { viewModel.generateEvalPrompt(type) },
        modifier = Modifier.testTag("evalGenerate")) {
        Text("生成" + title + "提示词")
    }

    val sent = viewModel.evalSentPrompt
    if (sent != null) {
        CopyablePromptDialog(
            title = title,
            sentPrompt = sent,
            onDismiss = { viewModel.evalSentPrompt = null },
            onConfirm = { viewModel.evalResult = it; viewModel.recordEvalResult() }
        )
    }
}
