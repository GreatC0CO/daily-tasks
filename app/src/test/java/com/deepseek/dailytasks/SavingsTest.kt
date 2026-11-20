package com.deepseek.dailytasks

import com.deepseek.dailytasks.data.BudgetItem
import com.deepseek.dailytasks.data.SavingTaskKind
import com.deepseek.dailytasks.data.SavingContribution
import com.deepseek.dailytasks.data.SavingGoal
import com.deepseek.dailytasks.data.SavingTask
import com.deepseek.dailytasks.data.Savings
import com.deepseek.dailytasks.data.moneyText
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 攒钱与本月必要开支的纯逻辑测试。 */
class SavingsTest {

    private val today = LocalDate.of(2026, 11, 15)

    private val dailyTask = SavingTask(id = 1L, name = "早起", amount = 5.0, kind = SavingTaskKind.DAILY, maxPerDay = 1)
    private val eachTask = SavingTask(id = 2L, name = "少点外卖", amount = 15.0, kind = SavingTaskKind.MILESTONE)

    private fun goal(tasks: List<SavingTask> = listOf(dailyTask, eachTask)) = SavingGoal(
        id = 100L,
        name = "相机",
        targetAmount = 100.0,
        tasks = tasks,
    )

    @Test
    fun dailyTaskCanBeClaimedOncePerDay() {
        val once = Savings.claim(goal(), dailyTask, today)
        assertTrue(once.contributions.isNotEmpty())
        assertFalse(Savings.canClaim(once, dailyTask, today))
        assertTrue(Savings.canClaim(once, dailyTask, today.plusDays(1)))
    }

    @Test
    fun eachTimeTaskCanBeClaimedRepeatedly() {
        var g = Savings.claim(goal(), eachTask, today)
        assertTrue(Savings.canClaim(g, eachTask, today))
        g = Savings.claim(g, eachTask, today)
        assertEquals(30.0, g.savedAmount, 0.0001)
        assertEquals(2, g.contributions.size)
    }

    @Test
    fun claimAddsAmountAndContribution() {
        val g = Savings.claim(goal(), dailyTask, today)
        assertEquals(5.0, g.savedAmount, 0.0001)
        assertEquals(1, g.contributions.size)
        assertEquals(today.toEpochDay(), g.contributions.first().epochDay)
        assertEquals("早起", g.contributions.first().taskName)
    }

    @Test
    fun undoRefundsAndRemoves() {
        val g = Savings.claim(goal(), eachTask, today)
        val contribution: SavingContribution = g.contributions.first()
        val back = Savings.undo(g, contribution)
        assertEquals(0.0, back.savedAmount, 0.0001)
        assertTrue(back.contributions.isEmpty())
    }

    @Test
    fun claimableTodaySumsOnlyAvailableTasks() {
        assertEquals(20.0, Savings.claimableToday(goal(), today), 0.0001)
        val after = Savings.claim(goal(), dailyTask, today)
        assertEquals(15.0, Savings.claimableToday(after, today), 0.0001)
    }

    @Test
    fun budgetSummaryOnlyCountsCurrentMonthReservations() {
        val items = listOf(
            BudgetItem(1L, "房租", 2000.0, reservedMonth = "2026-11"),
            BudgetItem(2L, "水电", 200.0, reservedMonth = "2026-10"),
            BudgetItem(3L, "话费", 50.0),
        )
        val summary = Savings.budgetSummary(items, today)
        assertEquals(2250.0, summary.total, 0.0001)
        assertEquals(2000.0, summary.reserved, 0.0001)
        assertEquals(250.0, summary.remaining, 0.0001)
        assertEquals(3, summary.count)
        assertEquals(1, summary.reservedCount)
    }

    @Test
    fun daysLeftInMonthAndDailyReserve() {
        assertEquals(16, Savings.daysLeftInMonth(today))
        val summary = Savings.budgetSummary(listOf(BudgetItem(1L, "房租", 320.0)), today)
        assertEquals(20.0, Savings.dailyReserve(summary, today), 0.0001)
    }

    @Test
    fun monthKeyAndLabel() {
        assertEquals("2026-11", Savings.monthKey(today))
        assertEquals("2026 年 11 月", Savings.monthLabel(today))
    }

    @Test
    fun moneyTextDropsTrailingZeros() {
        assertEquals("¥5", moneyText(5.0))
        assertEquals("¥12.5", moneyText(12.5))
        assertEquals("¥0", moneyText(0.0))
        assertEquals("¥199.99", moneyText(199.99))
    }

    @Test
    fun goalProgressAndCompleteFlag() {
        val g = goal().copy(savedAmount = 50.0)
        assertEquals(0.5f, g.progress, 0.0001f)
        assertEquals("50%", g.percentText)
        assertFalse(g.isComplete)
        val done = g.copy(savedAmount = 100.0)
        assertTrue(done.isComplete)
        assertEquals(0.0, done.remaining, 0.0001)
    }

    @Test
    fun sortedGoalsPutsUnfinishedFirstThenHigherProgress() {
        val a = SavingGoal(1L, "A", 100.0, savedAmount = 10.0)
        val b = SavingGoal(2L, "B", 100.0, savedAmount = 80.0)
        val c = SavingGoal(3L, "C", 100.0, savedAmount = 100.0)
        val sorted = Savings.sortedGoals(listOf(c, a, b))
        assertEquals(listOf("B", "A", "C"), sorted.map { it.name })
    }
}
