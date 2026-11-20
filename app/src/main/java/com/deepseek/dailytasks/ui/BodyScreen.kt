package com.deepseek.dailytasks.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog as MaterialDatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.deepseek.dailytasks.data.BodyLogic
import com.deepseek.dailytasks.data.BodyMetric
import com.deepseek.dailytasks.data.BodyPoint
import com.deepseek.dailytasks.data.BodyRecord
import com.deepseek.dailytasks.data.BodyStore
import com.deepseek.dailytasks.data.WeeklyAverage
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale

/** 身体数据页：每日体重 / 三围表格 + 周平均 + 曲线。 */
@Composable
fun BodyScreen(store: BodyStore, onBack: () -> Unit) {
    val today = remember { LocalDate.now() }
    var records by remember { mutableStateOf(store.loadRecords()) }
    var tab by remember { mutableIntStateOf(0) }
    var editorOpen by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<BodyRecord?>(null) }
    var deleteTarget by remember { mutableStateOf<BodyRecord?>(null) }
    var showArm by remember { mutableStateOf(true) }
    var showWaist by remember { mutableStateOf(true) }
    var showChest by remember { mutableStateOf(true) }

    fun persist(next: List<BodyRecord>) {
        records = next.sortedBy { it.date }
        store.saveRecords(records)
    }

    Scaffold(
        topBar = { BodyTopBar(onBack = onBack) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    editing = null
                    editorOpen = true
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("记一条") },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = 96.dp),
        ) {
            BodySummary(records = records, today = today)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    label = { Text("表格") },
                )
                FilterChip(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    label = { Text("曲线") },
                )
            }
            Spacer(Modifier.height(6.dp))
            if (tab == 0) {
                BodyTableView(
                    records = records,
                    onEdit = { record ->
                        editing = record
                        editorOpen = true
                    },
                    onDelete = { deleteTarget = it },
                )
            } else {
                BodyChartView(
                    records = records,
                    showArm = showArm,
                    showWaist = showWaist,
                    showChest = showChest,
                    onToggleArm = { showArm = !showArm },
                    onToggleWaist = { showWaist = !showWaist },
                    onToggleChest = { showChest = !showChest },
                )
            }
        }
    }

    if (editorOpen) {
        BodyEditorDialog(
            initial = editing,
            defaultDate = today,
            onDismiss = {
                editorOpen = false
                editing = null
            },
            onSave = { record ->
                persist(BodyLogic.upsert(records, record))
                editorOpen = false
                editing = null
            },
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("删除这一天的记录？") },
            text = { Text("${target.dateText} 的体重 / 三围会被删掉，不能恢复。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        persist(BodyLogic.remove(records, target.date))
                        deleteTarget = null
                    },
                ) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun BodyTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .padding(start = 4.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
        }
        Text(
            text = "身体数据",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun BodySummary(records: List<BodyRecord>, today: LocalDate) {
    val latest = BodyLogic.latest(records)
    val weekAverage = BodyLogic.recentWeekAverage(records, today)
    val change = BodyLogic.weightChange(records)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (latest == null) {
                Text(
                    text = "还没有身体数据",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "点右下角「记一条」记下今天的体重；臂围 / 腰围 / 胸围可以留空，想量的时候再填。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                return@Column
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "最新体重",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = latest.weightKg?.let { BodyRecord.formatValue(it) } ?: "—",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = " kg",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = latest.dateText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "最近 7 天平均",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = weekAverage?.let { String.format(Locale.US, "%.1f", it) + " kg" } ?: "—",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "与上次相比 ${BodyLogic.changeText(change)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun BodyTableView(
    records: List<BodyRecord>,
    onEdit: (BodyRecord) -> Unit,
    onDelete: (BodyRecord) -> Unit,
) {
    val weekly = BodyLogic.weeklyAverages(records).reversed()
    if (weekly.isNotEmpty()) {
        SectionTitle("每周平均体重（周一为一周开始）")
        weekly.take(8).forEach { average -> WeeklyAverageRow(average) }
        Spacer(Modifier.height(12.dp))
    }
    SectionTitle("每日记录")
    if (records.isEmpty()) {
        Text(
            text = "还没有记录。体重每天都可以记，三围不必每天量。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    TableHeader()
    records.reversed().forEach { record ->
        BodyTableRow(record = record, onEdit = { onEdit(record) }, onDelete = { onDelete(record) })
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 6.dp),
    )
}

@Composable
private fun WeeklyAverageRow(average: WeeklyAverage) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = average.label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "${average.days} 天",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(52.dp),
            textAlign = TextAlign.End,
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = average.averageText + " kg",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(84.dp),
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun TableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HeaderCell("日期", Modifier.weight(1.15f), TextAlign.Start)
        HeaderCell("体重", Modifier.weight(1f))
        HeaderCell("臂围", Modifier.weight(1f))
        HeaderCell("腰围", Modifier.weight(1f))
        HeaderCell("胸围", Modifier.weight(1f))
        Spacer(Modifier.width(30.dp))
    }
}

@Composable
private fun HeaderCell(text: String, modifier: Modifier, align: TextAlign = TextAlign.End) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
        textAlign = align,
    )
}

@Composable
private fun BodyTableRow(record: BodyRecord, onEdit: () -> Unit, onDelete: () -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = record.dateText,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1.15f),
        )
        CellText(record.weightText, Modifier.weight(1f), emphasized = record.weightKg != null)
        CellText(record.armText, Modifier.weight(1f))
        CellText(record.waistText, Modifier.weight(1f))
        CellText(record.chestText, Modifier.weight(1f))
        Box {
            IconButton(onClick = { menuOpen = true }, modifier = Modifier.width(30.dp)) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "更多",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("编辑") },
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

