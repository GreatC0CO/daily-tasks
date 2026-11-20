package com.deepseek.dailytasks

import com.deepseek.dailytasks.data.Exercise
import com.deepseek.dailytasks.data.SessionEntry
import com.deepseek.dailytasks.data.WorkoutLogic
import com.deepseek.dailytasks.data.WorkoutSession
import com.deepseek.dailytasks.data.WorkoutTemplate
import com.deepseek.dailytasks.data.formatVolume
import com.deepseek.dailytasks.data.formatWeight
import com.deepseek.dailytasks.data.repsCount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 重量 / 训练容量相关逻辑。 */
class WeightAndVolumeTest {

    private fun entry(
        name: String = "卧推",
        sets: Int = 4,
        reps: String = "8",
        done: Int = 2,
        weight: Double = 60.0,
    ) = SessionEntry(name = name, sets = sets, reps = reps, doneSets = done, weight = weight)

    @Test
    fun entriesOfCarriesWeightFromPlan() {
        val planExercises = listOf(
            Exercise("卧推", 4, "8", 60.0),
            Exercise("飞鸟", 3, "12", 12.5),
            Exercise("卷腹", 3, "20"),
        )
        val entries = WorkoutLogic.entriesOf(planExercises)
        assertEquals(60.0, entries[0].weight, 0.001)
        assertEquals(12.5, entries[1].weight, 0.001)
        assertEquals(0.0, entries[2].weight, 0.001)
        assertTrue(entries[0].hasWeight)
        assertFalse(entries[2].hasWeight)
    }

    @Test
    fun volumeIsDoneSetsTimesRepsTimesWeight() {
        // 2 组 × 8 次 × 60kg = 960
        assertEquals(960.0, entry().volumeKg, 0.001)
        // 没记重量的动作容量是 0
        assertEquals(0.0, entry(weight = 0.0).volumeKg, 0.001)
        // 一组都没做也是 0
        assertEquals(0.0, entry(done = 0).volumeKg, 0.001)
        assertEquals("960kg", entry().volumeText)
        assertEquals("—", entry(weight = 0.0).volumeText)
    }

    @Test
    fun repsCountReadsFirstNumber() {
        assertEquals(8, repsCount("8"))
        assertEquals(8, repsCount("8-10"))
        assertEquals(12, repsCount("12 次"))
        assertEquals(45, repsCount("45 秒"))
        assertEquals(0, repsCount("力竭"))
    }

    @Test
    fun weightTextHidesTrailingZero() {
        assertEquals("60kg", formatWeight(60.0))
        assertEquals("22.5kg", formatWeight(22.5))
        assertEquals("0.5kg", formatWeight(0.5))
        assertEquals("—", formatVolume(0.0))
        assertEquals("1,240kg", formatVolume(1240.0))
    }

    @Test
    fun setWeightOnlyAffectsTodayEntry() {
        val updated = WorkoutLogic.setWeight(entry(), 70.0)
        assertEquals(70.0, updated.weight, 0.001)
        // 负数归 0，过大归 1000
        assertEquals(0.0, WorkoutLogic.setWeight(entry(), -5.0).weight, 0.001)
        assertEquals(1000.0, WorkoutLogic.setWeight(entry(), 5000.0).weight, 0.001)
        // 其它字段不变
        assertEquals(2, updated.done)
        assertEquals(4, updated.sets)
    }

    @Test
    fun sessionSummaryShowsVolumeWhenWeighted() {
        val weighted = WorkoutSession(
            id = 1L,
            epochDay = 20000L,
            parts = listOf("胸"),
            entries = listOf(entry(done = 4)),
            atMillis = 1L,
        )
        assertEquals("胸 · 完成 4/4 组 · 容量 1,920kg", weighted.summaryText)

        val bodyweight = weighted.copy(
            entries = listOf(entry(weight = 0.0, done = 4)),
        )
        assertEquals("胸 · 完成 4/4 组", bodyweight.summaryText)
        assertEquals(0.0, bodyweight.totalVolume, 0.001)
    }

    @Test
    fun templateKeepsWeight() {
        val entries = listOf(entry(name = "深蹲", sets = 5, reps = "5", weight = 80.0))
        val template = WorkoutLogic.templateFrom(42L, "腿 日", listOf("腿"), entries)
        assertEquals(80.0, template.exercises.first().weight, 0.001)
        assertEquals("5 组 × 5 · 80kg", template.exercises.first().detailText)

        val restored = WorkoutLogic.applyTemplate(
            WorkoutTemplate(id = 1L, name = "腿 日", parts = listOf("腿"), exercises = template.exercises),
        )
        assertEquals(80.0, restored.first().weight, 0.001)
    }

    @Test
    fun addingExerciseWithWeightKeepsExistingProgress() {
        val before = listOf(entry(done = 2))
        val after = WorkoutLogic.withExercise(before, Exercise("卧推", 5, "8", 60.0))
        val row = after.first { it.name == "卧推" }
        assertEquals(5, row.sets)
        assertEquals(2, row.done)
        assertEquals(60.0, row.weight, 0.001)
    }

    @Test
    fun draftTotalVolume() {
        val two = listOf(entry(done = 2), entry(name = "飞鸟", sets = 3, reps = "12", done = 3, weight = 12.5))
        // 2×8×60 + 3×12×12.5 = 960 + 450 = 1410
        assertEquals(1410.0, WorkoutLogic.totalVolume(two), 0.001)
        assertEquals(1410.0, com.deepseek.dailytasks.data.WorkoutDraft(0L, listOf("胸"), two).totalVolume, 0.001)
    }
}
