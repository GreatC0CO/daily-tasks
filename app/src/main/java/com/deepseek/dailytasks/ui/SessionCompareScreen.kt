package com.deepseek.dailytasks.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.deepseek.dailytasks.data.CompareRow
import com.deepseek.dailytasks.data.WorkoutLogic
import com.deepseek.dailytasks.data.WorkoutSession
import com.deepseek.dailytasks.data.variantMark

/**
 * 两天的训练对比：按动作名对齐，左边第一天、右边第二天，右侧显示容量变化。
 */
@Composable
fun SessionCompareScreen(
    a: WorkoutSession,
    b: WorkoutSession,
    onBack: () -> Unit,
) {
    val rows = remember(a, b) { WorkoutLogic.compareSessions(a, b) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
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
                Column {
                    Text(
                        text = "两天计划对比",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "${a.dateText} ↔ ${b.dateText}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Column(Modifier.padding(12.dp)) {
                    DayLine("第一天", a)
                    Spacer(Modifier.height(8.dp))
                    DayLine("第二天", b)
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth()) {
                        Text(
                            "共 ${rows.size} 个动作",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            "按容量变化排序",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            rows.forEach { row ->
                CompareRowCard(row)
                Spacer(Modifier.height(8.dp))
            }

            Text(
                text = "说明：同名动作会合并成一行（不同重量一起统计）；「容量变化」= 第二天容量 − 第一天容量。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 12.dp),
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DayLine(label: String, session: WorkoutSession) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            shape = CircleShape,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = "${session.dateText} · ${session.partText} · 完成 ${session.totalDone}/${session.totalPlanned} 组",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun CompareRowCard(row: CompareRow) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (row.color != 0) {
                    Text(
                        text = variantMark(row.color - 1),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 6.dp),
                    )
                }
                Text(
                    text = row.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = row.deltaText,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (row.delta > 0.0) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CellColumn("A", row.a.cellText, Modifier.weight(1f))
                CellColumn("B", row.b.cellText, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CellColumn(tag: String, text: String, modifier: Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    ) {
        Column(Modifier.padding(8.dp)) {
            Text(tag, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(2.dp))
            Text(text, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** 选两次训练记录来做对比。 */
@Composable
fun SessionComparePickerDialog(
    sessions: List<WorkoutSession>,
    onDismiss: () -> Unit,
    onConfirm: (WorkoutSession, WorkoutSession) -> Unit,
) {
    var a by remember { mutableStateOf(sessions.getOrNull(0)) }
    var b by remember { mutableStateOf(sessions.getOrNull(1) ?: sessions.getOrNull(0)) }
    val same = a != null && b != null && a?.id == b?.id

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("对比两天的训练") },
        text = {
            Column {
                Text(
                    "选两条训练记录，按动作名对比两天的组数、次数、重量和容量。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                PickerField("第一天", a, sessions) { a = it }
                Spacer(Modifier.height(10.dp))
                PickerField("第二天", b, sessions) { b = it }
                if (same) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "两天选的是同一次记录，换一条再对比。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "提示：对比的是保存过的训练记录；只练过没保存的当天不会出现在列表里。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = a != null && b != null && !same,
                onClick = {
                    val first = a
                    val second = b
                    if (first != null && second != null) onConfirm(first, second)
                },
            ) { Text("开始对比") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun PickerField(
    label: String,
    selected: WorkoutSession?,
    sessions: List<WorkoutSession>,
    onSelect: (WorkoutSession) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = selected?.let { "${it.dateText} · ${it.partText}" } ?: "选择记录",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                )
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                sessions.forEach { session ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "${session.dateText} · ${session.partText} · 完成 ${session.totalDone}/${session.totalPlanned} 组",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        },
                        onClick = {
                            expanded = false
                            onSelect(session)
                        },
                    )
                }
            }
        }
    }
}