@Composable
private fun CellText(text: String, modifier: Modifier, emphasized: Boolean = false) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = if (emphasized) FontWeight.SemiBold else FontWeight.Normal,
        textAlign = TextAlign.End,
        modifier = modifier,
    )
}

@Composable
private fun BodyChartView(
    records: List<BodyRecord>,
    showArm: Boolean,
    showWaist: Boolean,
    showChest: Boolean,
    onToggleArm: () -> Unit,
    onToggleWaist: () -> Unit,
    onToggleChest: () -> Unit,
) {
    val weightPoints = BodyLogic.points(records, BodyMetric.WEIGHT)
    val weekly = BodyLogic.weeklyAverages(records)
    val weeklyPoints = weekly.map { BodyPoint(it.weekStart, it.averageWeight) }

    SectionTitle("体重曲线（圆点为每日体重，粗线为周平均）")
    if (weightPoints.isEmpty()) {
        EmptyChartHint("还没有体重数据，记两条以上就能看到曲线。")
    } else {
        BodyChart(
            series = listOf(
                ChartSeries(points = weightPoints, color = MaterialTheme.colorScheme.primary, width = 1.8f),
                ChartSeries(points = weeklyPoints, color = MaterialTheme.colorScheme.tertiary, width = 3.2f, dots = false),
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp),
        )
        LegendRow(
            entries = listOf(
                "每日体重" to MaterialTheme.colorScheme.primary,
                "周平均" to MaterialTheme.colorScheme.tertiary,
            ),
        )
        if (weekly.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = weekly.joinToString("   ") { "${it.label.removeSuffix(" 起")} ${it.averageText}" },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    Spacer(Modifier.height(16.dp))
    SectionTitle("三围曲线（按需记录，没有数据的地方断开）")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = showArm, onClick = onToggleArm, label = { Text("臂围") })
        FilterChip(selected = showWaist, onClick = onToggleWaist, label = { Text("腰围") })
        FilterChip(selected = showChest, onClick = onToggleChest, label = { Text("胸围") })
    }
    Spacer(Modifier.height(8.dp))
    val armPoints = if (showArm) BodyLogic.points(records, BodyMetric.ARM) else emptyList()
    val waistPoints = if (showWaist) BodyLogic.points(records, BodyMetric.WAIST) else emptyList()
    val chestPoints = if (showChest) BodyLogic.points(records, BodyMetric.CHEST) else emptyList()
    if (armPoints.isEmpty() && waistPoints.isEmpty() && chestPoints.isEmpty()) {
        EmptyChartHint("还没有三围数据。编辑任意一天，填上臂围 / 腰围 / 胸围就能看到曲线。")
    } else {
        BodyChart(
            series = buildList {
                add(ChartSeries(armPoints, Color(0xFF2E6BE6), width = 2.2f))
                add(ChartSeries(waistPoints, Color(0xFFF0A020), width = 2.2f))
                add(ChartSeries(chestPoints, Color(0xFF2FA84F), width = 2.2f))
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp),
        )
        LegendRow(
            entries = buildList {
                if (showArm) add("臂围" to Color(0xFF2E6BE6))
                if (showWaist) add("腰围" to Color(0xFFF0A020))
                if (showChest) add("胸围" to Color(0xFF2FA84F))
            },
        )
    }
}

@Composable
private fun EmptyChartHint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 8.dp),
    )
}

data class ChartSeries(
    val points: List<BodyPoint>,
    val color: Color,
    val width: Float = 2f,
    val dots: Boolean = true,
)

