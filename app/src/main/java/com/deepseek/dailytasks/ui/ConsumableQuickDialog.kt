package com.deepseek.dailytasks.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.deepseek.dailytasks.data.Consumable

/** 不用进编辑界面，直接改「还剩多少」：点数字就能精确设置。 */
@Composable
internal fun QuickRemainingDialog(
    item: Consumable,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit,
) {
    val total = item.totalAmount ?: 0.0
    var text by remember { mutableStateOf(weightTextOf(item.remainingAmount ?: total)) }
    val value = text.toDoubleOrNull()?.coerceIn(0.0, total) ?: 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("「${item.name}」还剩多少") },
        text = {
            Column {
                Text(
                    "总量 ${weightTextOf(total)}${item.unit}，现在还剩 ${weightTextOf(value)}${item.unit}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0.0, 0.25, 0.5, 0.75, 1.0).forEach { ratio ->
                        val preset = (total * ratio).coerceIn(0.0, total)
                        FilterChip(
                            selected = value == preset,
                            onClick = { text = weightTextOf(preset) },
                            label = {
                                Text(if (ratio == 1.0) "全部" else "${(ratio * 100).toInt()}%")
                            },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.filter { c -> c.isDigit() || c == '.' }.take(8) },
                    label = { Text("剩余量（${item.unit}）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(-10.0, -1.0, 1.0, 10.0).forEach { delta ->
                        OutlinedButton(
                            onClick = {
                                val base = text.toDoubleOrNull() ?: total
                                text = weightTextOf((base + delta).coerceIn(0.0, total))
                            },
                        ) {
                            Text(if (delta > 0) "+${weightTextOf(delta)}" else "−${weightTextOf(-delta)}")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(value) }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}
