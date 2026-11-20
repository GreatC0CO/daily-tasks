package com.deepseek.dailytasks.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.deepseek.dailytasks.data.ConsumableStore
import com.deepseek.dailytasks.data.SavingsStore
import com.deepseek.dailytasks.data.TaskStore
import com.deepseek.dailytasks.data.WorkoutStore

/** 底部四个页面：今日清单 / 锻炼 / 消耗品 / 攒钱；消耗品页里再进饼图统计。 */
@Composable
fun AppRoot(taskStore: TaskStore) {
    val context = LocalContext.current
    val consumableStore = remember { ConsumableStore(context) }
    val workoutStore = remember { WorkoutStore(context) }
    val savingsStore = remember { SavingsStore(context) }
    var tab by remember { mutableStateOf(0) }
    var showStats by remember { mutableStateOf(false) }

    if (showStats) {
        StatsScreen(consumableStore, onBack = { showStats = false })
        return
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                    label = { Text("今日清单") },
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = { Icon(Icons.Default.Favorite, contentDescription = null) },
                    label = { Text("锻炼") },
                )
                NavigationBarItem(
                    selected = tab == 2,
                    onClick = { tab = 2 },
                    icon = { Icon(Icons.Default.ShoppingCart, contentDescription = null) },
                    label = { Text("消耗品") },
                )
                NavigationBarItem(
                    selected = tab == 3,
                    onClick = { tab = 3 },
                    icon = { Icon(Icons.Default.Star, contentDescription = null) },
                    label = { Text("攒钱") },
                )
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (tab) {
                0 -> TodayScreen(taskStore)
                1 -> WorkoutScreen(workoutStore)
                2 -> ConsumableScreen(consumableStore, onOpenStats = { showStats = true })
                else -> SavingsScreen(savingsStore)
            }
        }
    }
}
