package com.deepseek.dailytasks

import com.deepseek.dailytasks.data.Task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class TaskVisibilityTest {

    private val day0 = LocalDate.of(2026, 3, 1)

    private fun task(
        repeat: Int,
        anchor: Long = day0.toEpochDay(),
        finished: Long? = null,
    ) = Task(
        id = 1L,
        title = "测试任务",
        startMinutes = 9 * 60,
        repeatEveryDays = repeat,
        anchorEpochDay = anchor,
        finishedEpochDay = finished,
    )

    @Test
    fun dailyTaskIsVisibleEveryDay() {
        val t = task(repeat = 1)
        assertTrue(t.isVisibleOn(day0))
        assertTrue(t.isVisibleOn(day0.plusDays(1)))
        assertTrue(t.isVisibleOn(day0.plusDays(100)))
    }

    @Test
    fun everyThreeDaysTask() {
        val t = task(repeat = 3)
        assertTrue(t.isVisibleOn(day0))
        assertFalse(t.isVisibleOn(day0.plusDays(1)))
        assertFalse(t.isVisibleOn(day0.plusDays(2)))
        assertTrue(t.isVisibleOn(day0.plusDays(3)))
        assertTrue(t.isVisibleOn(day0.plusDays(6)))
        // 基准日之前不出现
        assertFalse(t.isVisibleOn(day0.minusDays(1)))
    }

    @Test
    fun oneOffTaskStaysUntilFinished() {
        val t = task(repeat = 0)
        assertTrue(t.isVisibleOn(day0))
        assertTrue(t.isVisibleOn(day0.plusDays(5)))
    }

    @Test
    fun oneOffTaskDisappearsAfterFinishDay() {
        val finishedOn = day0.plusDays(2).toEpochDay()
        val t = task(repeat = 0, finished = finishedOn)
        assertFalse(t.isVisibleOn(day0))
        assertTrue(t.isVisibleOn(day0.plusDays(2)))
        assertFalse(t.isVisibleOn(day0.plusDays(3)))
    }

    @Test
    fun nextVisibleDateSkipsRestDays() {
        val t = task(repeat = 3)
        val d1 = day0.plusDays(1)
        assertEquals(day0.plusDays(3), t.nextVisibleDate(d1))
        // 一次性任务永远能找到日期
        val once = task(repeat = 0)
        assertEquals(d1, once.nextVisibleDate(d1))
        // 已经完成的那个一次性任务，往后找不到
        val doneOnce = task(repeat = 0, finished = day0.toEpochDay())
        assertNull(doneOnce.nextVisibleDate(day0.plusDays(1), maxDays = 30))
    }

    @Test
    fun repeatLabels() {
        assertEquals("一次性", task(0).repeatLabel)
        assertEquals("每天", task(1).repeatLabel)
        assertEquals("每3天", task(3).repeatLabel)
        assertEquals("每14天", task(14).repeatLabel)
    }

    @Test
    fun timeTextIsPadded() {
        assertEquals("09:05", Task.formatMinutes(9 * 60 + 5))
        assertEquals("00:00", Task.formatMinutes(0))
        assertEquals("23:59", Task.formatMinutes(23 * 60 + 59))
    }
}
