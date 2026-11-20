package com.deepseek.dailytasks

import com.deepseek.dailytasks.data.Exercise
import com.deepseek.dailytasks.data.Task
import com.deepseek.dailytasks.data.WorkoutLogic
import com.deepseek.dailytasks.data.WorkoutPlan
import com.deepseek.dailytasks.data.WorkoutTemplate
import com.deepseek.dailytasks.data.Consumable
import com.deepseek.dailytasks.data.SessionEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ConsumableMetricAndTemplateTest {

    // ---------------- 消耗品：剩余量 / 已用天数 互换 ----------------

    private fun item(
        total: Double? = null,
        remaining: Double? = null,
        mode: Int = Consumable.MODE_DAYS,
        startDaysAgo: Long = 4,
        end: LocalDate? = null,
    ) = Consumable(
        id = 1L,
        name = "洗衣液",
        type = "清洁",
        startDate = LocalDate.now().minusDays(startDaysAgo),
        endDate = end,
        quantity = 2.0,
        unit = "升",
        price = 39.9,
        totalAmount = total,
        remainingAmount = remaining,
        metricMode = mode,
    )

    @Test
    fun withoutAmountsFallsBackToDays() {
        val c = item()
        assertFalse(c.hasAmount)
        assertEquals(4, c.daysUsed)
        assertTrue(c.metricMainText.contains("已用"))
        assertTrue(c.metricMainText.contains("4"))
    }

    @Test
    fun consumedAmountUsesRemainingWhenTotalMissing() {
        val c = item(remaining = 1.2)
        assertEquals(1.2, c.remainingAmount!!, 1e-6)
        // 没有总量时无法算消耗量
        assertEquals(null, c.consumedAmount)
    }

    @Test
    fun remainingRatioAndText() {
        val c = item(total = 2.0, remaining = 0.5)
        assertEquals(1.5, c.consumedAmount!!, 1e-6)
        assertEquals(0.25f, c.remainingRatio!!, 1e-6f)
        assertTrue(c.metricSubText.contains("剩"))
        assertEquals("0.5升", c.remainingText)
        assertEquals("1.5升", c.consumedText)
    }

    @Test
    fun toggleMetricSwitchesBetweenDaysAndAmount() {
        val c = item(total = 2.0, remaining = 0.5, mode = Consumable.MODE_DAYS)
        assertEquals(Consumable.MODE_DAYS, c.metricMode)
        val switched = c.toggleMetric()
        assertEquals(Consumable.MODE_AMOUNT, switched.metricMode)
        assertTrue(switched.metricMainText.contains("剩"))
        assertTrue(switched.metricMainText.contains("25%"))
        assertEquals(Consumable.MODE_DAYS, switched.toggleMetric().metricMode)
    }

    @Test
    fun finishedItemUsesTotalDaysAndKeepsAmounts() {
        val end = LocalDate.now().minusDays(1)
        val c = item(total = 2.0, remaining = 0.0, mode = Consumable.MODE_AMOUNT, startDaysAgo = 6, end = end)
        assertEquals(5, c.daysUsed)
        assertEquals(0.0f, c.remainingRatio!!, 1e-6f)
        assertTrue(c.metricSubText.contains("已用"))
    }

    // ---------------- 锻炼：自定义部位 / 动作 / 模板 ----------------

    private val plan = WorkoutPlan(
        id = "p1",
        part = "胸",
        title = "胸 日",
        exercises = listOf(Exercise("卧推", 4, "8"), Exercise("俯卧撑", 3, "15")),
    )

    @Test
    fun customPartsCanBeAddedAndDetected() {
        val parts = listOf("胸", "背")
        val added = WorkoutLogic.addPart(parts, "小腿")
        assertEquals(listOf("胸", "背", "小腿"), added)
        assertTrue(WorkoutLogic.hasPart(added, "小腿"))
        assertFalse(WorkoutLogic.hasPart(added, "腿"))
        // 重复添加不会变
        assertEquals(added, WorkoutLogic.addPart(added, "小腿"))
    }

    @Test
    fun removePartDropsIt() {
        val parts = WorkoutLogic.removePart(listOf("胸", "背", "腿"), "背")
        assertEquals(listOf("胸", "腿"), parts)
    }

    @Test
    fun templateFromSavesCurrentEntries() {
        val entries = WorkoutLogic.entriesOf(plan.exercises, emptyList())
        val template = WorkoutLogic.templateFrom(7L, "推日", listOf("胸"), entries)
        assertEquals("推日", template.name)
        assertEquals(listOf("胸"), template.parts)
        assertEquals(2, template.exercises.size)
        assertEquals("卧推", template.exercises[0].name)
    }

    @Test
    fun applyTemplateReplacesEntriesAndKeepsProgressOfSameExercise() {
        val template = WorkoutTemplate(
            id = 1L,
            name = "推日",
            parts = listOf("胸"),
            exercises = listOf(Exercise("卧推", 4, "8"), Exercise("弯举", 3, "12")),
        )
        val previous = listOf(SessionEntry("卧推", 4, "8", doneSets = 2))
        val applied = WorkoutLogic.applyTemplate(template, previous)
        assertEquals(2, applied.size)
        assertEquals(2, applied[0].done)
        assertEquals(0, applied[1].done)
        assertEquals("弯举", applied[1].name)
    }

    @Test
    fun withExerciseReplacesSameNameAndAppendsNew() {
        var entries = WorkoutLogic.entriesOf(plan.exercises, emptyList())
        entries = WorkoutLogic.withExercise(entries, Exercise("卧推", 5, "8"))
        assertEquals(2, entries.size)
        assertEquals(5, entries.first { it.name == "卧推" }.sets)
        entries = WorkoutLogic.withExercise(entries, Exercise("飞鸟", 3, "12"))
        assertEquals(3, entries.size)
        assertNotNull(entries.firstOrNull { it.name == "飞鸟" })
    }

    @Test
    fun removedExerciseDisappearsFromSession() {
        val entries = WorkoutLogic.entriesOf(plan.exercises, emptyList())
        val after = WorkoutLogic.removeExercise(entries, "俯卧撑")
        assertEquals(1, after.size)
        assertEquals("卧推", after[0].name)
    }

    @Test
    fun moveTaskUsesSameHelperForToday() {
        // 日期跳转的间接校验：同一任务在锚点日及之后每 3 天可见
        val task = Task(
            id = 3L,
            title = "浇花",
            startMinutes = 480,
            repeatEveryDays = 3,
            anchorEpochDay = LocalDate.of(2026, 10, 1).toEpochDay(),
        )
        assertTrue(task.isVisibleOn(LocalDate.of(2026, 10, 4)))
        assertFalse(task.isVisibleOn(LocalDate.of(2026, 10, 5)))
        assertTrue(task.isVisibleOn(LocalDate.of(2026, 10, 7)))
    }
}
