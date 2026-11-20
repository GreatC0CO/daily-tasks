package com.deepseek.dailytasks.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepseek.dailytasks.data.EXERCISE_COLORS
import com.deepseek.dailytasks.data.Exercise
import com.deepseek.dailytasks.data.WorkoutLogic
import com.deepseek.dailytasks.data.WorkoutTemplate
import com.deepseek.dailytasks.data.romanMark
import com.deepseek.dailytasks.data.variantMark

/** 训练模板独立版块：查看 / 新建 / 编辑 / 删除 / 导入到今天的训练。 */
@Composable
fun TemplateScreen(
    templates: List<WorkoutTemplate>,
    parts: List<String>,
    library: List<Exercise>,
    onBack: () -> Unit,
    onSave: (WorkoutTemplate) -> Unit,
    onDelete: (Long) -> Unit,
    onImport: (WorkoutTemplate) -> Unit,
) {
    var editing by remember { mutableStateOf<WorkoutTemplate?>(null) }
    var creating by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 4.dp, end = 12.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                }
                Text("训练模板", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { creating = true }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("新建模板")
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
        ) {
            if (templates.isEmpty()) {
                Spacer(Modifier.height(60.dp))
                Text(
                    text = "还没有模板。\n模板是一套可以反复用的动作清单：先在这里建好，练的时候一键导入，再按当天状态微调组数 / 重量。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = "共 ${templates.size} 套模板 · 点卡片可以编辑、导入或删除",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(templates, key = { it.id }) { template ->
                        TemplateCard(
                            template = template,
                            onEdit = { editing = template },
                            onImport = { onImport(template) },
                            onDelete = { onDelete(template.id) },
                        )
                    }
                }
            }
        }
    }

    if (creating) {
        TemplateEditorDialog(
            initial = null,
            parts = parts,
            library = library,
            onDismiss = { creating = false },
            onSave = { template ->
                onSave(template)
                creating = false
            },
        )
    }
    editing?.let { template ->
        TemplateEditorDialog(
            initial = template,
            parts = parts,
            library = library,
            onDismiss = { editing = null },
            onSave = { updated ->
                onSave(updated)
                editing = null
            },
        )
    }
}

