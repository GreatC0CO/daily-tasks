package com.deepseek.dailytasks

import com.deepseek.dailytasks.data.Exercise
import com.deepseek.dailytasks.data.WorkoutLogic
import com.deepseek.dailytasks.data.WorkoutPlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 同色动作「N 选一」按动作名分组：同名的不同重量都保留，不同名只留选中的那个。
 */
class ColorGroupPickTest {

    private fun ex(
        name: String,
        sets: Int = 3,
        reps: String = "8",
        weight: Double = 0.0,
        color: Int = 0,
    ) = Exercise(name = name, sets = sets, reps = reps, weight = weight, color = color)

    @Test
    fun defaultAlternativeIsFirst() {
        val a = ex("上斜卧推", color = 1)
        val b = ex("平板卧推", color = 1)
        val kept = WorkoutLogic.pickAlternatives(listOf(a, b))
        assertEquals(listOf("上斜卧推"), kept.map { it.name })
    }

    @Test
    fun selectedAlternativeIsUsed() {
        val a = ex("上斜卧推", color = 1)
        val b = ex("平板卧推", color = 1)
        val kept = WorkoutLogic.pickAlternatives(listOf(a, b), mapOf(1 to "平板卧推"))
        assertEquals(listOf("平板卧推"), kept.map { it.name })
    }

    @Test
    fun entriesOfKeepsOnlyChosenOfGroup() {
        val a = ex("上斜卧推", color = 1)
        val b = ex("平板卧推", color = 1)
        val entries = WorkoutLogic.entriesOf(listOf(a, b), emptyList(), mapOf(1 to b.name))
        assertEquals(1, entries.size)
        assertEquals("平板卧推", entries[0].name)
    }

    @Test
    fun withoutPickGroupFallsBackToFirst() {
        val a = ex("上斜卧推", color = 1)
        val b = ex("平板卧推", color = 1)
        val entries = WorkoutLogic.entriesOf(listOf(a, b))
        assertEquals(1, entries.size)
        assertEquals("上斜卧推", entries[0].name)
    }

    @Test
    fun sameNameDifferentWeightsAreAllKept() {
        val light = ex("卧推", weight = 60.0, color = 1)
        val heavy = ex("卧推", weight = 70.0, color = 1)
        val kept = WorkoutLogic.pickAlternatives(listOf(light, heavy))
        assertEquals(2, kept.size)
        assertEquals(listOf(60.0, 70.0), kept.map { it.weight })
    }

    @Test
    fun sameNameDifferentWeightsInColorGroupStayTogether() {
        val light = ex("卧推", weight = 60.0, color = 1)
        val heavy = ex("卧推", weight = 70.0, color = 1)
        val other = ex("飞鸟", weight = 10.0, color = 1)
        val kept = WorkoutLogic.pickAlternatives(listOf(light, heavy, other))
        assertEquals(listOf("卧推", "卧推"), kept.map { it.name })
        // 选「飞鸟」则两条卧推一起消失、飞鸟保留
        val picked = WorkoutLogic.pickAlternatives(listOf(light, heavy, other), mapOf(1 to "飞鸟"))
        assertEquals(listOf("飞鸟"), picked.map { it.name })
    }

    @Test
    fun colorGroupsListDistinctNames() {
        val a = ex("上斜卧推", color = 1)
        val b = ex("上斜卧推", weight = 70.0, color = 1)
        val c = ex("平板卧推", color = 1)
        val groups = WorkoutLogic.colorGroups(listOf(a, b, c))
        assertEquals(1, groups.size)
        assertEquals(listOf("上斜卧推", "平板卧推"), groups[0].second)
    }

    @Test
    fun colorGroupsIgnoresSinglesAndUncolored() {
        val a = ex("上斜卧推", color = 1)
        val b = ex("平板卧推", color = 1)
        val single = ex("飞鸟", color = 2)
        val plain = ex("深蹲")
        val groups = WorkoutLogic.colorGroups(listOf(a, b, single, plain))
        assertEquals(1, groups.size)
        assertEquals(1, groups[0].first)
        assertEquals(2, groups[0].second.size)
    }

    @Test
    fun pickFallsBackWhenChosenVanishes() {
        val a = ex("上斜卧推", color = 1)
        val kept = WorkoutLogic.pickAlternatives(listOf(a), mapOf(1 to "已经删掉的动作"))
        assertEquals(listOf("上斜卧推"), kept.map { it.name })
    }

    @Test
    fun progressIsKeptWhenSwitchingAlternative() {
        val a = ex("上斜卧推", sets = 4, color = 1)
        val b = ex("平板卧推", sets = 4, color = 1)
        val done = WorkoutLogic.entriesOf(listOf(a, b), emptyList(), mapOf(1 to b.name))
            .map { it.copy(doneSets = 2) }
        val switched = WorkoutLogic.entriesOf(listOf(a, b), done, mapOf(1 to a.name))
        assertEquals(1, switched.size)
        assertEquals("上斜卧推", switched[0].name)
        assertEquals(0, switched[0].doneSets)
        assertEquals(4, switched[0].sets)
        assertTrue(switched[0].key == a.key)
    }

    @Test
    fun libraryDedupesSortsAndKeepsWeightVariants() {
        val plan = WorkoutPlan(
            id = "1",
            part = "胸",
            title = "胸日",
            exercises = listOf(
                ex("卧推", weight = 70.0),
                ex("飞鸟", weight = 10.0),
                ex("卧推", weight = 60.0),
            ),
        )
        val library = WorkoutLogic.exerciseLibrary(listOf(plan))
        assertEquals(3, library.size)
        assertEquals(listOf("卧推", "卧推", "飞鸟"), library.map { it.name })
        assertEquals(listOf(60.0, 70.0), library.filter { it.name == "卧推" }.map { it.weight })
    }
}
