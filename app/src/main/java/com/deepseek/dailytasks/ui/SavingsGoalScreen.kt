package com.deepseek.dailytasks.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepseek.dailytasks.data.SavingContribution
import com.deepseek.dailytasks.data.SavingGoal
import com.deepseek.dailytasks.data.SavingTask
import com.deepseek.dailytasks.data.SavingTaskKind
import com.deepseek.dailytasks.data.Savings
import com.deepseek.dailytasks.data.moneyText
import java.time.LocalDate
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/** 一个攒钱目标的专属页面：存钱罐按百分比灌满 + 攒钱任务 + 记录。 */
@Composable
fun SavingsGoalScreen(
    goal: SavingGoal,
    onBack: () -> Unit,
    onUpdate: (SavingGoal) -> Unit,
    onDelete: () -> Unit,
) {
    val today = remember { LocalDate.now() }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var menuOpen by remember { mutableStateOf(false) }
    var taskEditor by remember { mutableStateOf<SavingTask?>(null) }
    var taskAdding by remember { mutableStateOf(false) }
    var editGoal by remember { mutableStateOf(false) }
    var deleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 4.dp, end = 8.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        text = goal.name,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        maxLines = 1,
                    )
                    Text(
                        text = if (goal.isComplete) "已经攒满，可以买啦" else "还差 ${goal.remainingText}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "更多")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("编辑目标") },
                            onClick = {
                                menuOpen = false
                                editGoal = true
                            },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        )
                        DropdownMenuItem(
                            text = { Text("删除目标") },
                            onClick = {
                                menuOpen = false
                                deleteConfirm = true
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            PiggyBank(
                progress = goal.progress,
                percentText = goal.percentText,
                savedText = goal.savedText,
                targetText = goal.targetText,
                complete = goal.isComplete,
            )

            Column(Modifier.padding(horizontal = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    MiniStat("已攒", goal.savedText, Modifier.weight(1f))
                    MiniStat("目标", goal.targetText, Modifier.weight(1f))
                    MiniStat("还差", goal.remainingText, Modifier.weight(1f))
                }

                if (goal.note.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = goal.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("攒钱任务", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Spacer(Modifier.weight(1f))
                    val claimable = Savings.claimableToday(goal, today)
                    if (claimable > 0) {
                        Text(
                            text = "今天还能攒 ${moneyText(claimable)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    TextButton(onClick = { taskAdding = true }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(2.dp))
                        Text("加任务", fontSize = 13.sp)
                    }
                }

                if (goal.tasks.isEmpty()) {
                    Text(
                        text = "还没有攒钱任务。加一个，比如「早起 +5 元」「少喝一杯奶茶 +15 元」，每天/每次完成就能往存钱罐里丢钱。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                goal.tasks.forEach { task ->
                    TaskRow(
                        task = task,
                        canClaim = Savings.canClaim(goal, task, today),
                        claimedToday = Savings.claimedToday(goal, task, today),
                        claimedTotal = Savings.claimedTotal(goal, task),
                        onClaim = {
                            val next = Savings.claim(goal, task, today)
                            onUpdate(next)
                            scope.launch { snackbar.showSnackbar("攒到 ${task.amountText}，共 ${next.savedText}") }
                        },
                        onEdit = { taskEditor = task },
                        onDelete = {
                            onUpdate(goal.copy(tasks = goal.tasks - task))
                        },
                    )
                }

                if (goal.contributions.isNotEmpty()) {
                    Spacer(Modifier.height(20.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("攒钱记录", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = "共 ${goal.contributions.size} 次 · ${goal.savedText}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Savings.sortedContributions(goal).take(30).forEach { contribution ->
                        ContributionRow(
                            contribution = contribution,
                            onUndo = {
                                onUpdate(Savings.undo(goal, contribution))
                                scope.launch { snackbar.showSnackbar("已撤销 ${contribution.amountText}") }
                            },
                        )
                    }
                }
            }
        }
    }

    if (taskAdding || taskEditor != null) {
        SavingTaskDialog(
            initial = taskEditor,
            onDismiss = {
                taskAdding = false
                taskEditor = null
            },
            onSave = { task ->
                val nextTasks = if (taskEditor == null) goal.tasks + task else goal.tasks.map {
                    if (it.id == task.id) task else it
                }
                onUpdate(goal.copy(tasks = nextTasks))
                taskAdding = false
                taskEditor = null
            },
        )
    }

    if (editGoal) {
        GoalEditorDialog(
            initial = goal,
            onDismiss = { editGoal = false },
            onSave = { name, target, note ->
                onUpdate(goal.copy(name = name, targetAmount = target, note = note))
                editGoal = false
            },
        )
    }

    if (deleteConfirm) {
        AlertDialog(
            onDismissRequest = { deleteConfirm = false },
            title = { Text("删除这个目标？") },
            text = { Text("「${goal.name}」以及它的 ${goal.contributions.size} 条攒钱记录都会删掉。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteConfirm = false
                        onDelete()
                    },
                ) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { deleteConfirm = false }) { Text("取消") } },
        )
    }
}

/** 画一枚扁平的金币：深色币缘 + 金色币面 + 一点高光。 */
private fun DrawScope.drawCoin(
    cx: Float,
    cy: Float,
    rx: Float,
    ry: Float,
    edge: Color,
    body: Color,
    shine: Color,
) {
    drawOval(edge, topLeft = Offset(cx - rx, cy - ry + ry * 0.30f), size = Size(rx * 2f, ry * 2f))
    drawOval(body, topLeft = Offset(cx - rx, cy - ry), size = Size(rx * 2f, ry * 2f))
    drawOval(shine, topLeft = Offset(cx - rx * 0.55f, cy - ry * 0.95f), size = Size(rx * 0.9f, ry * 0.5f))
}

/**
 * 存钱罐：金币按金额百分比往上铺，攒到 100% 时刚好填到投币口下方。
 * 罐内用交错排列的金币做出纹理，上沿再补一排半露的金币，避免出现生硬的水平切线。
 */
@Composable
private fun PiggyBank(
    progress: Float,
    percentText: String,
    savedText: String,
    targetText: String,
    complete: Boolean,
) {
    val coinTarget = if (progress <= 0f) 0 else (progress.coerceIn(0f, 1f) * 100f).roundToInt().coerceIn(1, 100)
    // 液面直接跟完成度走：progress 是多少，金币就铺到罐子的多少高度。
    val fill by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "coinFill",
    )
    val jarStroke = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
    val slotColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
    val coinEdge = Color(0xFFB07A10)
    val coinBody = Color(0xFFFFC94D)
    val coinShine = Color(0xFFFFE7A3)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.15f),
        ) {
            val w = size.width
            val h = size.height
            val bodyLeft = w * 0.12f
            val bodyTop = h * 0.20f
            val bodyRight = w * 0.88f
            val bodyBottom = h * 0.95f
            val bodyWidth = bodyRight - bodyLeft
            val bodyHeight = bodyBottom - bodyTop
            val radius = bodyWidth * 0.22f

            // 罐子形状：上窄下宽的圆角罐
            val jarPath = Path().apply {
                moveTo(bodyLeft + bodyWidth * 0.18f, bodyTop)
                lineTo(bodyRight - bodyWidth * 0.18f, bodyTop)
                cubicTo(
                    bodyRight - bodyWidth * 0.06f, bodyTop,
                    bodyRight, bodyTop + bodyHeight * 0.18f,
                    bodyRight, bodyTop + bodyHeight * 0.30f,
                )
                lineTo(bodyRight, bodyBottom - radius)
                quadraticTo(bodyRight, bodyBottom, bodyRight - radius, bodyBottom)
                lineTo(bodyLeft + radius, bodyBottom)
                quadraticTo(bodyLeft, bodyBottom, bodyLeft, bodyBottom - radius)
                lineTo(bodyLeft, bodyTop + bodyHeight * 0.30f)
                cubicTo(
                    bodyLeft, bodyTop + bodyHeight * 0.18f,
                    bodyLeft + bodyWidth * 0.06f, bodyTop,
                    bodyLeft + bodyWidth * 0.18f, bodyTop,
                )
                close()
            }

            // 罐子底的玻璃阴影
            drawRoundRect(
                color = jarStroke.copy(alpha = 0.10f),
                topLeft = Offset(bodyLeft, bodyTop),
                size = androidx.compose.ui.geometry.Size(bodyWidth, bodyHeight),
                cornerRadius = CornerRadius(radius, radius),
            )

            // 金币铺满：金币之间的间距/大小固定，液面高度由完成度决定
            val coinSpacing = bodyWidth * 0.125f
            val coinRx = coinSpacing * 0.46f
            val coinRy = coinSpacing * 0.23f
            val rowStep = coinRy * 1.55f
            val baseY = bodyBottom - coinSpacing * 0.28f
            val topY = bodyTop + bodyHeight * 0.09f
            val levelY = baseY - (baseY - topY) * fill
            clipPath(jarPath) {
                if (fill > 0.001f) {
                    // 先用金币底色把液面以下铺满，保证 100% 时罐子是满的
                    drawRect(
                        color = coinBody,
                        topLeft = Offset(bodyLeft, levelY),
                        size = Size(bodyWidth, bodyBottom - levelY),
                    )
                    // 交错的硬币纹理，从罐底一直铺到液面
                    var row = 0
                    var y = baseY
                    while (y > levelY - coinRy * 0.2f) {
                        val shift = if (row % 2 == 0) 0f else coinSpacing * 0.5f
                        var x = bodyLeft + coinSpacing * 0.25f + shift
                        while (x < bodyRight + coinSpacing * 0.5f) {
                            drawCoin(x, y, coinRx, coinRy, coinEdge, coinBody, coinShine)
                            x += coinSpacing
                        }
                        y -= rowStep
                        row++
                    }
                    // 液面补一排半露的金币，上沿看起来是硬币堆而不是一条直线
                    var tx = bodyLeft + coinSpacing * 0.25f
                    var i = 0
                    while (tx < bodyRight + coinSpacing * 0.5f) {
                        val cy = levelY - coinRy * (if (i % 2 == 0) 0.25f else 0.7f)
                        drawCoin(tx, cy, coinRx, coinRy, coinEdge, coinBody, coinShine)
                        tx += coinSpacing
                        i++
                    }
                }
            }

            // 罐子高光与描边
            drawRoundRect(
                color = Color.White.copy(alpha = 0.35f),
                topLeft = Offset(bodyLeft + bodyWidth * 0.08f, bodyTop + bodyHeight * 0.10f),
                size = androidx.compose.ui.geometry.Size(bodyWidth * 0.08f, bodyHeight * 0.55f),
                cornerRadius = CornerRadius(bodyWidth * 0.05f, bodyWidth * 0.05f),
            )
            drawPath(
                path = jarPath,
                color = jarStroke,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f),
            )

            // 投币口
            drawRoundRect(
                color = slotColor,
                topLeft = Offset(w * 0.38f, bodyTop - h * 0.012f),
                size = androidx.compose.ui.geometry.Size(w * 0.24f, h * 0.026f),
                cornerRadius = CornerRadius(h * 0.013f, h * 0.013f),
            )

            // 刻度线（右侧 25/50/75）
            listOf(0.25f, 0.5f, 0.75f).forEach { tick ->
                val y = bodyTop + bodyHeight * (1f - tick)
                drawLine(
                    color = jarStroke.copy(alpha = 0.35f),
                    start = Offset(bodyRight - bodyWidth * 0.10f, y),
                    end = Offset(bodyRight - bodyWidth * 0.03f, y),
                    strokeWidth = 2f,
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (complete) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFB300),
                    modifier = Modifier.size(28.dp),
                )
                Spacer(Modifier.height(2.dp))
            }
            Text(
                text = percentText,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "$savedText / $targetText",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "金币 $coinTarget / 100",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF8A5A00),
            )
            if (complete) {
                Spacer(Modifier.height(4.dp))
                Surface(
                    color = Color(0xFFFFB300).copy(alpha = 0.18f),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text(
                        text = "攒满啦 🎉",
                        fontSize = 12.sp,
                        color = Color(0xFF8A5A00),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }
    }
}

