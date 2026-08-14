package com.promptoptimizer.ui.components

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp

/**
 * 人工发送模式的核心对话框：
 *  1. 展示「待复制的提示词」文本（用户点击"复制"后，发到任意在线免费 AI）。
 *  2. 提供输入框，让用户把 AI 的回复粘贴回来。
 *  3. 点击"确认"把粘贴的回复作为结果保存。
 *
 * @param title         对话框标题
 * @param sentPrompt    生成的、待复制的提示词文本
 * @param extraActions  可选附加按钮（如"复制后再发送一份变量提取"）
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CopyablePromptDialog(
    title: String,
    sentPrompt: String,
    onDismiss: () -> Unit,
    onConfirm: (pastedOutput: String) -> Unit,
    extraActions: (@Composable () -> Unit)? = null
) {
    var pastedOutput by remember { mutableStateOf("") }
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("① 复制下面这段提示词，发送给任意在线免费 AI（如 DeepSeek、Kimi、豆包、ChatGPT、Claude 等）：",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary)
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = sentPrompt,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(12.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        clipboard.setText(AnnotatedString(sentPrompt))
                        Toast.makeText(context, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
                    }, modifier = Modifier.testTag("copyButton")) {
                        Text("复制提示词")
                    }
                    if (extraActions != null) extraActions()
                }
                Text("② 把 AI 返回的结果粘贴到这里：",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary)
                OutlinedTextField(
                    value = pastedOutput,
                    onValueChange = { pastedOutput = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 160.dp)
                        .testTag("pastedOutput"),
                    placeholder = { Text("AI 的回复结果…") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(pastedOutput) },
                enabled = pastedOutput.isNotBlank(),
                modifier = Modifier.testTag("confirmButton")
            ) {
                Text("确认保存结果")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
