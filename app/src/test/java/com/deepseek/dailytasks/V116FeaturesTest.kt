package com.deepseek.dailytasks

import com.deepseek.dailytasks.data.Exercise
import com.deepseek.dailytasks.data.SavingTask
import com.deepseek.dailytasks.data.SavingTaskKind
import com.deepseek.dailytasks.data.WorkoutLogic
import com.deepseek.dailytasks.data.variantMark
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** v1.16：不同动作的 N 选一（①② 编号）、攒钱任务类型规则。 */
class V116FeaturesTest {

    private fun exercise(name: String, sets: Int = 3, reps: String = "10", weight: Double = 0.0, color: Int = 0) =
        Exercise(name = name, sets = sets, reps = reps, weight = weight, color = color)

    // ---------------- 不同动作的 N 选一 ----------------

    @Test
    fun altGroupsOnlyListsColoredGroupsWithSeveralNames() {
        val list = listOf(
            exercise("卧推", color = 1),
            exercise("哑铃卧推", color = 1),
            exercise("深蹲", color = 2),
            exercise("硬拉", color = 0),
        )
        val groups = WorkoutLogic.altGroups(list)
        assertEquals(1, groups.size)
        assertEquals(1, groups.first().first)
        assertEquals(listOf("卧推", "哑铃卧推"), groups.first().second.map { it.name })
    }

    @Test
    fun sameNameDifferentWeightsIsNotAnAltGroup() {
        val list = listOf(
            exercise("卧推", weight = 60.0, color = 1),
            exercise("卧推", weight = 70.0, color = 1),
        )
        assertTrue(WorkoutLogic.altGroups(list).isEmpty())
    }

    @Test
    fun pickOnePerGroupDefaultsToFirst() {
        val list = listOf(
            exercise("卧推", color = 1),
            exercise("哑铃卧推", color = 1),
            exercise("硬拉", color = 0),
        )
        val picked = WorkoutLogic.pickOnePerGroup(list)
        assertEquals(listOf("卧推", "硬拉"), picked.map { it.name })
    }

    @Test
    fun pickOnePerGroupHonoursSelection() {
        val list = listOf(
            exercise("卧推", color = 1),
            exercise("哑铃卧推", color = 1),
        )
        val picked = WorkoutLogic.pickOnePerGroup(list, mapOf(1 to 1))
        assertEquals(listOf("哑铃卧推"), picked.map { it.name })
    }

    @Test
    fun pickOnePerGroupKeepsAllVariantsOfChosenName() {
        val list = listOf(
            exercise("卧推", reps = "8", weight = 60.0, color = 1),
            exercise("哑铃卧推", color = 1),
            exercise("卧推", reps = "12", weight = 40.0, color = 1),
        )
        val picked = WorkoutLogic.pickOnePerGroup(list, mapOf(1 to 0))
        assertEquals(2, picked.size)
        assertTrue(picked.all { it.name == "卧推" })
    }

    @Test
    fun entriesOfPicksKeepsDoneSetsOfChosenExercise() {
        val list = listOf(
            exercise("卧推", color = 1),
            exercise("哑铃卧推", color = 1),
        )
        val previous = WorkoutLogic.entriesOfPicks(list, emptyList(), emptyMap())
        val trained = previous.map { it.copy(doneSets = it.sets) }
        val next = WorkoutLogic.entriesOfPicks(list, trained, mapOf(1 to 0))
        assertEquals(listOf("卧推"), next.map { it.name })
        assertTrue(next.first().isComplete)
    }

    @Test
    fun groupMarksUseGroupNumber() {
        val marks = WorkoutLogic.groupMarks(
            listOf(
                exercise("卧推", color = 1),
                exercise("哑铃卧推", color = 1),
                exercise("高位下拉", color = 3),
                exercise("引体向上", color = 3),
            ),
        )
        assertEquals("①", marks["卧推"])
        assertEquals("①", marks["哑铃卧推"])
        assertEquals("③", marks["高位下拉"])
        assertEquals("③", marks["引体向上"])
    }

    @Test
    fun variantMarksGoBeyondTwenty() {
        assertEquals("①", variantMark(0))
        assertEquals("⑳", variantMark(19))
    }

    // ---------------- 攒钱任务类型 ----------------

    @Test
    fun milestoneRuleTextMentionsTargetCount() {
        val task = SavingTask(id = 1L, name = "跑步", amount = 10.0, kind = SavingTaskKind.MILESTONE, targetCount = 30)
        assertTrue(task.isMilestone)
        assertTrue(task.hasTarget)
        assertEquals("里程碑 · 共 30 次", task.ruleText)
    }

    @Test
    fun dailyRuleTextMentionsPerDayLimit() {
        val task = SavingTask(id = 1L, name = "喝水", amount = 1.0, kind = SavingTaskKind.DAILY, maxPerDay = 2)
        assertTrue(task.isDaily)
        assertEquals("日常 · 每天最多 2 次", task.ruleText)
        assertFalse(task.hasTarget)
    }

    @Test
    fun unlimitedDailyTaskHasNoLimit() {
        val task = SavingTask(id = 1L, name = "喝水", amount = 1.0, kind = SavingTaskKind.DAILY)
        assertFalse(task.limited)
        assertEquals("日常 · 不限次数", task.ruleText)
    }
}
