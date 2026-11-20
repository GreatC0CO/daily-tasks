package com.deepseek.dailytasks.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.deepseek.dailytasks.data.TypeCatalog

/**
 * 消耗品类型选择器：列出所有可用类型（条数多了就在弹窗里滚动），
 * 也可以在最上面直接新建一个自定义类型，建完立刻出现在列表里并选中。
 */
@Composable
fun TypePickerDialog(
    types: List<String>,
    selected: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
    onCreateType: (String) -> Boolean,
) {
    var newType by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    val canCreate = newType.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择类型") },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newType,
                        onValueChange = {
                            newType = it.take(12)
                            errorText = null
                        },
                        label = { Text("新建类型") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(4.dp))
                    IconButton(
                        onClick = {
                            val ok = onCreateType(newType)
                            if (ok) {
                                onSelect(TypeCatalog.normalize(newType))
                            } else {
                                errorText = "已经有这个类型了"
                            }
                        },
                        enabled = canCreate,
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "新建类型")
                    }
                }
                if (errorText != null) {
                    Text(
                        text = errorText!!,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Spacer(Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    TypeOption(
                        title = "不分类",
                        subtitle = "留空，饼图里算作「未分类」",
                        selected = selected.isBlank(),
                        onClick = { onSelect("") },
                    )
                    if (types.isEmpty()) {
                        Text(
                            text = "还没有类型，先在上面新建一个吧（比如 洗护 / 清洁 / 食品）",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 12.dp),
                        )
                    }
                    types.forEach { type ->
                        TypeOption(
                            title = type,
                            subtitle = null,
                            selected = selected.equals(type, ignoreCase = true),
                            onClick = { onSelect(type) },
                        )
                    }
                }

                if (types.size > 6) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "一共 ${types.size} 个类型，列表可以上下滚动。",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        },
    )
}

@Composable
private fun TypeOption(
    title: String,
    subtitle: String?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
