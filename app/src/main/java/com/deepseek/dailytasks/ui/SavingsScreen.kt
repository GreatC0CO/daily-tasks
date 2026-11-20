package com.deepseek.dailytasks.ui

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepseek.dailytasks.data.BudgetItem
import com.deepseek.dailytasks.data.SavingGoal
import com.deepseek.dailytasks.data.Savings
import com.deepseek.dailytasks.data.SavingsStore
import com.deepseek.dailytasks.data.moneyText
import java.time.LocalDate

/** 攒钱 tab：顶部切换「攒钱目标 / 本月必要开支」。 */
@Composable
fun SavingsScreen(store: SavingsStore) {
    val today = remember { LocalDate.now() }
    var goals by remember { mutableStateOf(Savings.sortedGoals(store.loadGoals())) }
    var budget by remember { mutableStateOf(store.loadBudget()) }
    var mode by remember { mutableStateOf(0) } // 0 目标，1 预留
    var showGoalId by remember { mutableStateOf<Long?>(null) }
    var creatingGoal by remember { mutableStateOf(false) }
    var editingBudget by remember { mutableStateOf<BudgetItem?>(null) }
    var addingBudget by remember { mutableStateOf(false) }

    fun persist(next: List<SavingGoal>): List<SavingGoal> {
        goals = Savings.sortedGoals(next)
        store.saveGoals(goals)
        return goals
    }

    fun persistBudget(next: List<BudgetItem>) {
        budget = next
        store.saveBudget(next)
    }

    val current = goals.firstOrNull { it.id == showGoalId }
    if (current != null) {
        SavingsGoalScreen(
            goal = current,
            onBack = { showGoalId = null },
            onUpdate = { updated ->
                persist(goals.map { if (it.id == updated.id) updated else it })
            },
            onDelete = {
                persist(goals.filterNot { it.id == current.id })
                showGoalId = null
            },
        )
        return
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (mode == 0) creatingGoal = true else addingBudget = true
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(if (mode == 0) "新建目标" else "添加开支") },
            )
        },
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("攒钱", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = mode == 0,
                    onClick = { mode = 0 },
                    label = { Text("攒钱目标") },
                )
                FilterChip(
                    selected = mode == 1,
                    onClick = { mode = 1 },
                    label = { Text("本月必要开支") },
                )
            }
            Spacer(Modifier.height(8.dp))

            if (mode == 0) {
                val totalTarget = goals.sumOf { it.targetAmount }
                val totalSaved = goals.sumOf { it.savedAmount }
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        Text(
                            text = "${goals.size} 个目标 · 已攒 ${moneyText(totalSaved)} / 共需 ${moneyText(totalTarget)}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        val progress = if (totalTarget > 0) (totalSaved / totalTarget).toFloat() else 0f
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { progress.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))

                if (goals.isEmpty()) {
                    Text(
                        text = "还没有攒钱目标。点右下角「新建目标」，写下想买的东西和价格，再给目标加几个攒钱任务，完成后就能往存钱罐里丢钱。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 96.dp, top = 4.dp),
                    ) {
                        items(goals, key = { it.id }) { goal ->
                            GoalCard(goal = goal, today = today, onClick = { showGoalId = goal.id })
                        }
                    }
                }
            } else {
                BudgetScreen(
                    items = budget,
                    today = today,
                    onToggleReserved = { item ->
                        val month = Savings.monthKey(today)
                        persistBudget(
                            budget.map {
                                if (it.id == item.id) {
                                    it.copy(reservedMonth = if (it.reservedMonth == month) "" else month)
                                } else {
                                    it
                                }
                            },
                        )
                    },
                    onEdit = { editingBudget = it },
                    onDelete = { item -> persistBudget(budget.filterNot { it.id == item.id }) },
                )
            }
        }
    }

    if (creatingGoal) {
        GoalEditorDialog(
            initial = null,
            onDismiss = { creatingGoal = false },
            onSave = { name, target, note ->
                val goal = SavingGoal(
                    id = System.currentTimeMillis(),
                    name = name,
                    targetAmount = target,
                    note = note,
                )
                persist(goals + goal)
                creatingGoal = false
                showGoalId = goal.id
            },
        )
    }

    if (addingBudget || editingBudget != null) {
        BudgetItemDialog(
            initial = editingBudget,
            onDismiss = {
                addingBudget = false
                editingBudget = null
            },
            onSave = { item ->
                if (editingBudget == null) {
                    persistBudget(budget + item)
                } else {
                    persistBudget(budget.map { if (it.id == item.id) item else it })
                }
                addingBudget = false
                editingBudget = null
            },
        )
    }
}

@Composable
private fun GoalCard(goal: SavingGoal, today: LocalDate, onClick: () -> Unit) {
    val claimable = Savings.claimableToday(goal, today)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = goal.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = goal.percentText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = if (goal.isComplete) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { goal.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${goal.savedText} / ${goal.targetText}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = if (goal.isComplete) "已达成 🎉" else "还差 ${goal.remainingText}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${goal.tasks.size} 个任务",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (claimable > 0) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "今天可攒 ${moneyText(claimable)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyHint(text: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 24.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(12.dp),
        ) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(6.dp))
                Text(text, fontSize = 12.sp)
            }
        }
    }
}
