package com.deepseek.dailytasks.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepseek.dailytasks.data.BudgetItem
import com.deepseek.dailytasks.data.Savings
import com.deepseek.dailytasks.data.moneyText
import java.time.LocalDate

/** 本月必要开支：列出这个月还要预留多少钱，勾选表示已预留（换月自动失效）。 */
@Composable
fun BudgetScreen(
    items: List<BudgetItem>,
    today: LocalDate,
    onToggleReserved: (BudgetItem) -> Unit,
    onEdit: (BudgetItem) -> Unit,
    onDelete: (BudgetItem) -> Unit,
) {
    val month = Savings.monthKey(today)
    val summary = Savings.budgetSummary(items, today)
    val daysLeft = Savings.daysLeftInMonth(today)
    val perDay = Savings.dailyReserve(summary, today)

    Column(Modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.10f),
            shape = RoundedCornerShape(14.dp),
        ) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = Savings.monthLabel(today),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "${summary.reservedCount}/${summary.count} 项已预留",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = summary.remainingText,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "还需预留",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { summary.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp),
                    strokeCap = StrokeCap.Round,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "本月合计 ${summary.totalText} · 已预留 ${summary.reservedText} · 还剩 $daysLeft 天" +
                        if (perDay > 0) "，平均每天再留 ${moneyText(perDay)}" else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        if (items.isEmpty()) {
            Text(
                text = "把每月固定的必要开支记在这里，比如房租、水电、话费、通勤。勾选就表示这个月已经预留好了；下个月会自动清空勾选，重新提醒。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(bottom = 96.dp),
            ) {
                items(items, key = { it.id }) { item ->
                    val reserved = item.isReserved(month)
                    var menuOpen by remember { mutableStateOf(false) }
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (reserved) {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                        ),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 6.dp, end = 2.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(checked = reserved, onCheckedChange = { onToggleReserved(item) })
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = item.name,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    textDecoration = if (reserved) TextDecoration.LineThrough else null,
                                    color = if (reserved) {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                )
                                if (item.isDecreasing) {
                                    Text(
                                        text = "${item.decreaseText} · 本月 ${item.amountText}" +
                                            if (item.isExhausted(today)) {
                                                " · 已用完"
                                            } else {
                                                item.nextDecreaseDate(today)?.let { next ->
                                                    " · 下次 ${next.monthValue}/${next.dayOfMonth}"
                                                } ?: ""
                                            },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                if (item.note.isNotBlank()) {
                                    Text(
                                        text = item.note,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (item.isDecreasing) item.remainingText(today) else item.amountText,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = if (reserved) {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    } else {
                                        MaterialTheme.colorScheme.tertiary
                                    },
                                )
                                if (item.isDecreasing) {
                                    Text(
                                        text = "今天还剩",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Box {
                                IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(34.dp)) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "更多", modifier = Modifier.size(18.dp))
                                }
                                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                    DropdownMenuItem(
                                        text = { Text("编辑") },
                                        onClick = {
                                            menuOpen = false
                                            onEdit(item)
                                        },
                                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("删除") },
                                        onClick = {
                                            menuOpen = false
                                            onDelete(item)
                                        },
                                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 新增 / 编辑一条必要开支。 */
@Composable
internal fun BudgetItemDialog(
    initial: BudgetItem?,
    onDismiss: () -> Unit,
    onSave: (BudgetItem) -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var amountText by remember {
        mutableStateOf(
            initial?.amount?.let { if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString() } ?: "",
        )
    }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var decreasing by remember { mutableStateOf(initial?.isDecreasing ?: false) }
    var everyDaysText by remember { mutableStateOf((initial?.decreaseEveryDays ?: 1).toString()) }
    var stepText by remember {
        mutableStateOf(
            initial?.decreaseAmount?.let {
                if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString()
            } ?: "",
        )
    }
    val amount = amountText.trim().toDoubleOrNull() ?: 0.0
    val step = stepText.trim().toDoubleOrNull() ?: 0.0
    val everyDays = everyDaysText.trim().toIntOrNull()?.coerceIn(1, 30) ?: 1
    val valid = name.isNotBlank() && amount > 0.0 && (!decreasing || step > 0.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "添加必要开支" else "编辑必要开支") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(24) },
                    label = { Text("开支名称") },
                    placeholder = { Text("如：房租 / 水电 / 话费 / 通勤") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' }.take(9) },
                    label = { Text("每月金额") },
                    prefix = { Text("¥") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(120) },
                    label = { Text("备注（可选）") },
                    placeholder = { Text("比如缴费日期、账单号") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("逐日 / 逐几日降低", style = MaterialTheme.typography.labelMedium)
                        Text(
                            text = "适合生活费、零食、油费这类越用越少的预算",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = decreasing, onCheckedChange = { decreasing = it })
                }
                if (decreasing) {
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = everyDaysText,
                            onValueChange = { everyDaysText = it.filter { c -> c.isDigit() }.take(2) },
                            label = { Text("每隔几天") },
                            suffix = { Text("天") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = stepText,
                            onValueChange = { stepText = it.filter { c -> c.isDigit() || c == '.' }.take(8) },
                            label = { Text("每次减少") },
                            prefix = { Text("¥") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Text(
                        text = "例：本月 ¥3000、每 1 天减 ¥100 → 1 号剩 ¥3000，15 号剩 ¥1600，扣到 0 为止；" +
                            "「每 3 天」就是第 1/4/7… 天各扣一次。从本月 1 号开始算。",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "每个月都要重新勾选「已预留」，换月会自动清空。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        BudgetItem(
                            id = initial?.id ?: System.currentTimeMillis(),
                            name = name.trim(),
                            amount = amount,
                            note = note.trim(),
                            reservedMonth = initial?.reservedMonth ?: "",
                            decreaseAmount = if (decreasing) step else 0.0,
                            decreaseEveryDays = everyDays,
                            decreaseStartEpochDay = initial?.decreaseStartEpochDay ?: 0L,
                        ),
                    )
                },
                enabled = valid,
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}
