package com.deepseek.dailytasks

import com.deepseek.dailytasks.data.Exercise
import com.deepseek.dailytasks.data.SessionEntry
import com.deepseek.dailytasks.data.Task
import com.deepseek.dailytasks.data.WorkoutLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** v1.7：动作身份（同名不同负荷）、拆分/合并/排序，以及任务推迟。 */
class SplitMergeAndDeferralTest {

    private val today = LocalDate.of(2026, 10, 30)

    private fun ex(name: String, sets: Int, reps: String, weight: Double, color: Int = 0) =
        Exercise(name = name, sets = sets, reps = reps, weight = weight, color = color)

    // ---------------- 动作身份 ----------------

    @Test
    fun sameNameDifferentLoadAreDifferentExercises() {
        val a = ex("卧推", 3, "8", 60.0)
        val b = ex("卧推", 3, "6", 70.0)
        assertTrue(a.key != b.key)
        assertTrue(a.key == ex("卧推", 3, "8", 60.0).key)
    }

    @Test
    fun mergeKeepsAllSameNameVariants() {
        val plan = com.deepseek.dailytasks.data.WorkoutPlan(
            id = "chest",
            part = "胸",
            title = "胸",
            exercises = listOf(ex("卧推", 3, "8", 60.0), ex("卧推", 2, "6", 70.0), ex("飞鸟", 3, "12", 10.0)),
        )
        val merged = WorkoutLogic.mergeExercises(listOf(plan))
        assertEquals(3, merged.size)
        assertEquals(listOf("卧推", "卧推", "飞鸟"), merged.map { it.name })
    }

    // ---------------- 拆分 / 合并 ----------------

    @Test
    fun splitKeepsTotalSets() {
        val parts = WorkoutLogic.splitExercise(
            exercise = ex("卧推", 5, "8", 60.0),
            firstSets = 3,
            firstReps = "8",
            firstWeight = 60.0,
            secondReps = "6",
            secondWeight = 70.0,
        )
        assertEquals(2, parts.size)
        assertEquals(5, parts.sumOf { it.sets })
        assertEquals(3, parts[0].sets)
        assertEquals(60.0, parts[0].weight, 0.001)
        assertEquals(2, parts[1].sets)
        assertEquals(70.0, parts[1].weight, 0.001)
    }

    @Test
    fun splitUpLimitsFirstPart() {
        val parts = WorkoutLogic.splitExercise(ex("深蹲", 3, "5", 100.0), 9, "5", 100.0, "3", 110.0)
        assertEquals(3, parts.sumOf { it.sets })
        assertEquals(2, parts[0].sets)
        assertEquals(1, parts[1].sets)
        assertEquals(110.0, parts[1].weight, 0.001)
    }

    @Test
    fun mergePairAddsSetsAndTakesChosenLoad() {
        val merged = WorkoutLogic.mergePair(
            a = ex("卧推", 3, "8", 60.0, color = 2),
            b = ex("卧推", 2, "8", 60.0, color = 0),
            reps = "8",
            weight = 60.0,
        )
        assertEquals(5, merged.sets)
        assertEquals(60.0, merged.weight, 0.001)
        assertEquals(2, merged.color)
    }

    @Test
    fun moveItemReordersOnlyTarget() {
        val list = listOf("a", "b", "c", "d")
        assertEquals(listOf("c", "a", "b", "d"), WorkoutLogic.moveItem(list, 2, 0))
        assertEquals(listOf("a", "c", "b", "d"), WorkoutLogic.moveItem(list, 1, 2))
        assertEquals(list, WorkoutLogic.moveItem(list, 9, 0))
    }

    @Test
    fun entriesOfKeepsSameNameDifferentLoadApart() {
        val exercises = listOf(ex("卧推", 3, "8", 60.0), ex("卧推", 2, "6", 70.0))
        val entries = WorkoutLogic.entriesOf(exercises)
        assertEquals(2, entries.size)
        val doneSeparately = entries.map { WorkoutLogic.addSet(it) }
        assertEquals(1, doneSeparately[0].done)
        assertEquals(1, doneSeparately[1].done)
        assertEquals(60.0, doneSeparately[0].weight, 0.001)
        assertEquals(70.0, doneSeparately[1].weight, 0.001)
    }

    @Test
    fun keepingProgressWhenOnlySetsChange() {
        val previous = listOf(SessionEntry(name = "卧推", sets = 3, reps = "8", doneSets = 2, weight = 60.0))
        val entries = WorkoutLogic.entriesOf(listOf(ex("卧推", 4, "8", 60.0)), previous)
        assertEquals(1, entries.size)
        assertEquals(4, entries[0].sets)
        assertEquals(2, entries[0].done)
    }

    // ---------------- 任务推迟 ----------------

    @Test
    fun deferredOneOffTaskHidesTodayOnly() {
        val task = Task(id = 1L, title = "洗牙", startMinutes = 540, repeatEveryDays = 0)
        // 一次性任务推迟 = 今天隐藏；它本来就会一直挂到做完，所以明天起照常可见
        val deferred = task.copy(skipEpochDay = today.toEpochDay())
        assertFalse(deferred.isVisibleOn(today))
        assertTrue(deferred.isVisibleOn(today.plusDays(1)))
        assertTrue(deferred.isVisibleOn(today.plusDays(2)))
    }

    @Test
    fun shiftingAnchorMovesEveryNDaysTask() {
        val task = Task(
            id = 2L,
            title = "浇花",
            startMinutes = 600,
            repeatEveryDays = 3,
            anchorEpochDay = today.toEpochDay(),
        )
        assertTrue(task.isVisibleOn(today))
        val shifted = task.copy(anchorEpochDay = task.anchorEpochDay + 1)
        assertFalse(shifted.isVisibleOn(today))
        assertTrue(shifted.isVisibleOn(today.plusDays(1)))
        assertTrue(shifted.isVisibleOn(today.plusDays(4)))
    }

    @Test
    fun deferredTaskHidesOnSkippedDayEvenWhenDue() {
        val task = Task(
            id = 3L,
            title = "大扫除",
            startMinutes = 600,
            repeatEveryDays = 2,
            anchorEpochDay = today.toEpochDay(),
        )
        val deferred = task.copy(skipEpochDay = today.toEpochDay(), showEpochDay = today.plusDays(1).toEpochDay())
        assertFalse(deferred.isVisibleOn(today))
        assertTrue(deferred.isVisibleOn(today.plusDays(1)))
    }
}
