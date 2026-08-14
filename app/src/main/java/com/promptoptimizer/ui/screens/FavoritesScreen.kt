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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.promptoptimizer.model.FavoriteItem
import com.promptoptimizer.ui.viewmodel.MainViewModel

@Composable
fun FavoritesScreen(viewModel: MainViewModel) {
    var name by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("收藏", style = MaterialTheme.typography.headlineSmall)
        }

        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("保存新的提示词资产", style = MaterialTheme.typography.titleSmall)
                    OutlinedTextField(value = name, onValueChange = { name = it },
                        modifier = Modifier.fillMaxWidth(), label = { Text("名称") })
                    OutlinedTextField(value = content, onValueChange = { content = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        label = { Text("内容（提示词）") },
                        placeholder = { Text("可先在工作台优化后复制到这里") })
                    Button(onClick = {
                        if (content.isBlank()) {
                            Toast.makeText(context, "内容不能为空", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.repo.saveFavorite(FavoriteItem(name = name.ifBlank { "未命名" }, content = content))
                            name = ""; content = ""
                            Toast.makeText(context, "已收藏", Toast.LENGTH_SHORT).show()
                        }
                    }) { Text("保存收藏") }
                }
            }
        }

        val favorites = viewModel.repo.getFavorites()
        if (favorites.isEmpty()) {
            item { Text("暂无收藏", color = MaterialTheme.colorScheme.outline) }
        } else {
            items(favorites) { fav ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(fav.name, style = MaterialTheme.typography.titleSmall)
                            IconButton(onClick = { viewModel.repo.deleteFavorite(fav.id) }) {
                                androidx.compose.material3.Icon(Icons.Filled.Delete, contentDescription = "删除")
                            }
                        }
                        Text(fav.content, style = MaterialTheme.typography.bodySmall, maxLines = 4)
                        if (fav.category.isNotBlank())
                            Text("分类：${fav.category}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}
