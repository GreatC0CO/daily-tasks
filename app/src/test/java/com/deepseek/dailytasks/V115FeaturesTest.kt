package com.deepseek.dailytasks

import com.deepseek.dailytasks.data.Consumable
import com.deepseek.dailytasks.data.Exercise
import com.deepseek.dailytasks.data.PricePoint
import com.deepseek.dailytasks.data.SavingTaskKind
import com.deepseek.dailytasks.data.SavingContribution
import com.deepseek.dailytasks.data.SavingGoal
import com.deepseek.dailytasks.data.SavingTask
import com.deepseek.dailytasks.data.Savings
import com.deepseek.dailytasks.data.WorkoutLogic
import com.deepseek.dailytasks.data.variantMark
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** v1.15：按次攒钱上限、消耗品累计花费、同名动作 N 选一（①②③）。 */
class V115FeaturesTest {

    private val today = LocalDate.of(2026, 11, 16)

    // ---------------- 攒钱：每次完成的上限 ----------------

    private fun goal(maxPerDay: Int, contributions: List<SavingContribution> = emptyList()): SavingGoal {
        val task = SavingTask(id = 1L, name = "跑步", amount = 10.0, kind = SavingTaskKind.DAILY, maxPerDay = maxPerDay)
        return SavingGoal(
            id = 1L,
            name = "新耳机",
            targetAmount = 500.0,
            tasks = listOf(task),
            contributions = contributions,
        )
    }

    private fun contribution(count: Int): List<SavingContribution> =
        (1..count).map { SavingContribution(today.toEpochDay(), 1L, "跑步", 10.0) }

    @Test
    fun limitedTaskAllowsOnlyMaxClaimsPerDay() {
        val g = goal(maxPerDay = 2, contributions = contribution(1))
        val task = g.tasks.first()
        assertTrue(Savings.canClaim(g, task, today))
        val once = Savings.claim(g, task, today)
        assertFalse(Savings.canClaim(once, task, today))
    }

    @Test
    fun unlimitedTaskKeepsClaiming() {
        val g = goal(maxPerDay = 0, contributions = contribution(5))
        val task = g.tasks.first()
        assertTrue(Savings.canClaim(g, task, today))
        assertEquals(-1, Savings.remainingClaims(g, task, today))
    }

    @Test
    fun claimableTodayCountsRemainingTimes() {
        val g = goal(maxPerDay = 3, contributions = contribution(1))
        // 还剩 2 次 × 10 元
        assertEquals(20.0, Savings.claimableToday(g, today), 0.0001)
    }

    @Test
    fun dailyTaskCountsPerDayAndStopsAtLimit() {
        val task = SavingTask(id = 2L, name = "早起", amount = 5.0, kind = SavingTaskKind.DAILY, maxPerDay = 9)
        var g = SavingGoal(id = 2L, name = "书", targetAmount = 100.0, tasks = listOf(task))
        assertTrue(Savings.canClaim(g, task, today))
        repeat(9) { g = Savings.claim(g, task, today) }
        assertFalse(Savings.canClaim(g, task, today))
        assertTrue(Savings.canClaim(g, task, today.plusDays(1)))
    }

    @Test
    fun milestoneTaskStopsWhenTargetReached() {
        val task = SavingTask(id = 3L, name = "跑 30 次", amount = 10.0, kind = SavingTaskKind.MILESTONE, targetCount = 2)
        var g = SavingGoal(id = 3L, name = "跑鞋", targetAmount = 500.0, tasks = listOf(task))
        g = Savings.claim(g, task, today)
        assertTrue(Savings.canClaim(g, task, today))
        g = Savings.claim(g, task, today)
        assertFalse(Savings.canClaim(g, task, today))
        assertEquals(1f, Savings.taskProgress(g, task), 0.0001f)
        assertTrue(Savings.isFinished(g, task))
    }

    // ---------------- 消耗品：累计花费 ----------------

    @Test
    fun totalSpentSumsPriceHistory() {
        val item = Consumable(
            id = 1L,
            name = "洗面奶",
            price = 89.0,
            priceHistory = listOf(
                PricePoint(epochDay = 1L, price = 79.0),
                PricePoint(epochDay = 30L, price = 89.0),
                PricePoint(epochDay = 60L, price = 99.0),
            ),
        )
        assertEquals(267.0, item.totalSpent, 0.0001)
        assertEquals(3, item.purchaseCount)
    }

    @Test
    fun totalSpentFallsBackToCurrentPrice() {
        val item = Consumable(id = 2L, name = "纸巾", price = 12.5)
        assertEquals(12.5, item.totalSpent, 0.0001)
        assertEquals(1, item.purchaseCount)
    }

    // ---------------- 同名动作 N 选一（①②③） ----------------

    private fun exercise(name: String, sets: Int, reps: Int, weight: Double) =
        Exercise(name = name, sets = sets, reps = reps.toString(), weight = weight)

    private val bench = listOf(
        exercise("卧推", 4, 8, 60.0),
        exercise("卧推", 3, 10, 50.0),
        exercise("飞鸟", 3, 12, 10.0),
    )

    @Test
    fun variantGroupsOnlyListsNamesWithSeveralVariants() {
        val groups = WorkoutLogic.variantGroups(bench)
        assertEquals(1, groups.size)
        assertEquals("卧推", groups.first().first)
        assertEquals(2, groups.first().second.size)
    }

    @Test
    fun pickVariantsDefaultsToFirst() {
        val picked = WorkoutLogic.pickVariants(bench)
        assertEquals(2, picked.size)
        assertEquals(60.0, picked.first().weight, 0.0001)
        assertEquals("飞鸟", picked.last().name)
    }

    @Test
    fun pickVariantsHonoursSelection() {
        val picked = WorkoutLogic.pickVariants(bench, mapOf("卧推" to 1))
        assertEquals(1, picked.count { it.name == "卧推" })
        assertEquals(50.0, picked.first { it.name == "卧推" }.weight, 0.0001)
    }

    @Test
    fun entriesOfVariantsKeepsDonesForSameKey() {
        val entries = WorkoutLogic.entriesOfVariants(bench)
        val done = entries.mapIndexed { index, entry ->
            if (index == 0) WorkoutLogic.addSet(entry) else entry
        }
        val again = WorkoutLogic.entriesOfVariants(bench, done)
        assertEquals(1, again.first().doneSets)
    }

    @Test
    fun variantMarksAreNumbered() {
        assertEquals("①", variantMark(0))
        assertEquals("②", variantMark(1))
        assertEquals("③", variantMark(2))
    }
}
