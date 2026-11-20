package com.deepseek.dailytasks.ui

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.deepseek.dailytasks.data.Category
import com.deepseek.dailytasks.data.NameGuard
import com.deepseek.dailytasks.data.Task
import java.time.LocalTime

fun repeatText(everyDays: Int): String = when {
    everyDays <= 0 -> "一次性"
    everyDays == 1 -> "每天"
    else -> "每${everyDays}天"
}

/** 必要级标签色：必要=红，重要=橙，普通=灰。 */
fun priorityColor(priority: Int): Color = when (priority) {
    Task.PRIORITY_NECESSARY -> Color(0xFFD64545)
    Task.PRIORITY_IMPORTANT -> Color(0xFFF0A020)
    else -> Color(0xFF9AA4B2)
}

@Composable
fun TaskEditorDialog(
    initial: Task?,
    categories: List<Category>,
    defaultCategoryId: String,
    existingTitles: List<String>,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        minutes: Int,
        note: String,
        remind: Boolean,
        repeatEveryDays: Int,
        categoryId: String,
        priority: Int,
    ) -> Unit,
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var minutes by remember {
        mutableStateOf(initial?.startMinutes ?: ((LocalTime.now().hour + 1).coerceAtMost(23) * 60))
    }
    var remind by remember { mutableStateOf(initial?.remind ?: true) }
    var repeatEveryDays by remember { mutableStateOf(initial?.repeatEveryDays ?: 1) }
    var categoryId by remember { mutableStateOf(initial?.categoryId ?: defaultCategoryId) }
    var priority by remember { mutableStateOf(initial?.priority ?: Task.PRIORITY_NORMAL) }
    var showRepeatPicker by remember { mutableStateOf(false) }
    var showCategoryPicker by remember { mutableStateOf(false) }
    var showPriorityPicker by remember { mutableStateOf(false) }

    val categoryName = categories.firstOrNull { it.id == categoryId }?.name
        ?: Category.UNCATEGORIZED_NAME

    val duplicate = NameGuard.isDuplicate(title, existingTitles)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "新增任务" else "编辑任务") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("做什么") },
                    singleLine = true,
                    isError = duplicate,
                    supportingText = if (duplicate) {
                        { Text("已经有同名任务了，换一个名字") }
                    } else {
                        null
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("备注（可选）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                FieldRow(
                    label = "开始时间",
                    value = Task.formatMinutes(minutes),
                    monospace = true,
                    onClick = {
                        TimePickerDialog(
                            context,
                            { _, hour, minute -> minutes = hour * 60 + minute },
                            minutes / 60,
                            minutes % 60,
                            true,
                        ).show()
                    },
                )

                FieldRow(
                    label = "必要级",
                    value = Task.priorityLabelOf(priority),
                    onClick = { showPriorityPicker = true },
                )

                FieldRow(
                    label = "分类",
                    value = categoryName,
                    onClick = { showCategoryPicker = true },
                )

                FieldRow(
                    label = "重复",
                    value = repeatText(repeatEveryDays),
                    onClick = { showRepeatPicker = true },
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("到点提醒", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.weight(1f))
                    Switch(checked = remind, onCheckedChange = { remind = it })
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        title.trim(),
                        minutes,
                        note.trim(),
                        remind,
                        repeatEveryDays,
                        categoryId,
                        priority,
                    )
                },
                enabled = title.isNotBlank() && !duplicate,
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )

    if (showRepeatPicker) {
        RepeatPickerDialog(
            initialEvery = repeatEveryDays,
            onDismiss = { showRepeatPicker = false },
            onConfirm = {
                repeatEveryDays = it
                showRepeatPicker = false
            },
        )
    }

    if (showPriorityPicker) {
        PriorityPickerDialog(
            selected = priority,
            onDismiss = { showPriorityPicker = false },
            onSelect = {
                priority = it
                showPriorityPicker = false
            },
        )
    }

    if (showCategoryPicker) {
        CategoryPickerDialog(
            categories = categories,
            selectedId = categoryId,
            onDismiss = { showCategoryPicker = false },
            onSelect = {
                categoryId = it
                showCategoryPicker = false
            },
        )
    }
}

