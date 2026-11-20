package com.deepseek.dailytasks.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.deepseek.dailytasks.data.Consumable
import com.deepseek.dailytasks.data.NameGuard
import java.time.LocalDate
import java.util.Locale

/**
 * 「＋ 记一笔购买」：填名称、价格、数量就能记账。
 *
 * 名称和已有消耗品相同时（忽略大小写与空格）自动归档到那条记录上，
 * 价格追加进价格历史、购买次数 +1、累计花费自动累加；名称是新的才新建一条。
 */
@Composable
fun ConsumablePurchaseDialog(
    items: List<Consumable>,
    types: List<String>,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        type: String,
        price: Double,
        amount: Double,
        unit: String,
        note: String,
        date: LocalDate,
    ) -> Unit,
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("个") }
    var note by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now()) }

    val target = items.firstOrNull { NameGuard.normalize(it.name) == NameGuard.normalize(name) }
    val price = priceText.toDoubleOrNull() ?: 0.0
    val amount = amountText.toDoubleOrNull() ?: 0.0
    val valid = name.isNotBlank() && price > 0.0
    val canSave = valid

    fun pickDate() {
        DatePickerDialog(
            context,
            { _, year, month, day -> date = LocalDate.of(year, month + 1, day) },
            date.year,
            date.monthValue - 1,
            date.dayOfMonth,
        ).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("记一笔购买") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 470.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (type.isBlank()) {
                            type = items.firstOrNull { item ->
                                NameGuard.normalize(item.name) == NameGuard.normalize(it)
                            }?.type.orEmpty()
                        }
                    },
                    label = { Text("买了什么（如 洗面奶）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                // 已有同名：提示会归档到哪条，并显示累计情况
                if (target != null) {
                    Surface(
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                            Text(
                                text = "已归档到「${target.name}」",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            Text(
                                text = "已买 ${target.purchaseCount} 次 · 累计 ${target.totalSpentText}" +
                                    " · 上次 ${target.priceText}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            Text(
                                text = "这次的价格会追加到价格记录，累计花费与次数一起增加。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                } else if (name.isNotBlank()) {
                    Text(
                        text = "新名称：会新建一条消耗品记录。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // 最近常买的几条，点一下就填上名字
                if (items.isNotEmpty() && name.isBlank()) {
                    Text(
                        text = "点一下填名字",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items.take(3).forEach { item ->
                            AssistChip(
                                onClick = {
                                    name = item.name
                                    type = item.type
                                    unit = item.unit
                                },
                                label = { Text(item.name, maxLines = 1) },
                            )
                        }
                    }
                }

                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { input ->
                            priceText = input.filter { it.isDigit() || it == '.' }.take(8)
                        },
                        label = { Text("这次的价格") },
                        prefix = { Text("¥") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { input ->
                            amountText = input.filter { it.isDigit() || it == '.' }.take(8)
                        },
                        label = { Text("数量") },
                        suffix = { Text(unit) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                }

                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it.take(4) },
                        label = { Text("单位") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(
                        value = date.format(Consumable.FMT),
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("购买日期") },
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = { pickDate() }),
                    )
                }

                if (target == null && types.isNotEmpty()) {
                    Text(
                        text = "类型（可留空，之后也能在编辑里改）",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        types.take(3).forEach { suggestion ->
                            FilterChip(
                                selected = type.equals(suggestion, ignoreCase = true),
                                onClick = { type = suggestion },
                                label = { Text(suggestion) },
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("备注（可选）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(
                    text = "数量会累加到这件消耗品的用量上；如果它已经标记为用完，会自动重新开始使用（从这次购买日期算）。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!canSave) {
                    Text(
                        text = "名称和价格都要填写（价格大于 0）",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = {
                    onSave(
                        name.trim(),
                        type.trim(),
                        price,
                        amount,
                        unit.ifBlank { "个" },
                        note.trim(),
                        date,
                    )
                },
            ) { Text("记账") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}
