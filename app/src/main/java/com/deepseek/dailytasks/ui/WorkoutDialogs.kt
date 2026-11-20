package com.deepseek.dailytasks.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepseek.dailytasks.data.EXERCISE_COLORS
import com.deepseek.dailytasks.data.Exercise
import com.deepseek.dailytasks.data.variantMark

/** 点一下换下一个颜色，转一圈回到「不标色」。 */
internal fun nextExerciseColor(current: Int): Int =
    if (current >= EXERCISE_COLORS.size) 0 else current + 1

/** 重量转成输入框里的文字：0 显示空。 */
internal fun weightTextOf(weight: Double): String = when {
    weight <= 0.0 -> ""
    weight == weight.toLong().toDouble() -> weight.toLong().toString()
    else -> weight.toString()
}

/**
 * 拆分动作：把一个动作拆成两段，组数加起来不变，两段可以分别改次数与重量。
 * 计划编辑与今日清单共用。
 */
@Composable
internal fun SplitExerciseDialog(
    name: String,
    sets: Int,
    reps: String,
    weight: Double,
    onDismiss: () -> Unit,
    onConfirm: (firstSets: Int, repsA: String, weightA: Double, repsB: String, weightB: Double) -> Unit,
) {
    val total = sets.coerceAtLeast(2)
    var firstSetsText by remember { mutableStateOf((total / 2).coerceAtLeast(1).toString()) }
    var repsA by remember { mutableStateOf(reps) }
    var weightAText by remember { mutableStateOf(weightTextOf(weight)) }
    var repsB by remember { mutableStateOf(reps) }
    var weightBText by remember { mutableStateOf(weightTextOf(weight)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("拆分「$name」") },
        text = {
            Column {
                Text(
                    "一共 $total 组，拆成两段后组数加起来还是 $total 组；两段可以分别用不同的次数 / 重量。",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = firstSetsText,
                    onValueChange = { firstSetsText = it.filter(Char::isDigit).take(2) },
                    label = { Text("第一段组数（1..${total - 1}）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                val first = firstSetsText.toIntOrNull()?.coerceIn(1, total - 1) ?: (total / 2).coerceIn(1, total - 1)
                Text(
                    "第一段 $first 组 · 第二段 ${total - first} 组",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                )
                Text("第一段", style = MaterialTheme.typography.labelMedium)
                Row(Modifier.padding(top = 4.dp)) {
                    OutlinedTextField(
                        value = repsA,
                        onValueChange = { repsA = it },
                        label = { Text("次数") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(6.dp))
                    OutlinedTextField(
                        value = weightAText,
                        onValueChange = { weightAText = it.filter { c -> c.isDigit() || c == '.' }.take(6) },
                        label = { Text("重量kg") },
                        singleLine = true,
                        modifier = Modifier.width(110.dp),
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text("第二段", style = MaterialTheme.typography.labelMedium)
                Row(Modifier.padding(top = 4.dp)) {
                    OutlinedTextField(
                        value = repsB,
                        onValueChange = { repsB = it },
                        label = { Text("次数") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(6.dp))
                    OutlinedTextField(
                        value = weightBText,
                        onValueChange = { weightBText = it.filter { c -> c.isDigit() || c == '.' }.take(6) },
                        label = { Text("重量kg") },
                        singleLine = true,
                        modifier = Modifier.width(110.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val f = firstSetsText.toIntOrNull()?.coerceIn(1, total - 1) ?: (total / 2).coerceIn(1, total - 1)
                    onConfirm(
                        f,
                        repsA,
                        weightAText.toDoubleOrNull()?.coerceIn(0.0, 1000.0) ?: 0.0,
                        repsB,
                        weightBText.toDoubleOrNull()?.coerceIn(0.0, 1000.0) ?: 0.0,
                    )
                },
            ) { Text("拆分") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

/** 合并同名动作：可以先用一条当基准（次数/重量），组数会全部相加。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MergeSameNameDialog(
    name: String,
    variants: List<Exercise>,
    onDismiss: () -> Unit,
    onConfirm: (reps: String, weight: Double) -> Unit,
) {
    var base by remember { mutableStateOf(0) }
    var reps by remember { mutableStateOf(variants.firstOrNull()?.reps ?: "12") }
    var weightText by remember { mutableStateOf(weightTextOf(variants.firstOrNull()?.weight ?: 0.0)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("合并「$name」") },
        text = {
            Column {
                Text(
                    "有 ${variants.size} 条同名动作，合并后合成 1 条：组数是 ${variants.joinToString(" + ") { it.sets.toString() }} = ${variants.sumOf { it.sets }} 组；次数 / 重量用下面这一组。",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "以哪一条为基准",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    variants.forEachIndexed { index, v ->
                        FilterChip(
                            selected = base == index,
                            onClick = {
                                base = index
                                reps = v.reps
                                weightText = weightTextOf(v.weight)
                            },
                            label = { Text("${v.sets}组×${v.reps}${if (v.weight > 0.0) " · ${weightTextOf(v.weight)}kg" else ""}") },
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row {
                    OutlinedTextField(
                        value = reps,
                        onValueChange = { reps = it },
                        label = { Text("次数 / 时长") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(6.dp))
                    OutlinedTextField(
                        value = weightText,
                        onValueChange = { weightText = it.filter { c -> c.isDigit() || c == '.' }.take(6) },
                        label = { Text("重量kg") },
                        singleLine = true,
                        modifier = Modifier.width(110.dp),
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "颜色按第一条的标记保留。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        reps,
                        weightText.toDoubleOrNull()?.coerceIn(0.0, 1000.0) ?: 0.0,
                    )
                },
            ) { Text("合并") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

/** 今天的清单里给某个动作换颜色（可选）。 */
@Composable
internal fun ColorPickerDialog(
    current: Int,
    onDismiss: () -> Unit,
    onPick: (Int) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("标记颜色") },
        text = {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0).plus((1..EXERCISE_COLORS.size).toList()).forEach { index ->
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(
                                    if (index == 0) MaterialTheme.colorScheme.surfaceVariant else Color(EXERCISE_COLORS[index - 1])
                                )
                                .clickable { onPick(index) },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (index == current) {
                                Text(
                                    "✓",
                                    color = if (index == 0) MaterialTheme.colorScheme.onSurfaceVariant else Color.White,
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "第一个是「不标色」。相同颜色的动作在列表里一眼能认出来。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } },
    )
}

/**
 * 同名动作「N 选一」：一个名字下有多个「组数 / 重量」变体时，用 ①②③ 选一个，
 * 默认第一个；变体的具体设置留在「训练模板」里做。
 */
@Composable
internal fun AltPickerDialog(
    groups: List<Pair<Int, List<Exercise>>>,
    current: Map<Int, Int>,
    onDismiss: () -> Unit,
    onConfirm: (Map<Int, Int>) -> Unit,
) {
    var selected by remember(groups) {
        mutableStateOf(
            groups.associate { (color, list) ->
                color to (current[color] ?: 0).coerceIn(0, list.size - 1)
            },
        )
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("今天做哪一个") },
        text = {
            Column(
                Modifier
                    .heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    "编了同一个序号（① ② ③…）的不同动作是二选一/多选一，今天每组只做一个。默认第一个；想换动作就点另一个。",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                groups.forEach { (group, list) ->
                    Text(
                        "备选组 ${variantMark(group - 1)} · ${list.size} 选一",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    list.forEachIndexed { index, exercise ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selected = selected + (group to index) }
                                .padding(vertical = 2.dp),
                        ) {
                            RadioButton(
                                selected = selected[group] == index,
                                onClick = { selected = selected + (group to index) },
                            )
                            Text(
                                "${index + 1}.",
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.width(26.dp),
                            )
                            Column(Modifier.weight(1f)) {
                                Text(exercise.name, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    exercise.detailText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(selected) }) { Text("确定") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

/**
 * 板块切换按钮：一排三个等宽，选中态用主色填充。
 * 用途：今日计划 / 训练模板 / 动作库 三个板块的入口。
 */
@Composable
internal fun SectionTab(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = modifier
            .heightIn(min = 38.dp)
            .clip(MaterialTheme.shapes.small)
            .background(bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = fg, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

/**
 * 一排按钮均分宽度：能放一行就放一行（[perRow]），放不下就平均分成两行，
 * 避免手机上出现被截断的半个按钮。
 */
@Composable
internal fun EvenButtons(
    items: List<Pair<String, () -> Unit>>,
    perRow: Int = 2,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.chunked(perRow.coerceAtLeast(1)).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                rowItems.forEach { (label, action) ->
                    OutlinedButton(
                        onClick = action,
                        modifier = Modifier.weight(1f).heightIn(min = 36.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                    ) {
                        Text(label, fontSize = 12.sp, maxLines = 1)
                    }
                }
                // 最后一行不满时用空位占住，保证按钮宽度不被拉宽
                repeat(perRow - rowItems.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}