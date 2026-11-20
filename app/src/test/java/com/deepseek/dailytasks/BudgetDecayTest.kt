package com.deepseek.dailytasks

import com.deepseek.dailytasks.data.BudgetItem
import com.deepseek.dailytasks.data.Savings
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 会逐日 / 逐几日降低的必要开支。 */
class BudgetDecayTest {

    private fun daily(amount: Double = 3000.0, step: Double = 100.0) = BudgetItem(
        id = 1L,
        name = "生活费",
        amount = amount,
        decreaseAmount = step,
        decreaseEveryDays = 1,
    )

    @Test
    fun firstDayShowsFullAmount() {
        val item = daily()
        val first = LocalDate.of(2026, 11, 1)
        assertEquals(3000.0, item.remainingOn(first), 0.0001)
        assertEquals(0, item.passedSteps(first))
    }

    @Test
    fun decreasesEveryDay() {
        val item = daily()
        // 11/1 起算，11/15 已经过了 14 天
        assertEquals(1600.0, item.remainingOn(LocalDate.of(2026, 11, 15)), 0.0001)
        assertEquals(14, item.passedSteps(LocalDate.of(2026, 11, 15)))
    }

    @Test
    fun decreasesEveryThreeDays() {
        val item = BudgetItem(
            id = 2L,
            name = "零食预算",
            amount = 3000.0,
            decreaseAmount = 50.0,
            decreaseEveryDays = 3,
        )
        // 11/1 起算：11/3 才过 2 天（0 个周期），11/4 过 3 天（1 个周期）
        assertEquals(3000.0, item.remainingOn(LocalDate.of(2026, 11, 3)), 0.0001)
        assertEquals(2950.0, item.remainingOn(LocalDate.of(2026, 11, 4)), 0.0001)
        assertEquals(2850.0, item.remainingOn(LocalDate.of(2026, 11, 10)), 0.0001)
    }

    @Test
    fun neverGoesBelowZero() {
        val item = daily(amount = 250.0, step = 100.0)
        assertEquals(0.0, item.remainingOn(LocalDate.of(2026, 11, 20)), 0.0001)
        assertTrue(item.isExhausted(LocalDate.of(2026, 11, 20)))
    }

    @Test
    fun fixedItemIgnoresDays() {
        val item = BudgetItem(id = 3L, name = "房租", amount = 2000.0)
        assertFalse(item.isDecreasing)
        assertEquals(2000.0, item.remainingOn(LocalDate.of(2026, 11, 28)), 0.0001)
        assertEquals("", item.decreaseText)
    }

    @Test
    fun decreaseTextAndNextDate() {
        assertEquals("每天 −¥100", daily().decreaseText)
        assertEquals("每 3 天 −¥50", daily(step = 50.0).copy(decreaseEveryDays = 3).decreaseText)

        val today = LocalDate.of(2026, 11, 15)
        assertEquals(LocalDate.of(2026, 11, 16), daily().nextDecreaseDate(today))

        val everyThree = daily(step = 50.0).copy(decreaseEveryDays = 3)
        // 11/2 过了 1 天，还没到第一个周期，下一次是 11/4
        assertEquals(LocalDate.of(2026, 11, 4), everyThree.nextDecreaseDate(LocalDate.of(2026, 11, 2)))
    }

    @Test
    fun summaryUsesTodaysRemaining() {
        val today = LocalDate.of(2026, 11, 15)
        val items = listOf(
            daily(), // 还剩 1600
            BudgetItem(id = 9L, name = "话费", amount = 200.0, reservedMonth = "2026-11"),
        )
        val summary = Savings.budgetSummary(items, today)
        assertEquals(1800.0, summary.total, 0.0001)
        assertEquals(200.0, summary.reserved, 0.0001)
        assertEquals(1600.0, summary.remaining, 0.0001)
        assertEquals(2, summary.count)
        assertEquals(1, summary.reservedCount)
    }

    @Test
    fun restartsAtMonthBeginning() {
        val item = daily()
        // 跨月后从新月份 1 号重新算
        assertEquals(3000.0, item.remainingOn(LocalDate.of(2026, 12, 1)), 0.0001)
        assertEquals(2900.0, item.remainingOn(LocalDate.of(2026, 12, 2)), 0.0001)
    }

    @Test
    fun customStartDateIsRespected() {
        val item = daily().copy(decreaseStartEpochDay = LocalDate.of(2026, 11, 11).toEpochDay())
        // 11/11 是起算日：11/11 全价，11/12 少 100
        assertEquals(3000.0, item.remainingOn(LocalDate.of(2026, 11, 11)), 0.0001)
        assertEquals(2900.0, item.remainingOn(LocalDate.of(2026, 11, 12)), 0.0001)
    }
}
