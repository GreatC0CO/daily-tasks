package com.deepseek.dailytasks

import com.deepseek.dailytasks.data.SessionCategory
import com.deepseek.dailytasks.data.SessionEntry
import com.deepseek.dailytasks.data.SloganGroup
import com.deepseek.dailytasks.data.SloganLogic
import com.deepseek.dailytasks.data.WorkoutSession
import com.deepseek.dailytasks.data.categoryProgress
import com.deepseek.dailytasks.data.sessionCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** v1.21：标语按时段分组/轮换，训练记录按三大类比较进步。 */
class V121FeaturesTest {

    private fun session(id: Long, epochDay: Long, parts: List<String>, sets: Int, done: Int) =
        WorkoutSession(
            id = id,
            epochDay = epochDay,
            parts = parts,
            entries = listOf(SessionEntry("动作", sets = sets, reps = "8", doneSets = done)),
            atMillis = id,
        )

    @Test
    fun parseMinuteAcceptsClockAndCompactForms() {
        assertEquals(22 * 60, SloganLogic.parseMinute("22:00"))
        assertEquals(7 * 60 + 30, SloganLogic.parseMinute("7:30"))
        assertEquals(7 * 60, SloganLogic.parseMinute("0700"))
        assertNull(SloganLogic.parseMinute("24:00"))
        assertNull(SloganLogic.parseMinute("12:70"))
        assertNull(SloganLogic.parseMinute("abc"))
    }

    @Test
    fun groupContainsHandlesOvernightAndAllDay() {
        val night = SloganGroup(1L, 22 * 60, 7 * 60, listOf("早睡"), 30)
        assertTrue(night.contains(23 * 60))
        assertTrue(night.contains(6 * 60 + 59))
        assertFalse(night.contains(7 * 60))
        assertFalse(night.contains(12 * 60))

        val allDay = SloganGroup(2L, 0, 0, listOf("全天"), 60)
        assertTrue(allDay.contains(0))
        assertTrue(allDay.contains(13 * 60))
    }

    @Test
    fun defaultGroupsSwitchAtTheRightTime() {
        val groups = SloganLogic.defaultGroups(listOf("白天"))
        assertEquals(1L, SloganLogic.activeGroup(groups, 23 * 60)?.id)
        assertEquals(2L, SloganLogic.activeGroup(groups, 8 * 60)?.id)
        assertNull(SloganLogic.activeGroup(emptyList(), 8 * 60))
    }

    @Test
    fun sloganRotatesByCustomPeriodAndOffset() {
        val group = SloganGroup(9L, 0, 0, listOf("a", "b", "c"), 30)
        assertEquals("a", SloganLogic.sloganAt(group, 0))
        assertEquals("b", SloganLogic.sloganAt(group, 30))
        assertEquals("c", SloganLogic.sloganAt(group, 60))
        assertEquals("a", SloganLogic.sloganAt(group, 90))
        assertEquals("b", SloganLogic.sloganAt(group, 0, offset = 1))
        assertNull(SloganLogic.sloganAt(SloganGroup(1L, 0, 0, emptyList(), 30), 0))
    }

    @Test
    fun groupsJsonRoundTripAndEmptyFallsBack() {
        val groups = SloganLogic.defaultGroups(listOf("白天一", "白天二"))
        assertEquals(groups, SloganLogic.fromJson(SloganLogic.toJson(groups)))
        assertEquals(2, SloganLogic.fromJson("[]").size)
    }

    @Test
    fun sessionsSplitIntoThreeCategories() {
        assertEquals(SessionCategory.PUSH, sessionCategory(session(1, 100, listOf("胸", "三头"), 10, 10)))
        assertEquals(SessionCategory.PULL, sessionCategory(session(2, 100, listOf("背"), 10, 10)))
        assertEquals(SessionCategory.LEGS, sessionCategory(session(3, 100, listOf("臀腿"), 10, 10)))
        assertEquals(SessionCategory.OTHER, sessionCategory(session(4, 100, listOf("核心"), 10, 10)))
    }

    @Test
    fun categoryProgressComparesLatestWithPrevious() {
        val list = listOf(
            session(1, 100, listOf("腿"), 10, 5),
            session(2, 101, listOf("胸"), 10, 6),
            session(3, 102, listOf("腿"), 10, 8),
        )
        val progress = categoryProgress(list, SessionCategory.LEGS)
        assertEquals(listOf(1L, 3L), progress.sessions.map { it.id })
        assertEquals(3L, progress.latest?.id)
        assertEquals(1L, progress.previous?.id)
        assertEquals(3, progress.doneDelta)
        assertEquals("+3 组", progress.deltaText)
    }
}
