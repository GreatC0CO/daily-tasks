package com.deepseek.dailytasks.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.deepseek.dailytasks.data.EXERCISE_COLORS
import com.deepseek.dailytasks.data.Exercise
import com.deepseek.dailytasks.data.WorkoutLogic
import com.deepseek.dailytasks.data.WorkoutPlan

/**
 * 计划编辑（表格版）：每个动作一行，组数 / 次数 / 重量可以直接在格子里改，
 * ① ② 是备选组编号（同一组的动作今天只做一个），行尾菜单负责排序、拆分、删除。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PlanTableEditorDialog(
    plan: WorkoutPlan,
    availableParts: List<String>,
    onDismiss: () -> Unit,
    onSave: (WorkoutPlan) -> Unit,
) {
    var title by remember { mutableStateOf(plan.title) }
    var part by remember { mutableStateOf(plan.part) }
    var exercises by remember { mutableStateOf(plan.exercises) }
    var editIndex by remember { mutableStateOf<Int?>(null) }
    var menuIndex by remember { mutableStateOf<Int?>(null) }
    var splitIndex by remember { mutableStateOf<Int?>(null) }
    var mergeName by remember { mutableStateOf<String?>(null) }
    var newName by remember { mutableStateOf("") }
    var newSets by remember { mutableStateOf("3") }
    var newReps by remember { mutableStateOf("12") }
    var newWeight by remember { mutableStateOf("") }

    fun update(index: Int, transform: (Exercise) -> Exercise) {
        exercises = exercises.toMutableList().also { list -> list[index] = transform(list[index]) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("计划表格 · ${plan.part}") },
        text = {
            Column(
                Modifier
                    .heightIn(max = 470.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("计划名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    availableParts.forEach { p ->
                        FilterChip(
                            selected = part == p,
                            onClick = { part = p },
                            label = { Text(p) },
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                val marks = WorkoutLogic.groupMarks(exercises)
                Text(
                    "表格：左右滑动可看全部列；组数 / 次数 / 重量直接改格子。① ② 表示同一备选组，今天只做一个。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                ) {
                    Column(
                        Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 4.dp),
                    ) {
                        TableHeader()
                        exercises.forEachIndexed { index, exercise ->
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            TableRow(
                                exercise = exercise,
                                mark = marks[exercise.name.trim().lowercase()] ?: "",
                                canUp = index > 0,
                                canDown = index < exercises.lastIndex,
                                menuOpen = menuIndex == index,
                                onChange = { updated -> update(index, { updated }) },
                                onOpenDetail = { editIndex = index },
                                onMenu = { menuIndex = index },
                                onDismissMenu = { menuIndex = null },
                                onUp = { exercises = WorkoutLogic.moveItem(exercises, index, index - 1) },
                                onDown = { exercises = WorkoutLogic.moveItem(exercises, index, index + 1) },
                                onSplit = {
                                    menuIndex = null
                                    splitIndex = index
                                },
                                onDelete = {
                                    menuIndex = null
                                    exercises = exercises.filterIndexed { i, _ -> i != index }
                                },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        placeholder = { Text("新动作名", style = MaterialTheme.typography.labelSmall) },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(4.dp))
                    CompactCell(value = newSets, onChange = { newSets = it.filter(Char::isDigit).take(2) }, width = 48.dp, hint = "组")
                    Spacer(Modifier.width(4.dp))
                    CompactCell(value = newReps, onChange = { newReps = it.take(6) }, width = 52.dp, hint = "次")
                    Spacer(Modifier.width(4.dp))
                    CompactCell(
                        value = newWeight,
                        onChange = { newWeight = it.filter { c -> c.isDigit() || c == '.' }.take(6) },
                        width = 56.dp,
                        hint = "kg",
                    )
                    Spacer(Modifier.width(4.dp))
                    TextButton(
                        onClick = {
                            val n = newName.trim()
                            if (n.isEmpty()) return@TextButton
                            exercises = exercises + Exercise(
                                name = n,
                                sets = newSets.toIntOrNull()?.coerceIn(1, 20) ?: 3,
                                reps = newReps.trim().ifEmpty { "12" },
                                weight = newWeight.toDoubleOrNull()?.coerceIn(0.0, 1000.0) ?: 0.0,
                            )
                            newName = ""
                            newSets = "3"
                            newReps = "12"
                            newWeight = ""
                        },
                        enabled = newName.isNotBlank(),
                    ) { Text("＋") }
                }
                val duplicateNames = exercises
                    .groupBy { it.name.trim().lowercase() }
                    .filter { it.value.size > 1 }
                    .keys
                if (duplicateNames.isNotEmpty()) {
                    TextButton(onClick = { mergeName = duplicateNames.first() }) {
                        Text("合并同名动作（${duplicateNames.size} 组可选）")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        plan.copy(
                            title = title.trim().ifEmpty { "${part} 日" },
                            part = part,
                            exercises = exercises,
                        ),
                    )
                },
                enabled = exercises.isNotEmpty(),
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )

    editIndex?.let { index ->
        val target = exercises.getOrNull(index)
        if (target != null) {
            ExerciseEditorDialog(
                initial = target,
                onDismiss = { editIndex = null },
                onSave = { updated ->
                    update(index) { updated }
                    editIndex = null
                },
            )
        }
    }

    splitIndex?.let { index ->
        val target = exercises.getOrNull(index)
        if (target != null) {
            SplitExerciseDialog(
                name = target.name,
                sets = target.sets,
                reps = target.reps,
                weight = target.weight,
                onDismiss = { splitIndex = null },
                onConfirm = { firstSets, repsA, weightA, repsB, weightB ->
                    val pair = WorkoutLogic.splitExercise(target, firstSets, repsA, weightA, repsB, weightB)
                    exercises = exercises.toMutableList().also {
                        it.removeAt(index)
                        it.addAll(index, pair)
                    }
                    splitIndex = null
                },
            )
        }
    }

    mergeName?.let { name ->
        val variants = exercises.filter { it.name.trim().equals(name, ignoreCase = true) }
        if (variants.size >= 2) {
            MergeSameNameDialog(
                name = variants.first().name,
                variants = variants,
                onDismiss = { mergeName = null },
                onConfirm = { reps, weight ->
                    val group = exercises.filter { it.name.trim().equals(name, ignoreCase = true) }
                    val firstIndex = exercises.indexOfFirst { it.name.trim().equals(name, ignoreCase = true) }
                    val mergedExercise = group.reduce { acc, e -> WorkoutLogic.mergePair(acc, e, reps, weight) }
                    exercises = exercises.toMutableList().also { list ->
                        list.removeAll { it.name.trim().equals(name, ignoreCase = true) }
                        list.add(firstIndex.coerceIn(0, list.size), mergedExercise)
                    }
                    mergeName = null
                },
            )
        } else {
            mergeName = null
        }
    }
}

@Composable
private fun TableHeader() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Spacer(Modifier.width(22.dp))
        Text(
            "动作",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(120.dp),
        )
        TableHeaderCell("组", 48.dp)
        TableHeaderCell("次", 52.dp)
        TableHeaderCell("重(kg)", 56.dp)
        Spacer(Modifier.width(38.dp))
    }
}

@Composable
private fun TableHeaderCell(text: String, width: Dp) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .width(width)
            .padding(start = 4.dp),
    )
}

@Composable
private fun TableRow(
    exercise: Exercise,
    mark: String,
    canUp: Boolean,
    canDown: Boolean,
    menuOpen: Boolean,
    onChange: (Exercise) -> Unit,
    onOpenDetail: () -> Unit,
    onMenu: () -> Unit,
    onDismissMenu: () -> Unit,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onSplit: () -> Unit,
    onDelete: () -> Unit,
) {
    var setsText by remember(exercise) { mutableStateOf(exercise.sets.toString()) }
    var repsText by remember(exercise) { mutableStateOf(exercise.reps) }
    var weightText by remember(exercise) {
        mutableStateOf(if (exercise.weight > 0.0) formatPlain(exercise.weight) else "")
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = mark,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(end = 4.dp)
                .width(18.dp),
        )
        Column(
            modifier = Modifier
                .width(120.dp)
                .clickable(onClick = onOpenDetail),
        ) {
            Text(
                text = exercise.name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "点开可改名称/重量",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        CompactCell(
            value = setsText,
            onChange = { raw ->
                setsText = raw.filter(Char::isDigit).take(2)
                setsText.toIntOrNull()?.let { onChange(exercise.copy(sets = it.coerceIn(1, 20))) }
            },
            width = 48.dp,
            hint = "组",
        )
        CompactCell(
            value = repsText,
            onChange = { raw ->
                repsText = raw.take(6)
                if (repsText.isNotBlank()) onChange(exercise.copy(reps = repsText.trim()))
            },
            width = 52.dp,
            hint = "次",
        )
        CompactCell(
            value = weightText,
            onChange = { raw ->
                weightText = raw.filter { c -> c.isDigit() || c == '.' }.take(6)
                val w = weightText.toDoubleOrNull()?.coerceIn(0.0, 1000.0) ?: 0.0
                onChange(exercise.copy(weight = w))
            },
            width = 56.dp,
            hint = "kg",
        )
        Box {
            IconButton(onClick = onMenu, modifier = Modifier.size(38.dp)) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "更多",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = onDismissMenu) {
                DropdownMenuItem(
                    text = { Text("编辑详情（名称 / 组数 / 重量 / 备选组）") },
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                    onClick = {
                        onDismissMenu()
                        onOpenDetail()
                    },
                )
                DropdownMenuItem(
                    text = { Text("上移") },
                    enabled = canUp,
                    leadingIcon = { Icon(Icons.Default.KeyboardArrowUp, contentDescription = null) },
                    onClick = {
                        onDismissMenu()
                        onUp()
                    },
                )
                DropdownMenuItem(
                    text = { Text("下移") },
                    enabled = canDown,
                    leadingIcon = { Icon(Icons.Default.KeyboardArrowDown, contentDescription = null) },
                    onClick = {
                        onDismissMenu()
                        onDown()
                    },
                )
                if (exercise.sets >= 2) {
                    DropdownMenuItem(
                        text = { Text("拆成两段（组数不变）") },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        onClick = onSplit,
                    )
                }
                DropdownMenuItem(
                    text = { Text("删除动作") },
                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                    onClick = onDelete,
                )
            }
        }
    }
}

@Composable
private fun CompactCell(
    value: String,
    onChange: (String) -> Unit,
    width: Dp,
    hint: String,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        placeholder = { Text(hint, style = MaterialTheme.typography.labelSmall) },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodySmall,
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier
            .width(width)
            .padding(end = 4.dp),
    )
}

/** 22.5 → "22.5"，60.0 → "60"。 */
private fun formatPlain(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()

