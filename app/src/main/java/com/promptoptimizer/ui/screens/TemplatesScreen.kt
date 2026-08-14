package com.promptoptimizer.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.promptoptimizer.model.Template
import com.promptoptimizer.model.TemplateType
import com.promptoptimizer.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatesScreen(viewModel: MainViewModel) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { Text("模板管理", style = MaterialTheme.typography.headlineSmall) }
        item { Text("内置模板为只读；自定义模板支持编辑与删除。点击可复制模板全文。",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline) }

        item {
            NewTemplateCard(viewModel)
        }

        val groups = viewModel.repo.getTemplates().groupBy { it.type }
        groups.keys.sortedBy { it.ordinal }.forEach { type ->
            item {
                Text(type.zhName, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }
            items(groups[type] ?: emptyList()) { t ->
                TemplateRow(
                    template = t,
                    onCopy = {
                        val text = t.content ?: t.messages.joinToString("\n\n") { it.content }
                        clipboard.setText(AnnotatedString(text))
                        Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
                    },
                    onDelete = { viewModel.repo.deleteTemplate(t.id) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewTemplateCard(viewModel: MainViewModel) {
    var expanded by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(TemplateType.optimize) }
    val context = LocalContext.current

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("新增自定义模板", style = MaterialTheme.typography.titleSmall)
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(
                    value = type.zhName, onValueChange = {},
                    readOnly = true,
                    label = { Text("模板类型") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    TemplateType.entries.forEach { t ->
                        DropdownMenuItem(text = { Text(t.zhName) }, onClick = { type = t; expanded = false })
                    }
                }
            }
            OutlinedTextField(value = name, onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(), label = { Text("名称") })
            OutlinedTextField(value = content, onValueChange = { content = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                label = { Text("模板内容") },
                placeholder = { Text("简单模板：系统指令 + 换行 + 用户原始提示词\n数组模板请使用 {{originalPrompt}} 等 Mustache 语法") })
            Button(onClick = {
                if (name.isBlank() || content.isBlank()) {
                    Toast.makeText(context, "名称与内容不能为空", Toast.LENGTH_SHORT).show(); return@Button
                }
                viewModel.repo.saveUserTemplate(
                    Template(id = "custom-${System.currentTimeMillis()}", name = name, type = type,
                        content = content, isBuiltin = false)
                )
                name = ""; content = ""
                Toast.makeText(context, "已保存自定义模板", Toast.LENGTH_SHORT).show()
            }) { Text("保存模板") }
        }
    }
}

@Composable
private fun TemplateRow(template: Template, onCopy: () -> Unit, onDelete: () -> Unit) {
    Card(onClick = onCopy, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(template.name, style = MaterialTheme.typography.titleSmall)
                if (template.isBuiltin) {
                    Text("内置", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                } else {
                    IconButton(onClick = onDelete) {
                        androidx.compose.material3.Icon(Icons.Filled.Delete, contentDescription = "删除")
                    }
                }
            }
            if (template.description.isNotBlank())
                Text(template.description, style = MaterialTheme.typography.bodySmall)
            Text(if (template.isSimple) "简单模板" else "数组模板", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary)
        }
    }
}
