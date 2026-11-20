package com.deepseek.dailytasks.ui

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.deepseek.dailytasks.data.Consumable
import com.deepseek.dailytasks.data.ConsumableStore
import com.deepseek.dailytasks.data.PricePoint
import java.util.Locale

@Composable
fun ConsumableScreen(store: ConsumableStore, onOpenStats: () -> Unit) {
    var items by remember { mutableStateOf(store.loadConsumables()) }
    var types by remember { mutableStateOf(store.loadTypes()) }
    var quickAdjust by remember { mutableStateOf<Consumable?>(null) }
    var editorOpen by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Consumable?>(null) }
    var query by remember { mutableStateOf("") }
    var viewMode by remember { mutableStateOf(store.loadViewMode()) }
    var typeDetail by remember { mutableStateOf<String?>(null) }
    var priceTarget by remember { mutableStateOf<Consumable?>(null) }
    var purchaseOpen by remember { mutableStateOf(false) }

    fun persist(list: List<Consumable>) {
        val sorted = list.sortedWith(
            compareByDescending<Consumable> { it.isUsing }.thenByDescending { it.startDate }
        )
        items = sorted
        store.saveConsumables(sorted)
        types = store.loadTypes()
    }

    val usingCount = items.count { it.isUsing }
    val totalSpend = items.sumOf { it.totalSpent }
    val shown = items.filter { it.matchesQuery(query) }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { purchaseOpen = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("记一笔") },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(start = 20.dp, end = 12.dp, top = 24.dp, bottom = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "消耗品",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (query.isBlank()) {
                                "共 ${items.size} 条记录 · 在用 $usingCount 件 · 累计花费 ${yuan(totalSpend)}"
                            } else {
                                "匹配 ${shown.size} 条 · 共 ${items.size} 条记录"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(
                        onClick = {
                            viewMode = if (viewMode == ConsumableStore.VIEW_GROUPS) {
                                ConsumableStore.VIEW_LIST
                            } else {
                                ConsumableStore.VIEW_GROUPS
                            }
                            store.saveViewMode(viewMode)
                        },
                    ) {
                        Icon(
                            imageVector = if (viewMode == ConsumableStore.VIEW_GROUPS) {
                                Icons.AutoMirrored.Filled.List
                            } else {
                                Icons.Default.Menu
                            },
                            contentDescription = if (viewMode == ConsumableStore.VIEW_GROUPS) {
                                "切换成列表"
                            } else {
                                "切换成分类模块"
                            },
                        )
                    }
                    TextButton(onClick = onOpenStats) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("饼图统计")
                    }
                }

                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    placeholder = { Text("搜索消耗品：名称 / 类型 / 备注") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "清空搜索")
                            }
                        }
                    },
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

            when {
                typeDetail != null -> {
                    TypeDetailPanel(
                        typeName = typeDetail!!,
                        items = shown.filter { it.typeLabel == typeDetail },
                        onBack = { typeDetail = null },
                        onEdit = {
                            editing = it
                            editorOpen = true
                        },
                        onDelete = { target -> persist(items.filterNot { it.id == target.id }) },
                        onPriceHistory = { priceTarget = it },
                    )
                }

                viewMode == ConsumableStore.VIEW_GROUPS -> {
                    TypeGroupsGrid(
                        items = shown,
                        onOpen = { typeDetail = it },
                    )
                }

                shown.isEmpty() -> {
                    EmptyConsumableHint(searching = query.isNotBlank())
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(shown, key = { it.id }) { item ->
                            ConsumableRow(
                                item = item,
                                onSwapMetric = {
                                    persist(items.map { if (it.id == item.id) it.toggleMetric() else it })
                                },
                                onAdjust = { delta ->
                                    val total = item.totalAmount ?: 0.0
                                    val current = item.remainingAmount ?: total
                                    val next = (current + delta).coerceIn(0.0, total)
                                    persist(
                                        items.map {
                                            if (it.id == item.id) {
                                                it.copy(remainingAmount = next, metricMode = Consumable.MODE_AMOUNT)
                                            } else {
                                                it
                                            }
                                        }
                                    )
                                },
                                onSetRemaining = { quickAdjust = item },
                                onEdit = {
                                    editing = item
                                    editorOpen = true
                                },
                                onDelete = { persist(items.filterNot { it.id == item.id }) },
                                onPriceHistory = { priceTarget = item },
                            )
                        }
                        item {
                            Text(
                                text = "「在用」的物品会按开始使用的天数一直累计；补上「用完时间」后就会变成记录，参与饼图统计。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 16.dp),
                            )
                        }
                    }
                }
            }
        }
    }

    quickAdjust?.let { item ->
        QuickRemainingDialog(
            item = item,
            onDismiss = { quickAdjust = null },
            onConfirm = { value ->
                persist(
                    items.map {
                        if (it.id == item.id) {
                            it.copy(
                                remainingAmount = value.coerceIn(0.0, item.totalAmount ?: value),
                                metricMode = Consumable.MODE_AMOUNT,
                            )
                        } else {
                            it
                        }
                    }
                )
                quickAdjust = null
            },
        )
    }

    priceTarget?.let { target ->
        PriceHistoryDialog(
            item = items.firstOrNull { it.id == target.id } ?: target,
            onDismiss = { priceTarget = null },
        )
    }

    if (purchaseOpen) {
        ConsumablePurchaseDialog(
            items = items,
            types = types,
            onDismiss = { purchaseOpen = false },
            onSave = { name, type, price, amount, unit, note, date ->
                purchaseOpen = false
                store.recordPurchase(
                    name = name,
                    type = type,
                    price = price,
                    amount = amount,
                    unit = unit,
                    note = note,
                    date = date,
                )
                items = store.loadConsumables()
                types = store.loadTypes()
            },
        )
    }

    if (editorOpen) {
        ConsumableEditorDialog(
            initial = editing,
            types = types,
            existingNames = items.filter { it.id != editing?.id }.map { it.name },
            onAddType = { name ->
                val added = store.addType(name)
                if (added) types = store.loadTypes()
                added
            },
            onDismiss = { editorOpen = false },
            onSave = { updated ->
                editorOpen = false
                val list = items.toMutableList()
                val index = list.indexOfFirst { it.id == updated.id }
                if (index >= 0) list[index] = updated else list.add(updated)
                persist(list)
            },
        )
    }
}

