package com.deepseek.dailytasks.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.deepseek.dailytasks.data.ConsumableStore
import com.deepseek.dailytasks.data.StatMetric
import com.deepseek.dailytasks.data.StatSlice
import com.deepseek.dailytasks.data.buildStatSlices
import com.deepseek.dailytasks.data.Consumable
import java.util.Locale

@Composable
fun StatsScreen(store: ConsumableStore, onBack: () -> Unit) {
    val items = remember { store.loadConsumables() }
    var metric by remember { mutableStateOf(StatMetric.AMOUNT) }
    var selectedSlice by remember { mutableStateOf<StatSlice?>(null) }
    val slices = remember(metric, items) { buildStatSlices(items, metric) }
    val total = slices.sumOf { it.value }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
            }
            Column {
                Text(
                    text = "消耗品统计",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "按类型看花费和用量占比",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatMetric.entries.forEach { option ->
                FilterChip(
                    selected = metric == option,
                    onClick = { metric = option },
                    label = { Text(option.label) },
                )
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(top = 12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
        )

        if (slices.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "还没有可以统计的数据",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "先在「消耗品」页面记几件东西，填上类型和价格，这里就有饼图了。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Column
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            PieChart(
                slices = slices,
                total = total,
                centerLabel = metricValueText(total, metric),
                centerSub = "共 ${items.size} 条记录 · 点扇区看明细",
                onSliceClick = { selectedSlice = it },
            )

            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                Text(
                    text = "明细（点任意一行看具体记录）",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                slices.forEach { slice ->
                    SliceLegendRow(
                        slice = slice,
                        total = total,
                        metric = metric,
                        onClick = { selectedSlice = slice },
                    )
                    Spacer(Modifier.height(10.dp))
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "统计口径说明：金额 = 每条记录价格之和；数量 = 各自用量的直接相加（单位不同仅供参考）；" +
                        "次数 = 该类型下的记录条数。没有填价格的记录金额按 0 计算。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 24.dp),
                )
            }
        }
    }

    selectedSlice?.let { slice ->
        TypeRecordsDialog(
            slice = slice,
            items = items.filter { it.typeLabel == slice.label },
            metric = metric,
            onDismiss = { selectedSlice = null },
        )
    }
}

@Composable
private fun PieChart(
    slices: List<StatSlice>,
    total: Double,
    centerLabel: String,
    centerSub: String,
    onSliceClick: (StatSlice) -> Unit,
) {
    val holeColor = MaterialTheme.colorScheme.surface
    val textColor = MaterialTheme.colorScheme.onSurface
    val subColor = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(modifier = Modifier.size(240.dp), contentAlignment = Alignment.Center) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(slices, total) {
                        detectTapGestures { position ->
                            val centerX = size.width / 2f
                            val centerY = size.height / 2f
                            val dx = position.x - centerX
                            val dy = position.y - centerY
                            val distance = kotlin.math.sqrt(dx * dx + dy * dy)
                            val radius = kotlin.math.min(size.width, size.height) / 2f
                            if (distance <= radius * 0.60f) return@detectTapGestures
                            if (distance > radius) return@detectTapGestures
                            var angle = Math.toDegrees(kotlin.math.atan2(dy, dx).toDouble()).toFloat() + 90f
                            if (angle < 0f) angle += 360f
                            var start = 0f
                            slices.forEach { slice ->
                                val sweep = if (total <= 0.0) 0f else (slice.value / total * 360.0).toFloat()
                                if (angle >= start && angle < start + sweep) {
                                    onSliceClick(slice)
                                    return@detectTapGestures
                                }
                                start += sweep
                            }
                        }
                    }
            ) {
                var startAngle = -90f
                slices.forEach { slice ->
                    val sweep = if (total <= 0.0) 0f else (slice.value / total * 360.0).toFloat()
                    drawArc(
                        color = Color(slice.colorArgb),
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = true,
                        topLeft = Offset.Zero,
                        size = Size(size.width, size.height),
                    )
                    startAngle += sweep
                }
                drawCircle(
                    color = holeColor,
                    radius = size.minDimension * 0.30f,
                    center = center,
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = centerLabel,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                )
                Text(
                    text = centerSub,
                    style = MaterialTheme.typography.bodySmall,
                    color = subColor,
                )
            }
        }
    }
}

@Composable
private fun SliceLegendRow(slice: StatSlice, total: Double, metric: StatMetric, onClick: () -> Unit) {
    val percent = if (total <= 0.0) 0.0 else slice.value / total * 100.0
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(12.dp)
                .background(Color(slice.colorArgb), CircleShape)
        )
        Spacer(Modifier.size(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = slice.label,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = "${slice.count} 条记录",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = metricValueText(slice.value, metric),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.size(12.dp))
        Text(
            text = String.format(Locale.US, "%.0f%%", percent),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun metricValueText(value: Double, metric: StatMetric): String = when (metric) {
    StatMetric.AMOUNT -> "¥" + String.format(Locale.US, "%.2f", value)
    StatMetric.QUANTITY -> Consumable.trimNumber(value)
    StatMetric.COUNT -> Consumable.trimNumber(value) + " 次"
}

@Composable
private fun TypeRecordsDialog(
    slice: StatSlice,
    items: List<Consumable>,
    metric: StatMetric,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${slice.label} · ${slice.count} 条记录") },
        text = {
            Column(modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                Text(
                    text = "本类合计 ${metricValueText(slice.value, metric)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(8.dp))
                items.sortedByDescending { it.startDate }.forEach { item ->
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = if (item.isUsing) "在用" else "已用完",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (item.isUsing) Color(0xFF2FA84F) else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = item.dateRangeText,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = if (item.isUsing) "已用 ${item.daysUsed} 天" else "用了 ${item.daysUsed} 天",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "用量 ${item.quantityText} · 价格 ${item.priceText} · ${item.priceRecordText}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } },
    )
}