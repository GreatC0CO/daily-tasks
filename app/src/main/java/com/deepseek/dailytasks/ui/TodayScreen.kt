package com.deepseek.dailytasks.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.deepseek.dailytasks.data.CalendarHelper
import com.deepseek.dailytasks.data.Category
import com.deepseek.dailytasks.data.SloganGroup
import com.deepseek.dailytasks.data.SloganLogic
import com.deepseek.dailytasks.data.Task
import com.deepseek.dailytasks.data.TaskStore
import com.deepseek.dailytasks.data.matchesQuery
import com.deepseek.dailytasks.data.sortTasksForDisplay
import com.deepseek.dailytasks.notify.ReminderScheduler
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("M月d日 EEE", Locale.CHINA)

/** 当前时段一组标语都没有时，标题退化显示的默认标语。 */
private const val FALLBACK_SLOGAN = "今天也只做最重要的几件事"

@Composable
fun TodayScreen(store: TaskStore) {
    val context = LocalContext.current
    var todayDate by remember { mutableStateOf(LocalDate.now()) }
    var viewDate by remember { mutableStateOf(LocalDate.now()) }
    var tickNow by remember { mutableLongStateOf(System.currentTimeMillis()) }

    val allTasks = remember { mutableStateListOf<Task>() }
    val categories = remember { mutableStateListOf<Category>() }
    var doneIds by remember { mutableStateOf<Set<Long>>(emptySet()) }

    /** null 表示"全部" */
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var editorOpen by remember { mutableStateOf(false) }
    var editorTask by remember { mutableStateOf<Task?>(null) }
    var deferTarget by remember { mutableStateOf<Task?>(null) }
    var categoryManagerOpen by remember { mutableStateOf(false) }
    var sloganGroups by remember { mutableStateOf(store.loadSloganGroups()) }
    var sloganEditorOpen by remember { mutableStateOf(false) }
    var sloganGroupsEditorOpen by remember { mutableStateOf(false) }
    var sloganManualOffset by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun reload(date: LocalDate) {
        allTasks.clear()
        allTasks.addAll(store.loadTasks())
        categories.clear()
        categories.addAll(store.loadCategories())
        doneIds = store.loadDone(date)
    }

    LaunchedEffect(viewDate) { reload(viewDate) }

    // 每 30 秒检查一次是否跨天，同时刷新"已超时"的高亮
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            tickNow = System.currentTimeMillis()
            val now = LocalDate.now()
            if (now != todayDate) {
                if (viewDate == todayDate) viewDate = now
                todayDate = now
            }
        }
    }

    // 标语按当前时段和轮换周期取；tickNow 每 30 秒变一次，所以徽标会自动换句
    val nowMinute = remember(tickNow) { SloganLogic.minuteOfDay(LocalTime.now()) }
    val activeSloganGroup = remember(sloganGroups, nowMinute) {
        SloganLogic.activeGroup(sloganGroups, nowMinute)
    }
    val shownSlogan = remember(activeSloganGroup, tickNow, sloganManualOffset) {
        SloganLogic.sloganAt(activeSloganGroup, tickNow / 60_000L, sloganManualOffset)
            ?: FALLBACK_SLOGAN
    }

    fun persist(list: List<Task>) {
        allTasks.clear()
        allTasks.addAll(list.sortedBy { it.startMinutes })
        store.saveTasks(allTasks)
        ReminderScheduler.rescheduleAll(context, allTasks)
    }

    fun toggle(task: Task, checked: Boolean) {
        if (viewDate.isAfter(todayDate)) {
            scope.launch { snackbarHostState.showSnackbar("未来的计划还没到日子，到那天再打勾") }
            return
        }
        val next = if (checked) doneIds + task.id else doneIds - task.id
        doneIds = next
        store.saveDone(viewDate, next)

        // 一次性任务勾完就记下"哪天完成的"，第二天自动从清单里消失
        if (task.repeatEveryDays <= 0) {
            val updated = task.copy(finishedEpochDay = if (checked) viewDate.toEpochDay() else null)
            persist(allTasks.map { if (it.id == task.id) updated else it })
        }
    }

    /** 推迟：只推迟这一次，或让整个重复周期一起顺延一天。 */
    fun deferTask(task: Task, shiftAll: Boolean) {
        val updated = if (shiftAll && task.repeatEveryDays > 1) {
            task.copy(anchorEpochDay = task.anchorEpochDay + 1)
        } else if (task.repeatEveryDays <= 0) {
            // 一次性任务本来就一直挂着，推迟只是"今天先不显示"
            task.copy(skipEpochDay = viewDate.toEpochDay())
        } else {
            task.copy(
                skipEpochDay = viewDate.toEpochDay(),
                showEpochDay = viewDate.plusDays(1).toEpochDay(),
            )
        }
        persist(allTasks.map { if (it.id == task.id) updated else it })
        scope.launch {
snackbarHostState.showSnackbar(if (shiftAll) "整个周期顺延一天" else "只推迟这一次，已挪到下一天")
        }
    }

    fun saveTask(
        original: Task?,
        title: String,
        minutes: Int,
        note: String,
        remind: Boolean,
        repeatEveryDays: Int,
        categoryId: String,
        priority: Int,
    ) {
        val list = allTasks.toMutableList()
        if (original == null) {
            list.add(
                Task(
                    id = System.currentTimeMillis(),
                    title = title,
                    startMinutes = minutes,
                    note = note,
                    remind = remind,
                    repeatEveryDays = repeatEveryDays,
                    anchorEpochDay = viewDate.toEpochDay(),
                    categoryId = categoryId,
                    priority = priority,
                )
            )
        } else {
            val index = list.indexOfFirst { it.id == original.id }
            val repeatChanged = original.repeatEveryDays != repeatEveryDays
            val updated = original.copy(
                title = title,
                startMinutes = minutes,
                note = note,
                remind = remind,
                repeatEveryDays = repeatEveryDays,
                anchorEpochDay = if (repeatChanged) viewDate.toEpochDay() else original.anchorEpochDay,
                categoryId = categoryId,
                finishedEpochDay = if (repeatEveryDays <= 0) original.finishedEpochDay else null,
                priority = priority,
            )
            if (index >= 0) list[index] = updated else list.add(updated)
        }
        persist(list)
    }

    fun deleteTask(task: Task) {
        ReminderScheduler.cancel(context, task)
        persist(allTasks.filterNot { it.id == task.id })
    }

    fun addCategory(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        val updated = categories + Category(
            id = "c" + System.currentTimeMillis(),
            name = trimmed,
            colorArgb = Category.nextColor(categories),
        )
        categories.clear()
        categories.addAll(updated)
        store.saveCategories(categories)
    }

    fun renameCategory(category: Category, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        val updated = categories.map { if (it.id == category.id) it.copy(name = trimmed) else it }
        categories.clear()
        categories.addAll(updated)
        store.saveCategories(categories)
    }

    fun deleteCategory(category: Category) {
        val updated = categories.filterNot { it.id == category.id }
        categories.clear()
        categories.addAll(updated)
        store.saveCategories(categories)
        if (selectedCategory == category.id) selectedCategory = null
        // 原来属于这个分类的任务变成"未分类"
        persist(allTasks.map { if (it.categoryId == category.id) it.copy(categoryId = "") else it })
    }

    val isToday = viewDate == todayDate
    val isFuture = viewDate.isAfter(todayDate)
    val nowMinutes = LocalTime.now().let { it.hour * 60 + it.minute }
    val categoryMap = categories.associateBy { it.id }
    val visibleTasks = sortTasksForDisplay(
        allTasks.filter {
            it.isVisibleOn(viewDate) && it.matchesQuery(query, categoryMap[it.categoryId]?.name)
        }
    )
    val shownTasks = if (selectedCategory == null) {
        visibleTasks
    } else {
        visibleTasks.filter { it.categoryId == selectedCategory }
    }
    val doneCount = shownTasks.count { it.id in doneIds }
    val total = shownTasks.size
    val fraction = if (total == 0) 0f else doneCount.toFloat() / total

    // 按必要级拆开统计，进度条会按「必要 / 重要 / 普通」分段上色
    val necessaryTasks = shownTasks.filter { it.priority == Task.PRIORITY_NECESSARY }
    val importantTasks = shownTasks.filter { it.priority == Task.PRIORITY_IMPORTANT }
    val normalTasks = shownTasks.filter { it.priority == Task.PRIORITY_NORMAL }
    val priorityDone = listOf(
        necessaryTasks.count { it.id in doneIds },
        importantTasks.count { it.id in doneIds },
        normalTasks.count { it.id in doneIds },
    )
    val priorityTotal = listOf(necessaryTasks.size, importantTasks.size, normalTasks.size)

    fun pickDate() {
        DatePickerDialog(
            context,
            { _, year, month, day -> viewDate = LocalDate.of(year, month + 1, day) },
            viewDate.year,
            viewDate.monthValue - 1,
            viewDate.dayOfMonth,
        ).show()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editorTask = null
                    editorOpen = true
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "新增任务")
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            SummaryHeader(
                viewDate = viewDate,
                isToday = isToday,
                isFuture = isFuture,
                doneCount = doneCount,
                total = total,
                fraction = fraction,
                priorityDone = priorityDone,
                priorityTotal = priorityTotal,
                categories = categories,
                selectedCategory = selectedCategory,
                slogan = shownSlogan,
                query = query,
                onQueryChange = { query = it },
                onSelectCategory = { selectedCategory = it },
                onManageCategories = { categoryManagerOpen = true },
                onEditSlogan = { sloganEditorOpen = true },
                onPrevDay = { viewDate = viewDate.minusDays(1) },
                onNextDay = { viewDate = viewDate.plusDays(1) },
                onPickDate = { pickDate() },
                onBackToToday = { viewDate = todayDate },
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

            if (shownTasks.isEmpty()) {
                EmptyHint(
                    filtered = selectedCategory != null,
                    searching = query.isNotBlank(),
                    future = isFuture,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(shownTasks, key = { it.id }) { task ->
                        TaskRow(
                            task = task,
                            category = categoryMap[task.categoryId],
                            done = task.id in doneIds,
                            overdue = isToday && task.id !in doneIds && task.startMinutes < nowMinutes,
checkable = !isFuture,
date = viewDate,
deferable = task.id !in doneIds && task.repeatEveryDays != 1 &&
task.priority < Task.PRIORITY_NECESSARY,
                            onToggle = { toggle(task, it) },
                            onDefer = { deferTarget = task },
                            onEdit = {
                                editorTask = task
                                editorOpen = true
                            },
                            onDelete = { deleteTask(task) },
                        )
                    }
                    item {
                        Text(
                            text = "点日期可以翻到过去看完成记录、翻到以后做计划；每天 0 点自动翻新。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 16.dp),
                        )
                    }
                }
            }
        }
    }

    if (editorOpen) {
        TaskEditorDialog(
            initial = editorTask,
            categories = categories,
            defaultCategoryId = selectedCategory ?: "",
            existingTitles = allTasks.filter { it.id != editorTask?.id }.map { it.title },
            onDismiss = { editorOpen = false },
            onSave = { title, minutes, note, remind, repeatEveryDays, categoryId, priority ->
                editorOpen = false
                saveTask(editorTask, title, minutes, note, remind, repeatEveryDays, categoryId, priority)
            },
        )
    }

    deferTarget?.let { task ->
        DeferDialog(
            task = task,
            onDismiss = { deferTarget = null },
            onConfirm = { shiftAll ->
                deferTask(task, shiftAll)
                deferTarget = null
            },
        )
    }

    if (categoryManagerOpen) {        CategoryManagerDialog(
            categories = categories,
            onDismiss = { categoryManagerOpen = false },
            onAdd = { addCategory(it) },
            onRename = { category, name -> renameCategory(category, name) },
            onDelete = { deleteCategory(it) },
        )
    }

    if (sloganEditorOpen) {
        SloganDialog(
            slogan = shownSlogan,
            group = activeSloganGroup,
            onNext = { sloganManualOffset++ },
            onEditGroups = {
                sloganEditorOpen = false
                sloganGroupsEditorOpen = true
            },
            onDismiss = { sloganEditorOpen = false },
        )
    }

    if (sloganGroupsEditorOpen) {
        SloganGroupsDialog(
            groups = sloganGroups,
            onDismiss = { sloganGroupsEditorOpen = false },
            onSave = { next ->
                sloganGroups = next
                store.saveSloganGroups(next)
            },
        )
    }
}