/** 简易折线图：自动按所有点的最小 / 最大值定纵轴。 */
@Composable
private fun BodyChart(series: List<ChartSeries>, modifier: Modifier = Modifier) {
    val allPoints = series.flatMap { it.points }
    if (allPoints.isEmpty()) return
    val values = allPoints.map { it.value }
    var minValue = values.min()
    var maxValue = values.max()
    if (maxValue - minValue < 0.6) {
        // 数据几乎一样时给一点纵向余量，线才不会贴边
        minValue -= 0.3
        maxValue += 0.3
    } else {
        val margin = (maxValue - minValue) * 0.15
        minValue -= margin
        maxValue += margin
    }
    val dates = allPoints.map { it.date }.sorted()
    val firstDay = dates.first().toEpochDay().toFloat()
    val lastDay = dates.last().toEpochDay().toFloat()
    val daySpan = (lastDay - firstDay).takeIf { it > 0f } ?: 1f
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 4.dp, bottom = 4.dp, end = 4.dp, start = 34.dp),
        ) {
            for (i in 0..2) {
                val y = size.height * i / 2f
                drawLine(gridColor, Offset(0f, y), Offset(size.width, y), 1f)
            }
            series.forEach { line ->
                val offsets = line.points.map { point ->
                    val x = (point.date.toEpochDay().toFloat() - firstDay) / daySpan * size.width
                    val ratio = ((point.value - minValue) / (maxValue - minValue)).toFloat()
                    val y = size.height - ratio * size.height
                    Offset(x, y)
                }
                if (offsets.size >= 2) {
                    offsets.zipWithNext().forEach { (a, b) ->
                        drawLine(line.color, a, b, line.width, cap = StrokeCap.Round)
                    }
                }
                if (line.dots) {
                    offsets.forEach { offset -> drawCircle(line.color, radius = 3.2f, center = offset) }
                }
            }
        }
        Text(
            text = BodyRecord.formatValue(maxValue),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.TopStart),
        )
        Text(
            text = BodyRecord.formatValue(minValue),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.BottomStart),
        )
    }
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = dates.first().format(BodyRecord.WEEK_FMT),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = dates.last().format(BodyRecord.WEEK_FMT),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun LegendRow(entries: List<Pair<String, Color>>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        entries.forEach { (label, color) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = color, shape = RoundedCornerShape(50), modifier = Modifier.width(8.dp).height(8.dp)) {}
                Spacer(Modifier.width(4.dp))
                Text(text = label, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

/** 记一条 / 编辑某天的体重与三围。 */
@Composable
private fun BodyEditorDialog(
    initial: BodyRecord?,
    defaultDate: LocalDate,
    onDismiss: () -> Unit,
    onSave: (BodyRecord) -> Unit,
) {
    val context = LocalContext.current
    var date by remember { mutableStateOf(initial?.date ?: defaultDate) }
    var weightText by remember {
        mutableStateOf(initial?.weightKg?.let { BodyRecord.formatValue(it) } ?: "")
    }
    var armText by remember { mutableStateOf(initial?.armCm?.let { BodyRecord.formatValue(it) } ?: "") }
    var waistText by remember { mutableStateOf(initial?.waistCm?.let { BodyRecord.formatValue(it) } ?: "") }
    var chestText by remember { mutableStateOf(initial?.chestCm?.let { BodyRecord.formatValue(it) } ?: "") }

    val weight = weightText.toDoubleOrNull()
    val arm = armText.toDoubleOrNull()
    val waist = waistText.toDoubleOrNull()
    val chest = chestText.toDoubleOrNull()
    val valid = listOf(weight, arm, waist, chest).any { it != null && it > 0.0 }

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
        title = { Text(if (initial == null) "记录身体数据" else "编辑 ${initial.dateText}") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = ::pickDate)
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.width(20.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(text = "日期", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text(
                        text = date.format(BodyRecord.FMT),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                NumberField(
                    value = weightText,
                    onValueChange = { weightText = it },
                    label = "体重（kg）",
                    hint = "必填其一",
                )
                Text(
                    text = "下面三项按需填，不想量就留空。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                NumberField(value = armText, onValueChange = { armText = it }, label = "臂围（cm）")
                NumberField(value = waistText, onValueChange = { waistText = it }, label = "腰围（cm）")
                NumberField(value = chestText, onValueChange = { chestText = it }, label = "胸围（cm）")
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        BodyRecord(
                            date = date,
                            weightKg = weight?.takeIf { it > 0.0 },
                            armCm = arm?.takeIf { it > 0.0 },
                            waistCm = waist?.takeIf { it > 0.0 },
                            chestCm = chest?.takeIf { it > 0.0 },
                        ),
                    )
                },
                enabled = valid,
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun NumberField(value: String, onValueChange: (String) -> Unit, label: String, hint: String = "") {
    OutlinedTextField(
        value = value,
        onValueChange = { text -> onValueChange(text.filter { it.isDigit() || it == '.' }) },
        label = { Text(label) },
        placeholder = { Text("可留空") },
        supportingText = if (hint.isNotBlank()) {
            { Text(hint) }
        } else {
            null
        },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}
