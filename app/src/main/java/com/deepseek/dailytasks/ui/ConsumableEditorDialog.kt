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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.deepseek.dailytasks.data.Consumable
import com.deepseek.dailytasks.data.NameGuard
import java.time.LocalDate

@Composable
fun ConsumableEditorDialog(
    initial: Consumable?,
    types: List<String>,
    existingNames: List<String>,
    onAddType: (String) -> Boolean,
    onDismiss: () -> Unit,
    onSave: (Consumable) -> Unit,
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var type by remember { mutableStateOf(initial?.type ?: "") }
    var startDate by remember { mutableStateOf(initial?.startDate ?: LocalDate.now()) }
    var endDate by remember { mutableStateOf(initial?.endDate) }
    var quantityText by remember {
        mutableStateOf(initial?.let { Consumable.trimNumber(it.quantity) } ?: "1")
    }
    var unit by remember { mutableStateOf(initial?.unit ?: "个") }
    var priceText by remember {
        mutableStateOf(
            initial?.let { if (it.price > 0) String.format(java.util.Locale.US, "%.2f", it.price) else "" } ?: ""
        )
    }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var remainingText by remember {
        mutableStateOf(initial?.remainingAmount?.let { Consumable.trimNumber(it) } ?: "")
    }
    var metricMode by remember { mutableStateOf(initial?.metricMode ?: Consumable.MODE_DAYS) }
    var showTypePicker by remember { mutableStateOf(false) }

    val duplicate = NameGuard.isDuplicate(name, existingNames)

    fun pickDate(base: LocalDate, onPicked: (LocalDate) -> Unit) {
        DatePickerDialog(
            context,
            { _, year, month, day -> onPicked(LocalDate.of(year, month + 1, day)) },
            base.year,
            base.monthValue - 1,
            base.dayOfMonth,
        ).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "新增消耗品" else "编辑消耗品") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("名称（如 洗面奶）") },
                    singleLine = true,
                    isError = duplicate,
                    supportingText = if (duplicate) {
                        { Text("已经有同名消耗品了，换一个名字") }
                    } else {
                        null
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                ConsumableFieldRow(
                    label = "类型",
                    value = if (type.isBlank()) "不分类" else type,
                    onClick = { showTypePicker = true },
                )
                if (types.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        types.take(4).forEach { suggestion ->
                            FilterChip(
                                selected = type.equals(suggestion, ignoreCase = true),
                                onClick = { type = suggestion },
                                label = { Text(suggestion) },
                            )
                        }
                    }
                }
                Text(
                    text = "点「类型」可以从已建好的类型里选，也可以在弹窗里新建；类型多了会变成可滚动的列表。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                ConsumableFieldRow(
                    label = "开始使用",
                    value = startDate.format(Consumable.FMT),
                    onClick = { pickDate(startDate) { startDate = it } },
                )

                ConsumableFieldRow(
                    label = "用完时间",
                    value = endDate?.format(Consumable.FMT) ?: "还在用",
                    onClick = { pickDate(endDate ?: LocalDate.now()) { endDate = it } },
                    trailing = {
                        if (endDate != null) {
                            TextButton(onClick = { endDate = null }) { Text("还在用") }
                        }
                    },
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { input ->
                            quantityText = input.filter { it.isDigit() || it == '.' }.take(8)
                        },
                        label = { Text("用量（总量）") },
                        suffix = { Text(unit) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it.take(4) },
                        label = { Text("单位") },
                        singleLine = true,
                        modifier = Modifier.width(96.dp),
                    )
                }
                Text(
                    text = "「用量」就是这一件一共有多少（如 500 毫升、1 支），记一笔时数量会自动累加到这里。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                OutlinedTextField(
                    value = remainingText,
                    onValueChange = { input ->
                        remainingText = input.filter { it.isDigit() || it == '.' }.take(8)
                    },
                    label = { Text("现在还剩多少（可选）") },
                    suffix = { Text(unit) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    supportingText = { Text("填了就能算出剩下的百分比；不填只按「已用几天」显示。") },
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(
                    text = "列表里优先显示哪个",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = metricMode == Consumable.MODE_DAYS,
                        onClick = { metricMode = Consumable.MODE_DAYS },
                        label = { Text("已用天数") },
                    )
                    FilterChip(
                        selected = metricMode == Consumable.MODE_AMOUNT,
                        onClick = { metricMode = Consumable.MODE_AMOUNT },
                        label = { Text("剩余量") },
                    )
                }
                Text(
                    text = "填了剩余量之后，列表里点「⇄」就能在「已用几天」和「剩多少」之间互换。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                OutlinedTextField(
                    value = priceText,
                    onValueChange = { input ->
                        priceText = input.filter { it.isDigit() || it == '.' }.take(8)
                    },
                    label = { Text("当前价格") },
                    prefix = { Text("¥") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    supportingText = { Text("再买一次请在列表点「记一笔」，价格会追加成一条购买记录。") },
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("备注（可选）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && !duplicate,
                onClick = {
                    val base = Consumable(
                        id = initial?.id ?: System.currentTimeMillis(),
                        name = name.trim(),
                        type = type.trim(),
                        startDate = startDate,
                        endDate = endDate,
                        quantity = quantityText.toDoubleOrNull() ?: 1.0,
                        unit = unit.ifBlank { "个" },
                        price = priceText.toDoubleOrNull() ?: 0.0,
                        note = note.trim(),
                        totalAmount = quantityText.toDoubleOrNull(),
                        remainingAmount = remainingText.toDoubleOrNull(),
                        metricMode = metricMode,
                        priceHistory = initial?.priceHistory ?: emptyList(),
                    )
                    // 新建或改了价格时，把这次购买价记进价格历史（同价不重复记）
                    onSave(base.withPrice(LocalDate.now(), base.price))
                },
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )

    if (showTypePicker) {
        TypePickerDialog(
            types = types,
            selected = type,
            onDismiss = { showTypePicker = false },
            onSelect = {
                type = it
                showTypePicker = false
            },
            onCreateType = onAddType,
        )
    }
}

@Composable
private fun ConsumableFieldRow(
    label: String,
    value: String,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.weight(1f))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            trailing?.invoke()
        }
    }
}