@Composable
private fun TemplateCard(
    template: WorkoutTemplate,
    onEdit: () -> Unit,
    onImport: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = template.name.ifBlank { "未命名模板" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "删除模板",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Text(
                text = template.summaryText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            // 按动作名分层展示：第一层是动作，第二层是同名动作的不同练法 ①②③
            val groups = remember(template.exercises) { groupExercises(template.exercises) }
            groups.take(4).forEach { group ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        group.name,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    if (group.items.size > 1) {
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "${group.items.size} 种练法",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        group.items.mapIndexed { index, e ->
                            if (group.items.size > 1) romanMark(index) + e.detailText else e.detailText
                        }.joinToString(" / "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (groups.size > 4) {
                Text(
                    text = "…还有 ${groups.size - 4} 个动作",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                MiniButton(text = "编辑", onClick = onEdit, modifier = Modifier.weight(1f))
                MiniButton(text = "导入到今日", onClick = onImport, primary = true, modifier = Modifier.weight(1f))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TemplateEditorDialog(
    initial: WorkoutTemplate?,
    parts: List<String>,
    library: List<Exercise>,
    onDismiss: () -> Unit,
    onSave: (WorkoutTemplate) -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    val exercises = remember {
        (initial?.exercises ?: emptyList()).toMutableStateList()
    }
    val chosenParts = remember {
        (initial?.parts ?: emptyList()).toMutableStateList()
    }
    var exerciseEditor by remember { mutableStateOf<Exercise?>(null) }
    var adding by remember { mutableStateOf(false) }
    var libraryOpen by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "新建模板" else "编辑模板") },
        text = {
            Column(modifier = Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("模板名（如 周一胸日）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "适用部位（可多选）",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    (parts + chosenParts.filter { it !in parts }).forEach { part ->
                        FilterChip(
                            selected = part in chosenParts,
                            onClick = {
                                if (part in chosenParts) chosenParts.remove(part) else chosenParts.add(part)
                            },
                            label = { Text(part, fontSize = 12.sp) },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                val groups = groupExercises(exercises)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "动作（${groups.size} 种 · ${exercises.sumOf { it.sets }} 组）",
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { exerciseEditor = null; adding = true }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Text("加动作", fontSize = 12.sp)
                    }
                    TextButton(onClick = { libraryOpen = true }) {
                        Text("从动作库选", fontSize = 12.sp)
                    }
                }
                Text(
                    text = "第一层是动作，第二层是同一个动作的不同重量/次数（Ⅰ Ⅱ Ⅲ）。导入今日时只选其中一个。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                groups.forEachIndexed { groupIndex, group ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                    ) {
                        Column(Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = group.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.weight(1f),
                                )
                                if (group.items.size > 1) {
                                    Text(
                                        text = "· ${group.items.size} 种练法",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                                IconButton(
                                    onClick = { moveGroup(exercises, groups, groupIndex, groupIndex - 1) },
                                    modifier = Modifier.size(28.dp),
                                ) {
                                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "上移", modifier = Modifier.size(15.dp))
                                }
                                IconButton(
                                    onClick = { moveGroup(exercises, groups, groupIndex, groupIndex + 1) },
                                    modifier = Modifier.size(28.dp),
                                ) {
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "下移", modifier = Modifier.size(15.dp))
                                }
                            }
                            group.items.forEachIndexed { variantIndex, exercise ->
                                TemplateVariantRow(
                                    exercise = exercise,
                                    mark = romanMark(variantIndex),
                                    onEdit = { exerciseEditor = exercise },
                                    onUp = { swapVariant(exercises, exercise, -1) },
                                    onDown = { swapVariant(exercises, exercise, 1) },
                                    onDelete = { exercises.remove(exercise) },
                                )
                            }
                        }
                    }
                }
                if (exercises.isEmpty()) {
                    Text(
                        text = "还没有动作，点上面「加动作」或「从动作库选」。",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 6.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        WorkoutTemplate(
                            id = initial?.id ?: 0L,
                            name = name.trim(),
                            parts = chosenParts.toList(),
                            exercises = exercises.toList(),
                        )
                    )
                },
                enabled = name.isNotBlank() && exercises.isNotEmpty(),
            ) { Text(if (initial == null) "保存" else "保存修改") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )

    if (adding) {
        ExerciseEditorDialog(
            initial = null,
            onDismiss = { adding = false },
            onSave = { exercise ->
                exercises.add(exercise)
                adding = false
            },
        )
    }
    exerciseEditor?.let { exercise ->
        ExerciseEditorDialog(
            initial = exercise,
            onDismiss = { exerciseEditor = null },
            onSave = { updated ->
                val index = exercises.indexOf(exercise)
                if (index >= 0) exercises[index] = updated
                exerciseEditor = null
            },
        )
    }
    if (libraryOpen) {
        TemplateLibraryDialog(
            library = library,
            existingKeys = exercises.map { it.key }.toSet(),
            onDismiss = { libraryOpen = false },
            onPick = { exercise ->
                exercises.add(exercise)
            },
        )
    }
}

/** 模板编辑里的一组同名动作：第一层。 */
private data class ExerciseGroup(val name: String, val items: List<Exercise>)

private fun groupExercises(exercises: List<Exercise>): List<ExerciseGroup> =
    exercises.groupBy { it.name.trim().lowercase() }
        .map { (_, list) -> ExerciseGroup(list.first().name, list) }

/** 拖动整组（同名动作连同它的所有练法一起移动）。 */
private fun moveGroup(
    exercises: MutableList<Exercise>,
    groups: List<ExerciseGroup>,
    from: Int,
    to: Int,
) {
    if (from !in groups.indices || to !in groups.indices || from == to) return
    val blocks = groups.map { it.items }.toMutableList()
    val block = blocks.removeAt(from)
    blocks.add(to, block)
    exercises.clear()
    blocks.forEach { exercises.addAll(it) }
}

/** 在同一组内上下调整练法顺序。 */
private fun swapVariant(exercises: MutableList<Exercise>, exercise: Exercise, delta: Int) {
    val name = exercise.name.trim().lowercase()
    val positions = exercises.indices.filter { exercises[it].name.trim().lowercase() == name }
    val current = exercises.indexOf(exercise)
    val listIndex = positions.indexOf(current)
    val targetListIndex = listIndex + delta
    if (listIndex < 0 || targetListIndex !in positions.indices) return
    val target = positions[targetListIndex]
    exercises[current] = exercises[target]
    exercises[target] = exercise
}

@Composable
private fun TemplateVariantRow(
    exercise: Exercise,
    mark: String,
    onEdit: () -> Unit,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .padding(vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = mark.ifEmpty { "·" },
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(22.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = exercise.detailText,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "点一下改组数 / 重量",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
        IconButton(onClick = onUp, modifier = Modifier.size(30.dp)) {
            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "上移", modifier = Modifier.size(15.dp))
        }
        IconButton(onClick = onDown, modifier = Modifier.size(30.dp)) {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "下移", modifier = Modifier.size(15.dp))
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "删除这个练法",
                modifier = Modifier.size(15.dp),
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}

/** 从动作库挑一个动作加进模板。 */
@Composable
private fun TemplateLibraryDialog(
    library: List<Exercise>,
    existingKeys: Set<String>,
    onDismiss: () -> Unit,
    onPick: (Exercise) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("从动作库选择") },
        text = {
            if (library.isEmpty()) {
                Text("动作库还是空的。先在计划里加几个动作，动作库会自动汇总所有计划里的动作。")
            } else {
                Column(modifier = Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                    library.forEach { exercise ->
                        val already = exercise.key in existingKeys
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !already) { onPick(exercise) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val marks = WorkoutLogic.groupMarks(library)
                            val mark = marks[exercise.name.trim().lowercase()] ?: ""
                            Text(
                                text = mark,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.width(20.dp),
                            )
                            Text(
                                text = exercise.name,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = if (already) "已添加" else exercise.detailText,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("完成") } },
    )
}

/** 小号按钮：给「加动作 / 动作库 / 模板」这类并排入口用，避免文字被挤断。 */
@Composable
internal fun SmallActionButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
        modifier = Modifier.heightIn(min = 32.dp),
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(4.dp))
        }
        Text(text, fontSize = 12.sp, maxLines = 1)
    }
}

@Composable
private fun MiniButton(
    text: String,
    onClick: () -> Unit,
    primary: Boolean = false,
    modifier: Modifier = Modifier,
) {
    if (primary) {
        androidx.compose.material3.Button(
            onClick = onClick,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            modifier = modifier.heightIn(min = 32.dp),
        ) { Text(text, fontSize = 12.sp) }
    } else {
        OutlinedButton(
            onClick = onClick,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            modifier = modifier.heightIn(min = 32.dp),
        ) { Text(text, fontSize = 12.sp) }
    }
}