/** 必要级选择：必要 / 重要 / 普通。 */
@Composable
private fun PriorityPickerDialog(
    selected: Int,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("必要级") },
        text = {
            Column {
                PriorityOption(
                    level = Task.PRIORITY_NECESSARY,
                    desc = "今天必须做完的事",
                    selected = selected == Task.PRIORITY_NECESSARY,
                    onSelect = onSelect,
                )
                PriorityOption(
                    level = Task.PRIORITY_IMPORTANT,
                    desc = "最好今天做完，可以往后挪",
                    selected = selected == Task.PRIORITY_IMPORTANT,
                    onSelect = onSelect,
                )
                PriorityOption(
                    level = Task.PRIORITY_NORMAL,
                    desc = "有空再做，不做也没关系",
                    selected = selected == Task.PRIORITY_NORMAL,
                    onSelect = onSelect,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

@Composable
private fun PriorityOption(
    level: Int,
    desc: String,
    selected: Boolean,
    onSelect: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(level) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = { onSelect(level) })
        Box(
            Modifier
                .size(8.dp)
                .background(priorityColor(level), CircleShape)
        )
        Spacer(Modifier.width(8.dp))
        Column {
            Text(Task.priorityLabelOf(level), style = MaterialTheme.typography.bodyLarge)
            Text(
                text = desc,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FieldRow(
    label: String,
    value: String,
    monospace: Boolean = false,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.weight(1f))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/** 选择重复方式：一次性 / 每天 / 每 N 天。 */
@Composable
private fun RepeatPickerDialog(
    initialEvery: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    // 0 = 一次性, 1 = 每天, 2 = 每 N 天
    var mode by remember {
        mutableStateOf(
            when {
                initialEvery <= 0 -> 0
                initialEvery == 1 -> 1
                else -> 2
            }
        )
    }
    var daysText by remember { mutableStateOf(if (initialEvery > 1) initialEvery.toString() else "3") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("重复") },
        text = {
            Column {
                RepeatOption(
                    text = "一次性（做完就消失）",
                    selected = mode == 0,
                    onClick = { mode = 0 },
                )
                RepeatOption(
                    text = "每天",
                    selected = mode == 1,
                    onClick = { mode = 1 },
                )
                RepeatOption(
                    text = "每几天",
                    selected = mode == 2,
                    onClick = { mode = 2 },
                )
                if (mode == 2) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 40.dp, top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = daysText,
                            onValueChange = { input ->
                                daysText = input.filter { it.isDigit() }.take(2)
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(96.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("天一次", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = {
            val days = daysText.toIntOrNull()?.coerceIn(2, 60) ?: 3
            TextButton(
                onClick = {
                    onConfirm(
                        when (mode) {
                            0 -> 0
                            1 -> 1
                            else -> days
                        }
                    )
                },
            ) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

@Composable
private fun RepeatOption(text: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

/** 选择分类。 */
@Composable
private fun CategoryPickerDialog(
    categories: List<Category>,
    selectedId: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择分类") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                CategoryOption(
                    name = Category.UNCATEGORIZED_NAME,
                    color = 0xFF9AA4B2L,
                    selected = selectedId.isEmpty(),
                    onClick = { onSelect("") },
                )
                categories.forEach { category ->
                    CategoryOption(
                        name = category.name,
                        color = category.colorArgb,
                        selected = selectedId == category.id,
                        onClick = { onSelect(category.id) },
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
private fun CategoryOption(name: String, color: Long, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Box(
            Modifier
                .size(10.dp)
                .background(Color(color), CircleShape)
        )
        Spacer(Modifier.width(8.dp))
        Text(name, style = MaterialTheme.typography.bodyLarge)
    }
}

/** 分类管理：新增、改名、删除。 */
@Composable
fun CategoryManagerDialog(
    categories: List<Category>,
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit,
    onRename: (Category, String) -> Unit,
    onDelete: (Category) -> Unit,
) {
    var newName by remember { mutableStateOf("") }
    var editingId by remember { mutableStateOf<String?>(null) }
    var editingName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("管理分类") },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("新分类名称") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            onAdd(newName)
                            newName = ""
                        },
                        enabled = newName.isNotBlank(),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "添加分类")
                    }
                }

                Spacer(Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    categories.forEach { category ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier
                                    .size(10.dp)
                                    .background(Color(category.colorArgb), CircleShape)
                            )
                            Spacer(Modifier.width(10.dp))
                            if (editingId == category.id) {
                                OutlinedTextField(
                                    value = editingName,
                                    onValueChange = { editingName = it },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                )
                                TextButton(
                                    onClick = {
                                        onRename(category, editingName)
                                        editingId = null
                                    },
                                ) { Text("保存") }
                            } else {
                                Text(
                                    text = category.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            editingId = category.id
                                            editingName = category.name
                                        },
                                )
                                IconButton(onClick = { onDelete(category) }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "删除分类",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                    if (categories.isEmpty()) {
                        Text(
                            text = "还没有分类，先在上面加一个吧",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 12.dp),
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    text = "点分类名可以改名；删除分类后，里面的任务会变成「未分类」。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("完成") }
        },
    )
}