/** 标题右边的小标语：一句带引号的话，太长就省略，点一下打开编辑。 */
@Composable
private fun SloganBadge(
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
        Text(
            text = "「$text」",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

@Composable
private fun SloganDialog(
    slogan: String,
    group: SloganGroup?,
    onNext: () -> Unit,
    onEditGroups: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("小标语") },
        text = {
            Column {
                Text(
                    text = "「$slogan」",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (group == null) {
                        "当前没有匹配的时段，标题先显示默认那句。"
                    } else {
                        "当前时段 ${group.timeText} · 每 ${group.rotateText} 换一句"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = onNext,
                        enabled = (group?.slogans?.size ?: 0) > 1,
                    ) { Text("换一句") }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onEditGroups) { Text("编辑标语分组") }
                }
                Text(
                    text = "标语按手机当前时间自动切换；同一组在时段内按轮换周期依次显示。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        },
    )
}

/** 标语分组列表：看每一组的时段/周期/句数，可编辑或删除，也可以新增时段。 */
@Composable
private fun SloganGroupsDialog(
    groups: List<SloganGroup>,
    onDismiss: () -> Unit,
    onSave: (List<SloganGroup>) -> Unit,
) {
    var editing by remember { mutableStateOf<SloganGroup?>(null) }
    var adding by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("标语分组") },
        text = {
            Column {
                Text(
                    text = "按一天里的时段显示不同标语；时段重叠时，列表里靠前的先生效。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                if (groups.isEmpty()) {
                    Text(
                        text = "还没有任何时段，标题会显示默认那句。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        groups.forEach { group ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            ) {
                                Row(
                                    modifier = Modifier.padding(start = 12.dp, end = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            text = "${group.timeText} · 每 ${group.rotateText}",
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                        Text(
                                            text = if (group.slogans.isEmpty()) {
                                                "还没有句子（空组显示默认句）"
                                            } else {
                                                "${group.slogans.size} 句"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    IconButton(onClick = { editing = group }) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "编辑这个时段",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    IconButton(onClick = {
                                        onSave(groups.filterNot { it.id == group.id })
                                    }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "删除这个时段",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = { adding = true }) { Text("＋ 新增时段") }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("完成") }
        },
    )

    if (adding || editing != null) {
        SloganGroupEditorDialog(
            initial = editing,
            onDismiss = {
                adding = false
                editing = null
            },
            onSave = { group ->
                val next = if (editing == null) {
                    groups + group
                } else {
                    groups.map { if (it.id == group.id) group else it }
                }
                adding = false
                editing = null
                onSave(next)
            },
        )
    }
}

/** 单个时段：起止时间、轮换周期和这一组的句子。 */
@Composable
private fun SloganGroupEditorDialog(
    initial: SloganGroup?,
    onDismiss: () -> Unit,
    onSave: (SloganGroup) -> Unit,
) {
    val nowMinute = remember { SloganLogic.minuteOfDay(LocalTime.now()) }
    var startText by remember {
        mutableStateOf(
            initial?.let { SloganLogic.formatMinute(it.startMinute) }
                ?: SloganLogic.formatMinute(nowMinute)
        )
    }
    var endText by remember {
        mutableStateOf(
            initial?.let { SloganLogic.formatMinute(it.endMinute) }
                ?: SloganLogic.formatMinute((nowMinute + 120) % 1440)
        )
    }
    var rotateText by remember { mutableStateOf((initial?.rotateMinutes ?: 60).toString()) }
    val draft = remember {
        mutableStateListOf<String>().apply { addAll(initial?.slogans.orEmpty()) }
    }
    var newText by remember { mutableStateOf("") }

    val start = SloganLogic.parseMinute(startText)
    val end = SloganLogic.parseMinute(endText)
    val rotate = rotateText.trim().toIntOrNull()
    val valid = start != null && end != null && rotate != null && rotate in 1..1440

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "新增时段" else "编辑时段") },
        text = {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startText,
                        onValueChange = { startText = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("开始") },
                        placeholder = { Text("22:00") },
                        singleLine = true,
                        isError = startText.isNotBlank() && start == null,
                        shape = RoundedCornerShape(10.dp),
                    )
                    OutlinedTextField(
                        value = endText,
                        onValueChange = { endText = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("结束") },
                        placeholder = { Text("07:00") },
                        singleLine = true,
                        isError = endText.isNotBlank() && end == null,
                        shape = RoundedCornerShape(10.dp),
                    )
                }
                Text(
                    text = "结束早于开始表示跨零点；两个时间相同表示全天。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = rotateText,
                    onValueChange = { text ->
                        if (text.length <= 4 && text.all { it.isDigit() }) rotateText = text
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("轮换周期（分钟）") },
                    placeholder = { Text("30") },
                    singleLine = true,
                    isError = rotateText.isNotBlank() && (rotate == null || rotate !in 1..1440),
                    shape = RoundedCornerShape(10.dp),
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "这个时段的句子（${draft.size} 句）",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(4.dp))
                if (draft.isEmpty()) {
                    Text(
                        text = "还没有句子，空组会显示默认那句。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 160.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        draft.forEachIndexed { index, item ->
                            OutlinedTextField(
                                value = item,
                                onValueChange = { draft[index] = it },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                trailingIcon = {
                                    IconButton(onClick = { draft.removeAt(index) }) {
                                        Icon(Icons.Default.Clear, contentDescription = "删除这句")
                                    }
                                },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newText,
                        onValueChange = { newText = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        placeholder = { Text("加一句新标语") },
                    )
                    TextButton(
                        onClick = {
                            draft.add(newText.trim())
                            newText = ""
                        },
                        enabled = newText.isNotBlank(),
                    ) { Text("添加") }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = {
                    onSave(
                        SloganGroup(
                            id = initial?.id ?: System.currentTimeMillis(),
                            startMinute = start ?: 0,
                            endMinute = end ?: 0,
                            slogans = draft.map { it.trim() }
                                .filter { it.isNotEmpty() }
                                .distinct(),
                            rotateMinutes = rotate ?: 60,
                        )
                    )
                },
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun SummaryHeader(
    viewDate: LocalDate,
    isToday: Boolean,
    isFuture: Boolean,
    doneCount: Int,
    total: Int,
    fraction: Float,
    priorityDone: List<Int>,
    priorityTotal: List<Int>,
    categories: List<Category>,
    selectedCategory: String?,
    slogan: String,
    query: String,
    onQueryChange: (String) -> Unit,
    onSelectCategory: (String?) -> Unit,
    onManageCategories: () -> Unit,
    onEditSlogan: () -> Unit,
    onPrevDay: () -> Unit,
    onNextDay: () -> Unit,
    onPickDate: () -> Unit,
    onBackToToday: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(start = 20.dp, end = 12.dp, top = 24.dp, bottom = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "今日清单",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.width(10.dp))
                    SloganBadge(
                        text = slogan,
                        onClick = onEditSlogan,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = viewDate.format(dateFormatter),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onPickDate() }
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                    )
                }
            }
            IconButton(onClick = onManageCategories) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "管理分类",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrevDay) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "前一天")
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Text(
                    text = when {
                        isToday -> "今天"
                        isFuture -> "未来计划"
                        else -> "历史记录"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
            Spacer(Modifier.weight(1f))
            if (!isToday) {
                TextButton(onClick = onBackToToday) { Text("回到今天") }
            }
            IconButton(onClick = onNextDay) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "后一天")
            }
        }

        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = when {
                    total == 0 && isFuture -> "这天还没有安排"
                    total == 0 && !isToday -> "这天没有任务"
                    isFuture -> "已安排 $total 项"
                    isToday -> if (total == 0) "今天还没有安排" else "已完成 $doneCount / $total"
                    else -> "当天完成 $doneCount / $total"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            if (total > 0) {
                Text(
                    text = "${(fraction * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        PriorityProgressBar(
            done = priorityDone,
            total = priorityTotal,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
        )
        if (total > 0) {
            Spacer(Modifier.height(6.dp))
            PriorityLegend(priorityDone = priorityDone, priorityTotal = priorityTotal)
        }

        Spacer(Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { onSelectCategory(null) },
                    label = { Text("全部") },
                )
            }
            items(categories, key = { it.id }) { category ->
                FilterChip(
                    selected = selectedCategory == category.id,
                    onClick = {
                        onSelectCategory(if (selectedCategory == category.id) null else category.id)
                    },
                    label = { Text(category.name) },
                    leadingIcon = {
                        Box(
                            Modifier
                                .size(8.dp)
                                .background(Color(category.colorArgb), CircleShape)
                        )
                    },
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            placeholder = { Text("搜索任务：名称 / 备注 / 分类") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "清空搜索")
                    }
                }
            },
        )
    }
}