@Composable
private fun EmptyConsumableHint(searching: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = if (searching) "没有匹配的消耗品" else "还没有记录消耗品",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (searching) {
                "换个关键词试试，或者清空搜索框"
            } else {
                "点右下角 ＋ 记下第一件：洗面奶、牙膏、米、洗衣液……\n填上开始使用的日期，用完时再补上日期，就能统计了。"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** 关键词匹配：名称 / 类型 / 备注。 */
private fun Consumable.matchesQuery(query: String): Boolean {
    val q = query.trim()
    if (q.isEmpty()) return true
    return name.contains(q, ignoreCase = true) ||
        type.contains(q, ignoreCase = true) ||
        note.contains(q, ignoreCase = true)
}

private fun yuan(value: Double): String = "¥" + String.format(Locale.US, "%.2f", value)

@Composable
private fun ConsumableRow(
    item: Consumable,
    onSwapMetric: () -> Unit,
    onAdjust: (Double) -> Unit,
    onSetRemaining: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onPriceHistory: () -> Unit = {},
) {
    var menuOpen by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Text(
                            text = item.typeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier
                            .size(7.dp)
                            .background(
                                if (item.isUsing) Color(0xFF2FA84F) else Color(0xFF9AA4B2),
                                CircleShape,
                            )
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (item.isUsing) "在用" else "已用完",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (item.isUsing) Color(0xFF2FA84F) else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(Modifier.height(4.dp))
                Text(
                    text = item.dateRangeText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.metricMainText,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "  ⇄",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onSwapMetric() }
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = item.metricSubText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(Modifier.height(6.dp))
                Row {
                    Text(
                        text = "用量 ${item.quantityText}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = "价格 ${item.priceText}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (item.purchaseCount > 0) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "累计花费 ${item.totalSpentText} （${item.purchaseCount} 次购买）",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                if (item.totalAmount != null) {
                    val step = if ((item.totalAmount ?: 0.0) >= 100.0) 10.0 else 1.0
                    val left = item.remainingAmount ?: 0.0
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "剩余 ${item.remainingText}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onSetRemaining() }
                                .padding(end = 4.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        IconButton(
                            onClick = { onAdjust(-step) },
                            enabled = left > 0.0,
                            modifier = Modifier.size(32.dp),
                        ) {
                            Text(
                                "−",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        IconButton(
                            onClick = { onAdjust(step) },
                            enabled = left < (item.totalAmount ?: 0.0),
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "补一点",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Spacer(Modifier.width(2.dp))
                        Text(
                            text = "每次 ${weightTextOf(step)}${item.unit} · 点数字可精确设置",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                if (item.note.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = item.note,
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
                    DropdownMenuItem(
                        text = { Text("价格记录（${item.priceRecordText}）") },
                        leadingIcon = { Icon(Icons.Default.Star, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onPriceHistory()
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

// ---------------- 分类模块视图 ----------------

@Composable
private fun TypeGroupsGrid(items: List<Consumable>, onOpen: (String) -> Unit) {
    val groups = items.groupBy { it.typeLabel }.toList().sortedBy { it.first }
    if (groups.isEmpty()) {
        Text(
            text = "没有可以分组的记录",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(32.dp),
        )
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        gridItems(groups, key = { it.first }) { (label, list) ->
            TypeGroupCard(label = label, list = list, onOpen = { onOpen(label) })
        }
    }
}

@Composable
private fun TypeGroupCard(label: String, list: List<Consumable>, onOpen: () -> Unit) {
    val color = typeColor(label)
    val using = list.count { it.isUsing }
    val spend = list.sumOf { it.totalSpent }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(16.dp),
        onClick = onOpen,
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                Modifier
                    .size(width = 28.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${list.size} 条记录 · 在用 $using 件",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "累计花费 ${yuan(spend)}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = color,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "点开查看详细 →",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun typeColor(label: String): Color {
    val index = kotlin.math.abs(label.hashCode()) % com.deepseek.dailytasks.data.STAT_PALETTE.size
    return Color(com.deepseek.dailytasks.data.STAT_PALETTE[index])
}

@Composable
private fun TypeDetailPanel(
    typeName: String,
    items: List<Consumable>,
    onBack: () -> Unit,
    onEdit: (Consumable) -> Unit,
    onDelete: (Consumable) -> Unit,
    onPriceHistory: (Consumable) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = typeName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "${items.size} 条记录 · 累计花费 ${yuan(items.sumOf { it.totalSpent })}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        if (items.isEmpty()) {
            Text(
                text = "这个类型下还没有记录",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(32.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 96.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(items.sortedByDescending { it.startDate }, key = { it.id }) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    text = if (item.isUsing) "在用" else "已用完",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (item.isUsing) Color(0xFF2FA84F) else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = item.dateRangeText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "${item.metricMainText} · ${item.metricSubText}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "用量 ${item.quantityText} · 价格 ${item.priceText} · ${item.priceRecordText}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                TextButton(onClick = { onPriceHistory(item) }) { Text("价格记录") }
                                TextButton(onClick = { onEdit(item) }) { Text("编辑") }
                                TextButton(onClick = { onDelete(item) }) {
                                    Text("删除", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------- 价格变化 ----------------

@Composable
private fun PriceHistoryDialog(item: Consumable, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("价格记录 · ${item.name}") },
        text = {
            if (item.priceHistory.isEmpty()) {
                Text(
                    "还没有价格记录。\n之后每次在「编辑」里改一次价格并保存，这里就会多一条购买记录，方便看价格涨跌。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Column(modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                    Text(
                        text = "平均 ${yuan(item.averagePrice)} · 共 ${item.priceHistory.size} 次购买",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    item.lastPriceDelta?.let { delta ->
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (delta > 0.0) {
                                "最近一次涨了 ${yuan(delta)}"
                            } else if (delta < 0.0) {
                                "最近一次降了 ${yuan(-delta)}"
                            } else {
                                "最近一次价格没变"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = if (delta > 0.0) Color(0xFFD64545) else Color(0xFF2FA84F),
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    PriceSparkline(item.priceHistory)
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    val descending = item.priceHistory.reversed()
                    descending.forEachIndexed { index, point ->
                        val previous = descending.getOrNull(index + 1)
                        val delta = if (previous == null) null else point.price - previous.price
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = point.date.format(Consumable.FMT),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                            )
                            if (previous == null) {
                                Text(
                                    text = "首次",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(Modifier.width(8.dp))
                            } else if (delta != null && delta != 0.0) {
                                Text(
                                    text = if (delta > 0.0) "↑ ${yuan(delta)}" else "↓ ${yuan(-delta)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (delta > 0.0) Color(0xFFD64545) else Color(0xFF2FA84F),
                                )
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(
                                text = point.priceText,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } },
    )
}

@Composable
private fun PriceSparkline(points: List<PricePoint>) {
    if (points.isEmpty()) return
    val color = MaterialTheme.colorScheme.primary
    Canvas(modifier = Modifier.fillMaxWidth().height(64.dp)) {
        val min = points.minOf { it.price }
        val max = points.maxOf { it.price }
        val range = (max - min).takeIf { it > 0.0 } ?: 1.0
        val stepX = if (points.size > 1) size.width / (points.size - 1) else 0f
        val padded = 6f
        val usable = (size.height - padded * 2).coerceAtLeast(1f)
        val coords = points.mapIndexed { index, point ->
            val x = index * stepX
            val y = padded + usable * (1f - ((point.price - min) / range).toFloat())
            Offset(x, y)
        }
        coords.zipWithNext().forEach { (a, b) ->
            drawLine(color = color, start = a, end = b, strokeWidth = 3f)
        }
        coords.forEach { point -> drawCircle(color = color, radius = 4f, center = point) }
    }
}
