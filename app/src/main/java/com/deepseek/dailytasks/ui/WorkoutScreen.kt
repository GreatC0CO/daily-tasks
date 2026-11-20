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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepseek.dailytasks.data.BodyStore
import com.deepseek.dailytasks.data.CategoryProgress
import com.deepseek.dailytasks.data.EXERCISE_COLORS
import com.deepseek.dailytasks.data.Exercise
import com.deepseek.dailytasks.data.ExerciseLibraryStore
import com.deepseek.dailytasks.data.SessionCategory
import com.deepseek.dailytasks.data.SessionEntry
import com.deepseek.dailytasks.data.WorkoutDraft
import com.deepseek.dailytasks.data.WorkoutLogic
import com.deepseek.dailytasks.data.WorkoutPlan
import com.deepseek.dailytasks.data.WorkoutSession
import com.deepseek.dailytasks.data.WorkoutStore
import com.deepseek.dailytasks.data.WorkoutTemplate
import com.deepseek.dailytasks.data.categoryProgress
import com.deepseek.dailytasks.data.formatVolume
import com.deepseek.dailytasks.data.variantMark
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val workoutDateFmt = DateTimeFormatter.ofPattern("M月d日 EEE", Locale.CHINA)

/** 动作颜色下标：0 = 不标色，1..8 对应 EXERCISE_COLORS。 */
private fun exerciseColor(index: Int): Color =
    EXERCISE_COLORS.getOrNull(index - 1)?.let { Color(it) } ?: Color.Transparent

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkoutScreen(store: WorkoutStore) {
    val today = remember { LocalDate.now() }
    var availableParts by remember { mutableStateOf(store.loadParts()) }
    var plans by remember { mutableStateOf(store.loadPlans()) }
    var todayParts by remember { mutableStateOf<List<String>>(emptyList()) }
    var entries by remember { mutableStateOf<List<SessionEntry>>(emptyList()) }
    var templates by remember { mutableStateOf(store.loadTemplates()) }
    var recent by remember { mutableStateOf(store.loadSessions()) }

    var partManagerOpen by remember { mutableStateOf(false) }
    var editingPlan by remember { mutableStateOf<WorkoutPlan?>(null) }
    var exerciseEditor by remember { mutableStateOf<ExerciseEditorState?>(null) }
    var saveTemplateOpen by remember { mutableStateOf(false) }
    var importTemplateOpen by remember { mutableStateOf(false) }
    var weightTarget by remember { mutableStateOf<SessionEntry?>(null) }
    var splitTarget by remember { mutableStateOf<SessionEntry?>(null) }
    /** 颜色组 → 组内选项序号（不同动作的 N 选一，UI 里用 ①②③ 表示） */
    var picks by remember { mutableStateOf<Map<Int, Int>>(emptyMap()) }
    var pickerOpen by remember { mutableStateOf(false) }
    /** 选一弹窗的动作来源：null = 当前部位匹配的计划；导入模板时 = 模板里的动作 */
    var pickerSource by remember { mutableStateOf<List<Exercise>?>(null) }
    var libraryOpen by remember { mutableStateOf(false) }
    var templatesOpen by remember { mutableStateOf(false) }
    var bodyOpen by remember { mutableStateOf(false) }
    var journal by remember { mutableStateOf(store.loadJournal(today)) }
    var journalOpen by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val bodyStore = remember { BodyStore(context) }
    val libraryStore = remember { ExerciseLibraryStore(context) }
    var notes by remember { mutableStateOf(libraryStore.load()) }

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun saveDraft() {
        store.saveDraft(WorkoutDraft(today.toEpochDay(), todayParts, entries))
    }

    fun matchedExercises(nextParts: List<String>): List<Exercise> =
        WorkoutLogic.mergeExercises(WorkoutLogic.matchPlans(plans, nextParts))

    fun rebuild(nextParts: List<String>, base: List<SessionEntry>): List<SessionEntry> =
        WorkoutLogic.entriesOfPicks(matchedExercises(nextParts), base, picks)

    fun applyParts(next: List<String>) {
        todayParts = next
        entries = rebuild(next, entries)
        saveDraft()
        // 这批动作里有「同色动作 N 选一」：先让用户从每组挑一个动作，再开始练
        if (WorkoutLogic.altGroups(matchedExercises(next)).isNotEmpty()) {
            pickerOpen = true
        }
    }

    /** 把动作加进第一个选中部位的计划里，这样切换部位/重开 App 都还在。 */
    fun upsertIntoPlan(exercise: Exercise) {
        val part = todayParts.firstOrNull() ?: return
        val plan = plans.firstOrNull { it.part == part } ?: return
        val existing = plan.exercises.any { it.key == exercise.key }
        val nextExercises = if (existing) {
            plan.exercises.map { if (it.key == exercise.key) exercise else it }
        } else {
            plan.exercises + exercise
        }
        store.savePlan(plan.copy(exercises = nextExercises))
        plans = store.loadPlans()
    }

    fun updateEntry(exercise: Exercise) {
        entries = if (entries.any { it.key == exercise.key }) {
            entries.map {
                if (it.key == exercise.key) {
                    it.copy(sets = exercise.sets, reps = exercise.reps, weight = exercise.weight, color = exercise.color)
                } else {
                    it
                }
            }
        } else {
            WorkoutLogic.withExercise(entries, exercise)
        }
        upsertIntoPlan(exercise)
        saveDraft()
    }

    fun setEntryWeight(entry: SessionEntry, weight: Double) {
        entries = entries.map {
            if (it.key == entry.key) WorkoutLogic.setWeight(it, weight) else it
        }
        saveDraft()
    }

    /** 今天的清单里移除一条（同名不同重量的条目只删这一条）。 */
    fun removeEntry(entry: SessionEntry) {
        entries = WorkoutLogic.removeExerciseByKey(entries, entry.key)
        saveDraft()
    }

    /** 把今天的这条动作拆成两段（组数相加不变），两段可以分别用不同次数/重量。 */
    fun splitEntry(
        entry: SessionEntry,
        firstSets: Int,
        repsA: String,
        weightA: Double,
        repsB: String,
        weightB: Double,
    ) {
        if (entry.sets < 2) return
        val first = firstSets.coerceIn(1, entry.sets - 1)
        val second = entry.sets - first
        val a = entry.copy(
            sets = first,
            reps = repsA.trim().ifEmpty { entry.reps },
            weight = weightA.coerceIn(0.0, 1000.0),
            doneSets = entry.done.coerceAtMost(first),
        )
        val b = entry.copy(
            sets = second,
            reps = repsB.trim().ifEmpty { entry.reps },
            weight = weightB.coerceIn(0.0, 1000.0),
            doneSets = (entry.done - first).coerceIn(0, second),
        )
        val index = entries.indexOfFirst { it.key == entry.key }
        if (index < 0) return
        entries = entries.toMutableList().also {
            it.removeAt(index)
            it.addAll(index, listOf(a, b))
        }
        saveDraft()
    }

    /** 调整今日清单里的先后顺序（只影响今天，不改计划）。 */
    fun moveEntry(entry: SessionEntry, delta: Int) {
        val from = entries.indexOfFirst { it.key == entry.key }
        if (from < 0) return
        entries = WorkoutLogic.moveItem(entries, from, from + delta)
        saveDraft()
    }

    LaunchedEffect(Unit) {
        store.loadDraft(today)?.let { draft ->
            if (draft.parts.isNotEmpty() || draft.entries.isNotEmpty()) {
                todayParts = draft.parts
                entries = draft.entries
            }
        }
    }

    val matched = WorkoutLogic.matchPlans(plans, todayParts)
    val altGroups = WorkoutLogic.altGroups(WorkoutLogic.mergeExercises(matched))
    val done = WorkoutLogic.totalDone(entries)
    val planned = WorkoutLogic.totalPlanned(entries)
    val fraction = if (planned == 0) 0f else done.toFloat() / planned

    fun tapSet(entry: SessionEntry) {
        entries = entries.map { if (it.key == entry.key) WorkoutLogic.addSet(it) else it }
        saveDraft()
        val updated = entries.firstOrNull { it.key == entry.key }
        if (updated != null && updated.isComplete) {
            scope.launch { snackbar.showSnackbar("${entry.name}：${entry.sets} 组已完成 ✅") }
        }
    }

    fun importTemplate(template: WorkoutTemplate) {
        if (todayParts.isEmpty() && template.parts.isNotEmpty()) todayParts = template.parts
        picks = emptyMap()
        entries = WorkoutLogic.entriesOfPicks(template.exercises, entries)
        saveDraft()
        // 模板里有「同色动作 N 选一」，导入后直接让用户从每组挑一个动作
        if (WorkoutLogic.altGroups(template.exercises).isNotEmpty()) {
            pickerSource = template.exercises
            pickerOpen = true
        }
        scope.launch { snackbar.showSnackbar("已导入模板「${template.name}」，可以接着微调") }
    }

    fun addFromLibrary(exercise: Exercise) {
        if (entries.any { it.key == exercise.key }) {
            scope.launch { snackbar.showSnackbar("「${exercise.name}」已经在今天的清单里了") }
            return
        }
        entries = entries + SessionEntry(
            name = exercise.name,
            sets = exercise.sets,
            reps = exercise.reps,
            doneSets = 0,
            weight = exercise.weight,
            color = exercise.color,
        )
        saveDraft()
        scope.launch { snackbar.showSnackbar("已加入今日清单：${exercise.name} ${exercise.detailText}") }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (entries.isNotEmpty()) {
                Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 3.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "已完成 $done / $planned 组",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            if (WorkoutLogic.totalVolume(entries) > 0.0) {
                                Text(
                                    text = "今日容量 ${com.deepseek.dailytasks.data.formatVolume(WorkoutLogic.totalVolume(entries))}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { fraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        TextButton(
                            onClick = {
                                entries = entries.map { WorkoutLogic.resetSets(it) }
                                saveDraft()
                            }
                        ) { Text("重置") }
                        Button(
                            onClick = {
                                val now = System.currentTimeMillis()
                                store.addSession(
                                    WorkoutSession(
                                        id = now,
                                        epochDay = today.toEpochDay(),
                                        parts = todayParts,
                                        entries = entries,
                                        atMillis = now,
                                    )
                                )
                                store.pruneSessions(today)
                                recent = store.loadSessions()
                                entries = entries.map { WorkoutLogic.resetSets(it) }
                                saveDraft()
                                scope.launch {
                                    snackbar.showSnackbar("已记录：${todayParts.joinToString("+")}，共 $done 组")
                                }
                            },
                            enabled = done > 0,
                        ) { Text("完成训练") }
                    }
                }
            }
        },
    ) { innerPadding ->
        if (templatesOpen) {
            TemplateScreen(
                templates = templates,
                parts = availableParts,
                library = WorkoutLogic.exerciseLibrary(plans),
                onBack = { templatesOpen = false },
                onSave = { tpl ->
                    store.saveTemplate(tpl)
                    templates = store.loadTemplates()
                },
                onDelete = { id ->
                    store.deleteTemplate(id)
                    templates = store.loadTemplates()
                },
                onImport = { tpl ->
                    templatesOpen = false
                    importTemplate(tpl)
                },
            )
            return@Scaffold
        }
        if (bodyOpen) {
            BodyScreen(store = bodyStore, onBack = { bodyOpen = false })
            return@Scaffold
        }
        if (libraryOpen) {
            ExerciseLibraryScreen(
                items = WorkoutLogic.exerciseLibrary(plans),
                notes = notes,
                onSaveNote = { note ->
                    val next = notes.filterNot { it.name.equals(note.name, ignoreCase = true) } + note
                    notes = next
                    libraryStore.save(next)
                },
                onBack = { libraryOpen = false },
                onPick = { picked ->
                    libraryOpen = false
                    addFromLibrary(picked)
                },
            )
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "今日训练",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.width(10.dp))
                    JournalBadge(
                        text = journal,
                        onClick = { journalOpen = true },
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = today.format(workoutDateFmt),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "选部位 → 自动带出计划 → 练完一组点一下动作行；动作和部位都能自己加。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "今日部位",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { partManagerOpen = true }) { Text("管理部位") }
                }
                Spacer(Modifier.height(4.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    availableParts.forEach { part ->
                        FilterChip(
                            selected = part in todayParts,
                            onClick = {
                                val next = if (part in todayParts) {
                                    todayParts - part
                                } else {
                                    todayParts + part
                                }
                                applyParts(next)
                            },
                            label = { Text(part) },
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                // 三大板块：今日计划 / 训练模板 / 动作库（一排平均分，不挤）
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    SectionTab("今日计划", selected = true, onClick = {}, modifier = Modifier.weight(1f))
                    SectionTab("训练模板", selected = false, onClick = { templatesOpen = true }, modifier = Modifier.weight(1f))
                    SectionTab("动作库", selected = false, onClick = { libraryOpen = true }, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                // 今日计划里的操作：每行最多 2 个，平均分宽度
                EvenButtons(
                    items = listOf(
                        "加动作" to { exerciseEditor = ExerciseEditorState(initial = null) },
                        "身体数据" to { bodyOpen = true },
                        "存为模板" to { saveTemplateOpen = true },
                        "导入模板" to { importTemplateOpen = true },
                    ),
                    perRow = 2,
                )
                if (altGroups.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    EvenButtons(
                        items = listOf(
                            "同组动作选一（①②）" to {
                                pickerSource = null
                                pickerOpen = true
                            },
                        ),
                        perRow = 1,
                    )
                }
                if (templates.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "已有模板：${templates.joinToString("、") { it.name }}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

            if (todayParts.isEmpty()) {
                HintCard("还没有选部位", "从上面点一个部位（比如「胸」），对应的训练计划会自动出现在这里；没有想要的部位就点「管理部位」自己加。")
            } else if (matched.isEmpty()) {
                HintCard("没有找到部位的计划", "点下面的「编辑」新建一份，或者用「加动作」直接往今天加。")
            } else {
                matched.forEach { plan ->
                    PlanSection(
                        plan = plan,
                        entries = entries.filter { e -> plan.exercises.any { it.key == e.key } },
                        onTap = { tapSet(it) },
                        onReset = { e ->
                            entries = entries.map { if (it.key == e.key) WorkoutLogic.resetSets(it) else it }
                            saveDraft()
                        },
                        onEdit = { editingPlan = plan },
                        onWeightEdit = { e -> weightTarget = e },
                        onSplit = { e -> splitTarget = e },
                    )
                }
            }

            val extraEntries = entries.filter { e -> matched.none { m -> m.exercises.any { it.key == e.key } } }
            val overallMarks = WorkoutLogic.groupMarks(matched.flatMap { it.exercises })
            if (extraEntries.isNotEmpty()) {
                Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)) {
                    Text(
                        text = "今天额外加的动作",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(6.dp))
                    extraEntries.forEach { entry ->
                        ExerciseRow(
                            entry = entry,
                            onTap = { tapSet(entry) },
                            onReset = {
                                entries = entries.map { if (it.key == entry.key) WorkoutLogic.resetSets(it) else it }
                                saveDraft()
                            },
                            onEdit = {
                                exerciseEditor = ExerciseEditorState(
                                    initial = Exercise(entry.name, entry.sets, entry.reps, entry.weight, entry.color)
                                )
                            },
                            onWeightEdit = { weightTarget = entry },
                            onRemove = { removeEntry(entry) },
                            onSplit = { splitTarget = entry },
                            modifier = Modifier.padding(bottom = 6.dp),
                            mark = overallMarks[entry.name.trim().lowercase()] ?: "",
                        )
                    }
                }
            }

            if (recent.isNotEmpty()) {
                Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp)) {
                    Text(
                        text = "训练进步",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(6.dp))
                    SessionCategory.values().forEach { category ->
                        val progress = categoryProgress(recent, category)
                        if (category != SessionCategory.OTHER || progress.sessions.isNotEmpty()) {
                            CategoryProgressCard(progress)
                            Spacer(Modifier.height(6.dp))
                        }
                    }
                }

                Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp)) {
                    Text(
                        text = "最近训练",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "点开一条，回看当时的完整计划",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    recent.take(5).forEach { session ->
                        SessionRow(
                            session = session,
                            onDelete = {
                                store.deleteSession(session.id)
                                recent = store.loadSessions()
                            },
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                }
            }

            Text(
                text = "训练记录会保存在本机的「最近训练」里；每天 0 点之后草稿自动作废，重新开始。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            )
        }
    }

    if (partManagerOpen) {
        PartManagerDialog(
            parts = availableParts,
            onDismiss = { partManagerOpen = false },
            onAdd = { name ->
                val added = store.addPart(name)
                if (added) {
                    availableParts = store.loadParts()
                    plans = store.loadPlans()
                }
                added
            },
            onRename = { old, new ->
                val renamed = store.renamePart(old, new)
                if (renamed) {
                    availableParts = store.loadParts()
                    plans = store.loadPlans()
                    todayParts = todayParts.map { if (it == old) new.trim() else it }
                    entries = rebuild(todayParts, entries)
                    saveDraft()
                }
                renamed
            },
            onRemove = { name ->
                store.removePart(name)
                availableParts = store.loadParts()
                plans = store.loadPlans()
                if (name in todayParts) {
                    todayParts = todayParts - name
                    entries = rebuild(todayParts, entries)
                    saveDraft()
                }
            },
        )
    }

    splitTarget?.let { entry ->
        SplitExerciseDialog(
            name = entry.name,
            sets = entry.sets,
            reps = entry.reps,
            weight = entry.weight,
            onDismiss = { splitTarget = null },
            onConfirm = { firstSets, repsA, weightA, repsB, weightB ->
                splitEntry(entry, firstSets, repsA, weightA, repsB, weightB)
                splitTarget = null
            },
        )
    }

    exerciseEditor?.let { state ->
        ExerciseEditorDialog(
            initial = state.initial,
            onDismiss = { exerciseEditor = null },
            onSave = { exercise ->
                exerciseEditor = null
                updateEntry(exercise)
            },
        )
    }

    if (saveTemplateOpen) {
        TemplateSaveDialog(
            defaultName = todayParts.joinToString("+").ifBlank { "我的训练" },
            exerciseCount = entries.size,
            existingNames = templates.map { it.name },
            onDismiss = { saveTemplateOpen = false },
            onSave = { name ->
                val template = WorkoutLogic.templateFrom(
                    id = System.currentTimeMillis(),
                    name = name,
                    parts = todayParts,
                    entries = entries,
                )
                store.saveTemplate(template)
                templates = store.loadTemplates()
                saveTemplateOpen = false
                scope.launch { snackbar.showSnackbar("模板「${template.name}」已保存") }
            },
        )
    }

    if (importTemplateOpen) {
        TemplateImportDialog(
            templates = templates,
            onDismiss = { importTemplateOpen = false },
            onPick = {
                importTemplateOpen = false
                importTemplate(it)
            },
            onDelete = { template ->
                store.deleteTemplate(template.id)
                templates = store.loadTemplates()
            },
        )
    }

    editingPlan?.let { plan ->
        PlanTableEditorDialog(
            plan = plan,
            availableParts = availableParts,
            onDismiss = { editingPlan = null },
            onSave = { updated ->
                editingPlan = null
                store.savePlan(updated)
                plans = store.loadPlans()
                entries = rebuild(todayParts, entries)
                saveDraft()
            },
        )
    }

    weightTarget?.let { entry ->
        WeightDialog(
            name = entry.name,
            initial = entry.weight,
            onDismiss = { weightTarget = null },
            onSave = { weight ->
                weightTarget = null
                setEntryWeight(entry, weight)
            },
        )
    }

    if (pickerOpen) {
        val source = pickerSource ?: WorkoutLogic.mergeExercises(matched)
        AltPickerDialog(
            groups = WorkoutLogic.altGroups(source),
            current = picks,
            onDismiss = {
                pickerOpen = false
                pickerSource = null
            },
            onConfirm = { selected ->
                picks = selected
                entries = WorkoutLogic.entriesOfPicks(source, entries, picks)
                pickerOpen = false
                pickerSource = null
                saveDraft()
            },
        )
    }

    if (journalOpen) {
        JournalDialog(
            date = today,
            initial = journal,
            onDismiss = { journalOpen = false },
            onSave = { value ->
                journalOpen = false
                journal = value.trim()
                store.saveJournal(today, journal)
                scope.launch {
                    snackbar.showSnackbar(
                        if (journal.isBlank()) "已清空今日日记" else "今日日记已保存"
                    )
                }
            },
        )
    }
}

/** 标题右边的日记入口：没写过显示「写今日日记」，写过就带一句开头。 */
@Composable
private fun JournalBadge(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Edit,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(13.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = if (text.isBlank()) {
                    "写今日日记"
                } else {
                    "日记 · " + text.lineSequence().first().take(12)
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun JournalDialog(
    date: LocalDate,
    initial: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("今日锻炼日记") },
        text = {
            Column {
                Text(
                    text = date.format(workoutDateFmt),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp),
                    placeholder = { Text("今天练得怎么样？重量、状态、感受……") },
                    minLines = 4,
                    maxLines = 8,
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "按天保存，换一天就是新的一篇。",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.weight(1f))
                    if (initial.isNotBlank()) {
                        TextButton(onClick = { onSave("") }) { Text("清空") }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(text) }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

/** 「加动作 / 改动作」用的临时状态。 */
private data class ExerciseEditorState(val initial: Exercise?)

@Composable
private fun HintCard(title: String, body: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** 一大类训练的进步卡：最近一次做了什么，比上一次多了多少，最近几次的容量走势。 */
@Composable
private fun CategoryProgressCard(progress: CategoryProgress) {
    val latest = progress.latest
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = progress.category.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = progress.deltaText,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (progress.volumeDelta > 0.0) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            Text(
                text = progress.category.subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (latest == null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "还没有这一类的记录",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "最近 ${latest.dateText} · ${latest.partText}",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = "完成 ${latest.totalDone}/${latest.totalPlanned} 组 · 容量 ${formatVolume(latest.totalVolume)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (progress.sessions.size > 1) {
                    Text(
                        text = "近 ${progress.sessions.size} 次容量：${progress.trendText}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionRow(session: WorkoutSession, onDelete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column {
            Row(
                modifier = Modifier.padding(start = 14.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { expanded = !expanded },
                ) {
                    Text(session.summaryText, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        session.dateText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expanded) "收起计划" else "查看计划",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "删除这条记录",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (expanded) {
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                Column(
                    modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    session.entries.forEach { entry ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (entry.color != 0) {
                                Text(
                                    text = variantMark(entry.color - 1),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(end = 6.dp),
                                )
                            }
                            Text(
                                text = entry.name,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = buildString {
                                    append("${entry.done}/${entry.sets} 组 × ${entry.reps}")
                                    if (entry.hasWeight) append(" · ${entry.weightText}")
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanSection(
    plan: WorkoutPlan,
    entries: List<SessionEntry>,
    onTap: (SessionEntry) -> Unit,
    onReset: (SessionEntry) -> Unit,
    onEdit: () -> Unit,
    onWeightEdit: (SessionEntry) -> Unit,
    onSplit: (SessionEntry) -> Unit,
) {
    val done = entries.sumOf { it.done }
    val planned = entries.sumOf { it.sets }
    val marks = WorkoutLogic.groupMarks(plan.exercises)

    Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = plan.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "$planned 组 · ${plan.exercises.size} 个动作 · 已完成 $done 组",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onEdit) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "编辑计划",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (entries.isEmpty()) {
            Text(
                text = "这个部位还没有动作，点上面的「加动作」添加。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }

        Spacer(Modifier.height(6.dp))
        entries.forEach { entry ->
            ExerciseRow(
                entry = entry,
                onTap = { onTap(entry) },
                onReset = { onReset(entry) },
                onEdit = null,
                onWeightEdit = { onWeightEdit(entry) },
                onRemove = null,
                onSplit = { onSplit(entry) },
                modifier = Modifier.padding(bottom = 6.dp),
                mark = marks[entry.name.trim().lowercase()] ?: "",
            )
        }
    }
}

@Composable
private fun ExerciseRow(
    entry: SessionEntry,
    onTap: () -> Unit,
    onReset: () -> Unit,
    onEdit: (() -> Unit)?,
    onRemove: (() -> Unit)?,
    modifier: Modifier = Modifier,
    onWeightEdit: (() -> Unit)? = null,
    onSplit: (() -> Unit)? = null,
    mark: String = "",
) {
    val complete = entry.isComplete
    var menuOpen by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onTap() },
        colors = CardDefaults.cardColors(
            containerColor = if (complete) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (complete) 0.dp else 1.dp),
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (mark.isNotEmpty()) {
                        Text(
                            text = mark,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 6.dp),
                        )
                    }
                    Text(
                        text = entry.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                    )
                }
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = entry.targetText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (entry.hasWeight) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            )
                            .clickable(enabled = onWeightEdit != null) { onWeightEdit?.invoke() }
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = entry.weightText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (entry.hasWeight) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                    if (entry.hasWeight && entry.done > 0) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "容量 ${entry.volumeText}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    repeat(entry.sets) { i ->
                        Box(
                            Modifier
                                .padding(end = 4.dp)
                                .size(10.dp)
                                .background(
                                    if (i < entry.done) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    CircleShape,
                                )
                        )
                    }
                }
            }
            Text(
                text = entry.progressText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (complete) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            IconButton(onClick = onReset) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "重置这组",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (onEdit != null || onRemove != null || onWeightEdit != null || onSplit != null) {
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "更多",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        if (onEdit != null) {
                            DropdownMenuItem(
                                text = { Text("编辑组数 / 次数") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = {
                                    menuOpen = false
                                    onEdit()
                                },
                            )
                        }
                        if (onWeightEdit != null) {
                            DropdownMenuItem(
                                text = { Text("改今天的重量") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = {
                                    menuOpen = false
                                    onWeightEdit()
                                },
                            )
                        }
                        if (onRemove != null) {
                            DropdownMenuItem(
                                text = { Text("从今天移除") },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                                onClick = {
                                    menuOpen = false
                                    onRemove()
                                },
                            )
                        }
                        if (onSplit != null && entry.sets >= 2) {
                            DropdownMenuItem(
                                text = { Text("拆成两段（组数不变）") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = {
                                    menuOpen = false
                                    onSplit()
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlanEditorDialog(
    plan: WorkoutPlan,
    availableParts: List<String>,
    onDismiss: () -> Unit,
    onSave: (WorkoutPlan) -> Unit,
) {
    var title by remember { mutableStateOf(plan.title) }
    var part by remember { mutableStateOf(plan.part) }
    var exercises by remember { mutableStateOf(plan.exercises) }
    var name by remember { mutableStateOf("") }
    var setsText by remember { mutableStateOf("3") }
    var reps by remember { mutableStateOf("12") }
    var weightText by remember { mutableStateOf("") }
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    var rowMenu by remember { mutableStateOf<Int?>(null) }
    var splitIndex by remember { mutableStateOf<Int?>(null) }
    var mergeName by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑「${plan.part}」计划") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("计划名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "这个计划练哪个部位",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
                Spacer(Modifier.height(6.dp))
                Text(
                    "动作列表（点圆点换颜色，点 ↑↓ 调整顺序，点 ⋮ 编辑 / 拆分 / 删除）",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                exercises.forEachIndexed { index, e ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(if (e.color == 0) MaterialTheme.colorScheme.surfaceVariant else exerciseColor(e.color))
                                .clickable {
                                    exercises = exercises.toMutableList().also {
                                        it[index] = e.copy(color = nextExerciseColor(e.color))
                                    }
                                },
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { editingIndex = index },
                        ) {
                            Text(e.name, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                e.detailText,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(
                            onClick = { exercises = WorkoutLogic.moveItem(exercises, index, index - 1) },
                            enabled = index > 0,
                        ) {
                            Icon(
                                Icons.Default.KeyboardArrowUp,
                                contentDescription = "上移",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(
                            onClick = { exercises = WorkoutLogic.moveItem(exercises, index, index + 1) },
                            enabled = index < exercises.lastIndex,
                        ) {
                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                contentDescription = "下移",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Box {
                            IconButton(onClick = { rowMenu = index }) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = "更多",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            DropdownMenu(expanded = rowMenu == index, onDismissRequest = { rowMenu = null }) {
                                DropdownMenuItem(
                                    text = { Text("编辑组数 / 次数 / 重量") },
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                    onClick = {
                                        rowMenu = null
                                        editingIndex = index
                                    },
                                )
                                if (e.sets >= 2) {
                                    DropdownMenuItem(
                                        text = { Text("拆成两段（组数不变）") },
                                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                        onClick = {
                                            rowMenu = null
                                            splitIndex = index
                                        },
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("删除动作") },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                                    onClick = {
                                        rowMenu = null
                                        exercises = exercises.filterIndexed { i, _ -> i != index }
                                    },
                                )
                            }
                        }
                    }
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
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("动作名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(6.dp))
                Row {
                    OutlinedTextField(
                        value = setsText,
                        onValueChange = { setsText = it.filter(Char::isDigit).take(2) },
                        label = { Text("组数") },
                        singleLine = true,
                        modifier = Modifier.width(88.dp),
                    )
                    Spacer(Modifier.width(6.dp))
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
                        modifier = Modifier.width(96.dp),
                    )
                }
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = {
                        val n = name.trim()
                        if (n.isNotEmpty()) {
                            exercises = exercises + Exercise(
                                name = n,
                                sets = setsText.toIntOrNull()?.coerceIn(1, 20) ?: 3,
                                reps = reps.trim().ifEmpty { "12" },
                                weight = weightText.toDoubleOrNull()?.coerceIn(0.0, 1000.0) ?: 0.0,
                            )
                            name = ""
                            setsText = "3"
                            reps = "12"
                            weightText = ""
                        }
                    },
                    enabled = name.isNotBlank(),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("添加动作")
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
                        )
                    )
                },
                enabled = exercises.isNotEmpty(),
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )

    editingIndex?.let { index ->
        val target = exercises.getOrNull(index)
        if (target != null) {
            ExerciseEditorDialog(
                initial = target,
                onDismiss = { editingIndex = null },
                onSave = { updated ->
                    exercises = exercises.toMutableList().also { it[index] = updated }
                    editingIndex = null
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
internal fun ExerciseEditorDialog(
    initial: Exercise?,
    onDismiss: () -> Unit,
    onSave: (Exercise) -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var setsText by remember { mutableStateOf((initial?.sets ?: 3).toString()) }
    var reps by remember { mutableStateOf(initial?.reps ?: "12") }
    var weightText by remember {
        mutableStateOf(
            (initial?.weight ?: 0.0).let { w ->
                if (w > 0.0) {
                    if (w == w.toLong().toDouble()) w.toLong().toString() else w.toString()
                } else {
                    ""
                }
            }
        )
    }
    var color by remember { mutableStateOf(initial?.color ?: 0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "加一个动作" else "改动作") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("动作名称（如 哑铃卧推）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Row {
                    OutlinedTextField(
                        value = setsText,
                        onValueChange = { setsText = it.filter(Char::isDigit).take(2) },
                        label = { Text("组数") },
                        singleLine = true,
                        modifier = Modifier.width(88.dp),
                    )
                    Spacer(Modifier.width(6.dp))
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
                        modifier = Modifier.width(96.dp),
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "重量可以留空（自重 / 有氧动作）；训练时还能单独改今天的重量，不影响计划。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "备选组：同一组的动作今天只做一个（选「不分组」就和别的动作互不影响）",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    FilterChip(
                        selected = color == 0,
                        onClick = { color = 0 },
                        label = { Text("不分组", fontSize = 12.sp) },
                    )
                    (1..6).forEach { index ->
                        FilterChip(
                            selected = color == index,
                            onClick = { color = index },
                            label = { Text(variantMark(index - 1), fontSize = 13.sp) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    onSave(
                        Exercise(
                            name = name.trim(),
                            sets = setsText.toIntOrNull()?.coerceIn(1, 20) ?: 3,
                            reps = reps.trim().ifEmpty { "12" },
                            weight = weightText.toDoubleOrNull()?.coerceIn(0.0, 1000.0) ?: 0.0,
                            color = color,
                        )
                    )
                },
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

/** 单独改“今天这个动作多重”，不动计划。 */
@Composable
private fun WeightDialog(
    name: String,
    initial: Double,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit,
) {
    var text by remember {
        mutableStateOf(if (initial > 0.0) {
            if (initial == initial.toLong().toDouble()) initial.toLong().toString() else initial.toString()
        } else {
            ""
        })
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("「$name」今天的重量") },
        text = {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(20.0, 30.0, 40.0, 60.0).forEach { quick ->
                        OutlinedButton(onClick = { text = quick.toLong().toString() }) {
                            Text("${quick.toLong()}kg")
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.filter { c -> c.isDigit() || c == '.' }.take(6) },
                    label = { Text("重量（kg，留空 = 自重）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "只改今天这份清单，计划里的目标重量不变。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(text.toDoubleOrNull()?.coerceIn(0.0, 1000.0) ?: 0.0) },
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun PartManagerDialog(
    parts: List<String>,
    onDismiss: () -> Unit,
    onAdd: (String) -> Boolean,
    onRename: (String, String) -> Boolean,
    onRemove: (String) -> Unit,
) {
    var newName by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var renaming by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("管理部位") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("新部位（如 胸 / 臀 / 拉伸）") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (!onAdd(newName)) error = "已经有这个部位了" else error = ""
                            newName = ""
                        },
                        enabled = newName.isNotBlank(),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "新增部位")
                    }
                }
                if (error.isNotBlank()) {
                    Text(
                        error,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Spacer(Modifier.height(8.dp))
                parts.forEach { part ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(part, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        IconButton(onClick = { renaming = part }) {
                            Icon(Icons.Default.Edit, contentDescription = "改名")
                        }
                        IconButton(onClick = { onRemove(part) }) {
                            Icon(Icons.Default.Delete, contentDescription = "删除部位")
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("完成") } },
    )

    val renameTarget = renaming
    if (renameTarget != null) {
        var text by remember(renameTarget) { mutableStateOf(renameTarget) }
        AlertDialog(
            onDismissRequest = { renaming = null },
            title = { Text("给「$renameTarget」改名") },
            text = {
                Column {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        singleLine = true,
                        label = { Text("新名称") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = "改完这个名字，原来的计划也会跟着改名。",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = text.isNotBlank(),
                    onClick = {
                        if (onRename(renameTarget, text)) renaming = null
                    },
                ) { Text("改名") }
            },
            dismissButton = { TextButton(onClick = { renaming = null }) { Text("取消") } },
        )
    }
}

@Composable
private fun TemplateSaveDialog(
    defaultName: String,
    exerciseCount: Int,
    existingNames: List<String>,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var name by remember { mutableStateOf(defaultName) }
    var error by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("把今天的训练存成模板") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        error = ""
                    },
                    label = { Text("模板名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (error.isNotBlank()) {
                    Text(
                        error,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "包含 $exerciseCount 个动作。同名模板会被覆盖。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    val trimmed = name.trim()
                    if (existingNames.any { it.equals(trimmed, ignoreCase = true) }) {
                        error = "已经有同名模板了，换个名字或直接保存覆盖"
                    }
                    onSave(trimmed)
                },
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun TemplateImportDialog(
    templates: List<WorkoutTemplate>,
    onDismiss: () -> Unit,
    onPick: (WorkoutTemplate) -> Unit,
    onDelete: (WorkoutTemplate) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("导入模板") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                templates.forEach { template ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(template) },
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                template.name,
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                template.summaryText,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { onDelete(template) }) {
                            Icon(Icons.Default.Delete, contentDescription = "删除模板")
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}
