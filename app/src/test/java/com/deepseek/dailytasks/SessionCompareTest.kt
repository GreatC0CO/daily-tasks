package com.deepseek.dailytasks

import com.deepseek.dailytasks.data.DayTotals
import com.deepseek.dailytasks.data.SessionEntry
import com.deepseek.dailytasks.data.WorkoutLogic
import com.deepseek.dailytasks.data.WorkoutSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 训练详情用到的「一天汇总」和「两天对比」：同名对齐、两天的并集、容量差与排序。
 */
class SessionCompareTest {

    private fun entry(
        name: String,
        sets: Int = 3,
        reps: String = "8",
        weight: Double = 60.0,
        done: Int = 0,
        color: Int = 0,
    ) = SessionEntry(
        name = name,
        sets = sets,
        reps = reps,
        doneSets = done,
        weight = weight,
        color = color,
    )

    private fun session(name: String, entries: List<SessionEntry>): WorkoutSession =
        WorkoutSession(
            id = name.hashCode().toLong(),
            epochDay = 20000L,
            parts = listOf("胸"),
            entries = entries,
            atMillis = 0L,
        )

    @Test
    fun dayTotalsMergesSameNameDifferentWeights() {
        val totals = WorkoutLogic.dayTotals(
            listOf(entry("卧推", sets = 3, weight = 60.0), entry("卧推", sets = 4, weight = 70.0)),
        )
        val row = totals.getValue("卧推")
        assertEquals(7, row.sets)
        assertEquals("60kg/70kg", row.weightText)
    }

    @Test
    fun dayTotalsUsesSelfWeightWhenNoWeight() {
        val row = WorkoutLogic.dayTotals(listOf(entry("引体向上", weight = 0.0))).getValue("引体向上")
        assertEquals("自重", row.weightText)
    }

    @Test
    fun compareListsUnionOfBothDays() {
        val a = session("a", listOf(entry("卧推", sets = 3)))
        val b = session("b", listOf(entry("卧推", sets = 4), entry("飞鸟", sets = 3, weight = 10.0)))
        val rows = WorkoutLogic.compareSessions(a, b)
        assertEquals(2, rows.size)
        val bench = rows.first { it.name == "卧推" }
        assertEquals(3, bench.a.sets)
        assertEquals(4, bench.b.sets)
        val fly = rows.first { it.name == "飞鸟" }
        assertFalse(fly.hasA)
        assertTrue(fly.hasB)
    }

    @Test
    fun missingSideShowsPlaceholderText() {
        val a = session("a", listOf(entry("卧推", sets = 3)))
        val rows = WorkoutLogic.compareSessions(a, session("b", emptyList()))
        val bench = rows.first()
        assertEquals("—", bench.b.cellText)
        assertEquals("本日没有", bench.deltaText)
    }

    @Test
    fun newExerciseDeltaText() {
        val rows = WorkoutLogic.compareSessions(
            session("a", emptyList()),
            session("b", listOf(entry("飞鸟", sets = 3, weight = 10.0, done = 3))),
        )
        assertEquals("新增 240kg", rows.first().deltaText)
    }

    @Test
    fun deltaTextSigns() {
        val a = session("a", listOf(entry("卧推", sets = 3, weight = 60.0, done = 3)))
        val plus = WorkoutLogic.compareSessions(a, session("b", listOf(entry("卧推", sets = 4, weight = 60.0, done = 4))))
        assertEquals("+480kg", plus.first().deltaText)

        val same = WorkoutLogic.compareSessions(a, session("c", listOf(entry("卧推", sets = 3, weight = 60.0, done = 3))))
        assertEquals("持平", same.first().deltaText)
    }

    @Test
    fun sortedByAbsoluteVolumeChange() {
        val a = session(
            "a",
            listOf(entry("卧推", sets = 3, weight = 60.0, done = 3), entry("飞鸟", sets = 3, weight = 10.0, done = 3)),
        )
        val b = session(
            "b",
            listOf(entry("卧推", sets = 5, weight = 60.0, done = 5), entry("飞鸟", sets = 3, weight = 10.0, done = 1)),
        )
        val rows = WorkoutLogic.compareSessions(a, b)
        // 卧推变化 960，飞鸟变化 -480，绝对值大的排前面
        assertEquals("卧推", rows[0].name)
        assertEquals("飞鸟", rows[1].name)
    }

    @Test
    fun selfWeightHasZeroVolume() {
        val row = WorkoutLogic.dayTotals(listOf(entry("引体向上", weight = 0.0, done = 3))).getValue("引体向上")
        assertEquals(0.0, row.volume, 0.0001)
    }

    @Test
    fun colorIsCarriedIntoCompareRow() {
        val a = session("a", listOf(entry("卧推", color = 2)))
        val rows = WorkoutLogic.compareSessions(a, null)
        assertEquals(2, rows.first().color)
    }

    @Test
    fun totalsReflectSessionSummary() {
        val s = session("s", listOf(entry("卧推", sets = 3, done = 2), entry("飞鸟", sets = 3, weight = 10.0, done = 3)))
        assertEquals(5, s.totalDone)
        assertEquals(6, s.totalPlanned)
        assertEquals(2 * 8 * 60.0 + 3 * 8 * 10.0, s.totalVolume, 0.0001)
    }

    @Test
    fun emptyDayHasNoRows() {
        assertTrue(WorkoutLogic.compareSessions(null, null).isEmpty())
        assertEquals("—", DayTotals(0, 0, 0.0, "—", "—").cellText)
    }
}