@Composable
private fun EmptyHint(
    filtered: Boolean,
    searching: Boolean,
    future: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = when {
                searching -> "没有匹配的任务"
                filtered -> "这个分类在这天没有任务"
                future -> "这天还很空"
                else -> "还没有任务"
            },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = when {
                searching -> "换个关键词试试，或者清空搜索框"
                filtered -> "切换上面的分类看看，或者新建一条"
                future -> "点右下角 ＋ 给这天先安排一件事"
                else -> "点右下角的 ＋ 添加今天要做的第一件事"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DeferDialog(
    task: Task,
    onDismiss: () -> Unit,
    onConfirm: (shiftAll: Boolean) -> Unit,
) {
    var shiftAll by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("推迟「${task.title}」") },
        text = {
            Column {
                Text(
"这一天先不做，改到下一天。必要级最高的任务、每天都要做的任务不支持推迟。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = !shiftAll, onClick = { shiftAll = false })
Text("只推迟这一次（下一天补上）", style = MaterialTheme.typography.bodyMedium)
                }
                if (task.repeatEveryDays > 1) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = shiftAll, onClick = { shiftAll = true })
                        Text(
                            "之后的任务一起顺延（每${task.repeatEveryDays}天的周期整体后退一天）",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(shiftAll) }) { Text("推迟") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun TaskRow(
    task: Task,
    category: Category?,
    done: Boolean,
    overdue: Boolean,
checkable: Boolean,
date: LocalDate,
deferable: Boolean = false,
onToggle: (Boolean) -> Unit,
onDefer: () -> Unit = {},
onCalendarResult: (Boolean) -> Unit = {},
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
var menuOpen by remember { mutableStateOf(false) }
val context = LocalContext.current

Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (done) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (done) 0.dp else 1.dp),
    ) {
        Row(
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = done,
                onCheckedChange = onToggle,
                enabled = checkable,
            )

            Text(
                text = task.timeText,
                modifier = Modifier.width(56.dp),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleSmall,
                color = when {
                    done -> MaterialTheme.colorScheme.onSurfaceVariant
                    overdue -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.primary
                },
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (done) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (done) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )

                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(7.dp)
                            .background(
                                Color(category?.colorArgb ?: 0xFF9AA4B2L),
                                CircleShape,
                            )
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = category?.name ?: Category.UNCATEGORIZED_NAME,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "  ·  ${task.repeatLabel}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (task.priority > Task.PRIORITY_NORMAL) {
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = priorityColor(task.priority).copy(alpha = 0.14f),
                        ) {
                            Text(
                                text = task.priorityLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = priorityColor(task.priority),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                            )
                        }
                    }
                }

                if (task.note.isNotBlank()) {
                    Text(
                        text = task.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "编辑或删除",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    if (deferable) {
                        DropdownMenuItem(
                            text = { Text("推迟一天") },
                            leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                            onClick = {
                                menuOpen = false
                                onDefer()
                            },
                        )
                    }
DropdownMenuItem(
text = { Text("加到系统日历") },
leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
onClick = {
menuOpen = false
onCalendarResult(CalendarHelper.addEvent(context, task.title, date, task.startMinutes, task.note))
},
)
DropdownMenuItem(
text = { Text("编辑") },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onEdit()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("删除") },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onDelete()
                        },
                    )
                }
            }
        }
    }
}