@Composable
private fun TaskRow(
    task: SavingTask,
    canClaim: Boolean,
    claimedToday: Int,
    claimedTotal: Int,
    onClaim: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(task.name, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                Text(
                    text = "${task.amountText} · ${task.ruleText}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (task.hasTarget) {
                    val progress = (claimedTotal.toFloat() / task.targetCount).coerceIn(0f, 1f)
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(5.dp),
                        strokeCap = StrokeCap.Round,
                    )
                    Text(
                        text = "已完成 $claimedTotal/${task.targetCount} 次" +
                            if (progress >= 1f) " · 任务完成 🎉" else " · 还差 ${task.targetCount - claimedTotal} 次",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else if (task.limited) {
                    Text(
                        text = "今天已领 $claimedToday/${task.maxPerDay} 次",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            if (canClaim) {
                Button(
                    onClick = onClaim,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text("完成 +${task.amountText}", fontSize = 12.sp)
                }
            } else {
                AssistChip(
                    onClick = {},
                    enabled = false,
                    label = {
                        Text(
                            if (task.hasTarget && claimedTotal >= task.targetCount) "任务已完成" else "今天已领满",
                            fontSize = 11.sp,
                        )
                    },
                )
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "更多")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("编辑任务") },
                        onClick = {
                            menuOpen = false
                            onEdit()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("删除任务") },
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

@Composable
private fun ContributionRow(contribution: SavingContribution, onUndo: () -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(contribution.taskName, fontSize = 14.sp)
            Text(
                contribution.dateText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = "+" + contribution.amountText,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
        )
        Box {
            IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.MoreVert, contentDescription = "撤销", modifier = Modifier.size(16.dp))
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("撤销这笔") },
                    onClick = {
                        menuOpen = false
                        onUndo()
                    },
                )
            }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
}

/** 新增 / 编辑攒钱任务。 */
@Composable
internal fun SavingTaskDialog(
    initial: SavingTask?,
    onDismiss: () -> Unit,
    onSave: (SavingTask) -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var amountText by remember {
        mutableStateOf(initial?.amount?.let { if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString() } ?: "")
    }
    var kind by remember { mutableStateOf(initial?.kind ?: SavingTaskKind.DAILY) }
    var targetText by remember {
        mutableStateOf(initial?.targetCount?.takeIf { it > 0 }?.toString() ?: "")
    }
    var maxText by remember {
        mutableStateOf(
            initial?.maxPerDay?.takeIf { it > 0 && initial.kind == SavingTaskKind.DAILY }?.toString() ?: "",
        )
    }
    val amount = amountText.trim().toDoubleOrNull() ?: 0.0
    val targetCount = targetText.trim().toIntOrNull()?.coerceIn(1, 9999) ?: 0
    val maxPerDay = maxText.trim().toIntOrNull()?.coerceIn(1, 99) ?: 0
    val valid = name.isNotBlank() && amount > 0.0 &&
        (kind == SavingTaskKind.DAILY || targetCount > 0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "加一个攒钱任务" else "编辑攒钱任务") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(24) },
                    label = { Text("任务名称") },
                    placeholder = { Text("如：跑步 / 少点一次外卖") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' }.take(9) },
                    label = { Text("完成一次攒多少") },
                    prefix = { Text("¥") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                Text("任务类型", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = kind == SavingTaskKind.MILESTONE,
                        onClick = { kind = SavingTaskKind.MILESTONE },
                        label = { Text("里程碑") },
                    )
                    FilterChip(
                        selected = kind == SavingTaskKind.DAILY,
                        onClick = { kind = SavingTaskKind.DAILY },
                        label = { Text("日常积累") },
                    )
                }
                Text(
                    text = kind.hint,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (kind == SavingTaskKind.MILESTONE) {
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = targetText,
                        onValueChange = { targetText = it.filter { c -> c.isDigit() }.take(4) },
                        label = { Text("总共要做几次") },
                        placeholder = { Text("如 30") },
                        supportingText = { Text("做满这些次数就算完成，之后不能再领；总计金额 = 次数 × 每次攒的钱") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = maxText,
                        onValueChange = { maxText = it.filter { c -> c.isDigit() }.take(2) },
                        label = { Text("每天最多领几次") },
                        placeholder = { Text("留空 = 不限次数") },
                        supportingText = { Text("比如每天最多 3 次；领满后按钮变成「今天已领满」，第二天自动恢复") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        SavingTask(
                            id = initial?.id ?: System.currentTimeMillis(),
                            name = name.trim(),
                            amount = amount,
                            kind = kind,
                            targetCount = if (kind == SavingTaskKind.MILESTONE) targetCount else 0,
                            maxPerDay = if (kind == SavingTaskKind.DAILY) maxPerDay else 0,
                        ),
                    )
                },
                enabled = valid,
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

/** 新建 / 编辑攒钱目标（想买的东西 + 金额）。 */
@Composable
internal fun GoalEditorDialog(
    initial: SavingGoal?,
    onDismiss: () -> Unit,
    onSave: (name: String, target: Double, note: String) -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var targetText by remember {
        mutableStateOf(
            initial?.targetAmount?.let {
                if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString()
            } ?: "",
        )
    }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    val target = targetText.trim().toDoubleOrNull() ?: 0.0
    val valid = name.isNotBlank() && target > 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "新的攒钱目标" else "编辑目标") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(30) },
                    label = { Text("想买什么") },
                    placeholder = { Text("如：机械键盘 / 相机") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = targetText,
                    onValueChange = { targetText = it.filter { c -> c.isDigit() || c == '.' }.take(9) },
                    label = { Text("目标金额") },
                    prefix = { Text("¥") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(120) },
                    label = { Text("备注（可选）") },
                    placeholder = { Text("比如型号、颜色、在哪买") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name.trim(), target, note.trim()) },
                enabled = valid,
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}


