package com.deepseek.dailytasks

import com.deepseek.dailytasks.data.Exercise
import com.deepseek.dailytasks.data.WorkoutLogic
import com.deepseek.dailytasks.data.romanMark
import com.deepseek.dailytasks.data.variantMark
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** v1.17：备选组直接按序号 ①②③ 标记，模板第二层改用罗马数字。 */
class V117FeaturesTest {

    private fun exercise(
        name: String,
        sets: Int = 3,
        reps: String = "8",
        weight: Double = 0.0,
        color: Int = 0,
    ) = Exercise(name = name, sets = sets, reps = reps, weight = weight, color = color)

    // ---------------- 序号 ①②③ ----------------

    @Test
    fun variantMarksAreCircledNumbers() {
        assertEquals("①", variantMark(0))
        assertEquals("②", variantMark(1))
        assertEquals("⑩", variantMark(9))
    }

    @Test
    fun marksFollowGroupNumberNotIndex() {
        val marks = WorkoutLogic.groupMarks(
            listOf(
                exercise("卧推", color = 2),
                exercise("哑铃卧推", color = 2),
                exercise("深蹲", color = 1),
                exercise("腿举", color = 1),
            ),
        )
        assertEquals("②", marks["卧推"])
        assertEquals("②", marks["哑铃卧推"])
        assertEquals("①", marks["深蹲"])
        assertEquals("①", marks["腿举"])
    }

    @Test
    fun singleMemberGroupHasNoMark() {
        val marks = WorkoutLogic.groupMarks(listOf(exercise("卧推", color = 5), exercise("飞鸟", color = 0)))
        assertTrue(marks.isEmpty())
    }

    @Test
    fun altGroupsAreSortedByGroupNumber() {
        val groups = WorkoutLogic.altGroups(
            listOf(
                exercise("深蹲", color = 2),
                exercise("腿举", color = 2),
                exercise("卧推", color = 1),
                exercise("哑铃卧推", color = 1),
            ),
        )
        assertEquals(listOf(1, 2), groups.map { it.first })
        assertEquals("卧推", groups[0].second.first().name)
    }

    // ---------------- 罗马数字（模板第二层） ----------------

    @Test
    fun romanMarksStartAtOne() {
        assertEquals("Ⅰ", romanMark(0))
        assertEquals("Ⅱ", romanMark(1))
        assertEquals("Ⅳ", romanMark(3))
        assertEquals("Ⅻ", romanMark(11))
    }

    @Test
    fun romanMarkFallsBackBeyondList() {
        assertEquals("(13)", romanMark(12))
    }
}