/**
 * 按必要级分段的进度条：整条按各等级的任务数分配宽度，
 * 每段里按完成比例填充颜色（必要=红、重要=橙、普通=灰蓝），
 * 所以完成一个「必要」任务，进度条上就补上一截红色。
 */
@Composable
private fun PriorityProgressBar(
    done: List<Int>,
    total: List<Int>,
    modifier: Modifier = Modifier,
) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    val colors = listOf(
        priorityColor(Task.PRIORITY_NECESSARY),
        priorityColor(Task.PRIORITY_IMPORTANT),
        priorityColor(Task.PRIORITY_NORMAL),
    )
    Canvas(modifier) {
        val radius = size.height / 2f
        val corner = CornerRadius(radius, radius)
        drawRoundRect(color = track, cornerRadius = corner)
        val sum = total.sum()
        if (sum <= 0) return@Canvas
        var x = 0f
        total.forEachIndexed { index, count ->
            if (count <= 0) return@forEachIndexed
            val band = size.width * count / sum
            val doneCount = done.getOrElse(index) { 0 }.coerceIn(0, count)
            val filled = band * doneCount.toFloat() / count
            if (filled > 0f) {
                drawRoundRect(
                    color = colors[index],
                    topLeft = Offset(x, 0f),
                    size = Size(filled, size.height),
                    cornerRadius = corner,
                )
            }
            x += band
        }
    }
}

/** 进度条下面的小图例：必要 2/3 · 重要 1/2 · 普通 3/5（没有任务的等级不显示）。 */
@Composable
private fun PriorityLegend(priorityDone: List<Int>, priorityTotal: List<Int>) {
    val labels = listOf("必要", "重要", "普通")
    val colors = listOf(
        priorityColor(Task.PRIORITY_NECESSARY),
        priorityColor(Task.PRIORITY_IMPORTANT),
        priorityColor(Task.PRIORITY_NORMAL),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        labels.forEachIndexed { index, label ->
            val count = priorityTotal.getOrElse(index) { 0 }
            if (count <= 0) return@forEachIndexed
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(colors[index]),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "$label ${priorityDone.getOrElse(index) { 0 }}/$count",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
