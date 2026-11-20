package com.deepseek.dailytasks

import com.deepseek.dailytasks.data.Consumable
import com.deepseek.dailytasks.data.NameGuard
import com.deepseek.dailytasks.data.Task
import com.deepseek.dailytasks.data.TypeCatalog
import com.deepseek.dailytasks.data.matchesQuery
import com.deepseek.dailytasks.data.sortTasksForDisplay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** 重名校验、必要级排序、类型目录、搜索匹配。 */
class NameGuardAndTypeTest {

    private fun task(
        id: Long,
        title: String,
        minutes: Int = 600,
        priority: Int = Task.PRIORITY_NORMAL,
        note: String = "",
    ) = Task(
        id = id,
        title = title,
        startMinutes = minutes,
        note = note,
        repeatEveryDays = 1,
        anchorEpochDay = LocalDate.of(2026, 1, 1).toEpochDay(),
        priority = priority,
    )

    private fun consumable(id: Long, name: String, type: String = "", note: String = "") = Consumable(
        id = id,
        name = name,
        type = type,
        startDate = LocalDate.of(2026, 10, 1),
        note = note,
    )

    // ---------------- 重名 ----------------

    @Test
    fun duplicateTaskNameIsBlocked() {
        assertTrue(NameGuard.isDuplicate("买菜", listOf("买菜", "写周报")))
        assertFalse(NameGuard.isDuplicate("跑步", listOf("买菜", "写周报")))
    }

    @Test
    fun duplicateIgnoresCaseAndExtraSpaces() {
        assertTrue(NameGuard.isDuplicate("  Buy Milk ", listOf("buy milk")))
        assertTrue(NameGuard.isDuplicate("写  周报", listOf("写 周报")))
        assertFalse(NameGuard.isDuplicate("   ", listOf("")))
    }

    @Test
    fun editingOwnNameIsNotADuplicate() {
        val tasks = listOf(task(1, "买菜"), task(2, "写周报"))
        assertFalse(NameGuard.taskDuplicate("买菜", tasks, excludeId = 1))
        assertTrue(NameGuard.taskDuplicate("买菜", tasks, excludeId = 2))
    }

    @Test
    fun duplicateConsumableNameIsBlocked() {
        val items = listOf(consumable(1, "洗面奶"), consumable(2, "牙膏"))
        assertTrue(NameGuard.consumableDuplicate("洗面奶", items))
        assertFalse(NameGuard.consumableDuplicate("洗面奶", items, excludeId = 1))
        assertFalse(NameGuard.consumableDuplicate("洗发水", items))
    }

    // ---------------- 必要级 ----------------

    @Test
    fun necessaryTasksComeFirstThenByTime() {
        val sorted = sortTasksForDisplay(
            listOf(
                task(1, "普通1", minutes = 540),
                task(2, "必要1", minutes = 720, priority = Task.PRIORITY_NECESSARY),
                task(3, "重要1", minutes = 600, priority = Task.PRIORITY_IMPORTANT),
                task(4, "必要2", minutes = 660, priority = Task.PRIORITY_NECESSARY),
            )
        )
        assertEquals(listOf("必要2", "必要1", "重要1", "普通1"), sorted.map { it.title })
    }

    @Test
    fun priorityLabels() {
        assertEquals("必要", Task.priorityLabelOf(Task.PRIORITY_NECESSARY))
        assertEquals("重要", Task.priorityLabelOf(Task.PRIORITY_IMPORTANT))
        assertEquals("普通", Task.priorityLabelOf(Task.PRIORITY_NORMAL))
        assertEquals("必要", task(1, "x", priority = Task.PRIORITY_NECESSARY).priorityLabel)
    }

    // ---------------- 搜索 ----------------

    @Test
    fun queryMatchesTitleNoteAndCategory() {
        val t = task(1, "买牛奶", note = "超市，顺便买鸡蛋")
        assertTrue(t.matchesQuery("牛奶", "生活"))
        assertTrue(t.matchesQuery("鸡蛋", "生活"))
        assertTrue(t.matchesQuery("生活", "生活"))
        assertTrue(t.matchesQuery("  ", "生活"))
        assertFalse(t.matchesQuery("牛奶", null).not())
    }

    // ---------------- 消耗品类型目录 ----------------

    @Test
    fun typeCatalogMergesStoredAndHistoryWithoutDuplicates() {
        val merged = TypeCatalog.merge(
            stored = listOf("洗护", "清洁"),
            history = listOf("清洁", "食品", "洗护 ", "")
        )
        assertEquals(listOf("洗护", "清洁", "食品"), merged)
    }

    @Test
    fun addingNewTypeAppearsOnceAndRejectsDuplicate() {
        val start = listOf("洗护")
        val added = TypeCatalog.add(start, "食品")
        assertEquals(listOf("洗护", "食品"), added)
        assertEquals(added, TypeCatalog.add(added, " 食品 "))
        assertEquals(added, TypeCatalog.add(added, "洗护"))
        assertTrue(TypeCatalog.contains(added, "食品"))
        assertFalse(TypeCatalog.contains(added, "药品"))
    }

    @Test
    fun typeCatalogFiltersByKeyword() {
        val types = listOf("洗护用品", "清洁用品", "食品")
        assertEquals(listOf("洗护用品", "清洁用品"), TypeCatalog.filter(types, "用品"))
        assertEquals(types, TypeCatalog.filter(types, ""))
    }
}
