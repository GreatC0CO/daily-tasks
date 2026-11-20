package com.deepseek.dailytasks

import com.deepseek.dailytasks.data.Exercise
import com.deepseek.dailytasks.data.DefaultPlans
import com.deepseek.dailytasks.data.HistoryQuery
import com.deepseek.dailytasks.data.SessionEntry
import com.deepseek.dailytasks.data.Task
import com.deepseek.dailytasks.data.TaskRecord
import com.deepseek.dailytasks.data.WorkoutLogic
import com.deepseek.dailytasks.data.WorkoutParts
import com.deepseek.dailytasks.data.WorkoutPlan
import com.deepseek.dailytasks.data.WorkoutSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class WorkoutAndHistoryTest {

    // ---------------- 锻炼：部位 → 计划匹配 ----------------

    @Test
    fun matchesPlansForSelectedPartsInOrder() {
        val plans = DefaultPlans.all()
        val matched = WorkoutLogic.matchPlans(plans, listOf("背", "胸"))
        assertEquals(2, matched.size)
        assertEquals("背", matched[0].part)
        assertEquals("胸", matched[1].part)
    }

    @Test
    fun unknownPartMatchesNothing() {
        assertTrue(WorkoutLogic.matchPlans(DefaultPlans.all(), listOf("脖子")).isEmpty())
    }

    @Test
    fun mergeDedupesExercisesAndKeepsBiggerSets() {
        val a = WorkoutPlan("a", "胸", "胸 日", listOf(Exercise("卧推", 4, "8"), Exercise("俯卧撑", 3, "15")))
        val b = WorkoutPlan("b", "手臂", "手臂 日", listOf(Exercise("卧推", 5, "5"), Exercise("弯举", 3, "12")))
        val merged = WorkoutLogic.mergeExercises(listOf(a, b))
        // v1.7 起动作身份 = 名称 + 次数 + 重量：同名但 8 次和 5 次是两个动作，都要保留
        assertEquals(4, merged.size)
        assertEquals(2, merged.count { it.name == "卧推" })
        assertTrue(merged.first { it.reps == "5" }.sets == 5)
        // 完全相同的动作才会合并成一条
        val same = listOf(
            WorkoutPlan("a", "胸", "胸 日", listOf(Exercise("卧推", 4, "8"))),
            WorkoutPlan("b", "手臂", "手臂 日", listOf(Exercise("卧推", 5, "8"))),
        )
        val deduped = WorkoutLogic.mergeExercises(same)
        assertEquals(1, deduped.size)
        assertEquals(5, deduped[0].sets)
    }

    @Test
    fun singleTapAddsOneSetAndStopsAtTarget() {
        var e = SessionEntry("卧推", 3, "10")
        repeat(3) { e = WorkoutLogic.addSet(e) }
        assertEquals(3, e.done)
        assertTrue(e.isComplete)
        // 已经满了，再点不变
        e = WorkoutLogic.addSet(e)
        assertEquals(3, e.done)
    }

    @Test
    fun changingPartsKeepsPreviousProgress() {
        val before = listOf(
            SessionEntry("卧推", 4, "8", doneSets = 2),
            SessionEntry("引体向上", 4, "8", doneSets = 1),
        )
        val exercises = listOf(Exercise("卧推", 4, "8"), Exercise("弯举", 3, "12"))
        val after = WorkoutLogic.entriesOf(exercises, before)
        assertEquals(2, after.size)
        assertEquals(2, after[0].done)   // 卧推进度保留
        assertEquals(0, after[1].done)   // 新动作从 0 开始
    }

    @Test
    fun totalsAndReset() {
        var entries = listOf(
            SessionEntry("深蹲", 5, "5", doneSets = 3),
            SessionEntry("腿举", 3, "12", doneSets = 3),
        )
        assertEquals(6, WorkoutLogic.totalDone(entries))
        assertEquals(8, WorkoutLogic.totalPlanned(entries))
        entries = entries.map { WorkoutLogic.resetSets(it) }
        assertEquals(0, WorkoutLogic.totalDone(entries))
    }

    @Test
    fun sessionSummaryAndFiltering() {
        val day = LocalDate.of(2026, 10, 20).toEpochDay()
        val sessions = listOf(
            WorkoutSession(
                id = 1L, epochDay = day, parts = listOf("胸", "背"),
                atMillis = 1L,
                entries = listOf(SessionEntry("卧推", 4, "8", 4), SessionEntry("引体向上", 4, "8", 2)),
            ),
            WorkoutSession(
                id = 2L, epochDay = day - 1, parts = listOf("腿"),
                atMillis = 2L,
                entries = listOf(SessionEntry("深蹲", 5, "5", 5)),
            ),
        )
        assertEquals("胸 + 背 · 完成 6/8 组", sessions[0].summaryText)
        // 按部位搜
        assertEquals(1, WorkoutLogic.filterSessions(sessions, "背").size)
        // 按动作搜
        assertEquals(1, WorkoutLogic.filterSessions(sessions, "深蹲").size)
        // 时间范围：只要昨天那次
        val onlyYesterday = WorkoutLogic.filterSessions(sessions, "", fromEpochDay = day - 1, toEpochDay = day - 1)
        assertEquals(listOf(2L), onlyYesterday.map { it.id })
        // 分组按日期倒序
        val groups = WorkoutLogic.groupSessionsByDate(sessions)
        assertEquals(2, groups.size)
        assertEquals(LocalDate.of(2026, 10, 20), groups[0].first)
    }

    @Test
    fun defaultPlansCoverAllParts() {
        val plans = DefaultPlans.all()
        assertEquals(WorkoutParts.ALL.size, plans.size)
        WorkoutParts.ALL.forEach { part ->
            val plan = plans.firstOrNull { it.part == part }
            assertTrue("缺少部位计划：$part", plan != null && plan.exercises.isNotEmpty())
        }
    }

    // ---------------- 历史记录查询 ----------------

    private fun rec(id: Long, day: LocalDate, title: String, category: String = "生活", start: Int = 540) =
        TaskRecord(
            taskId = id,
            epochDay = day.toEpochDay(),
            title = title,
            categoryName = category,
            startMinutes = start,
            priority = Task.PRIORITY_NORMAL,
            atMillis = 0L,
        )

    @Test
    fun historyFiltersByKeywordAndRange() {
        val d1 = LocalDate.of(2026, 10, 20)
        val d2 = LocalDate.of(2026, 10, 22)
        val all = listOf(
            rec(1, d1, "晨跑 5 公里", "运动"),
            rec(2, d1, "写周报", "工作"),
            rec(3, d2, "买牛奶", "生活"),
        )
        assertEquals(1, HistoryQuery.filter(all, "周报").size)
        assertEquals(1, HistoryQuery.filter(all, "运动").size)
        assertEquals(3, HistoryQuery.filter(all, "").size)
        val range = HistoryQuery.filter(all, "", fromEpochDay = d2.toEpochDay())
        assertEquals(1, range.size)
        assertEquals("买牛奶", range[0].title)
    }

    @Test
    fun historyGroupsByDateDescending() {
        val d1 = LocalDate.of(2026, 10, 20)
        val d2 = LocalDate.of(2026, 10, 21)
        val groups = HistoryQuery.groupByDate(listOf(rec(1, d1, "A"), rec(2, d2, "B"), rec(3, d2, "C")))
        assertEquals(2, groups.size)
        assertEquals(d2, groups[0].first)
        assertEquals(2, groups[0].second.size)
        assertEquals(d1, groups[1].first)
    }

    @Test
    fun sameTaskSameDayIsRecordedOnce() {
        val day = LocalDate.of(2026, 10, 20)
        val first = rec(9, day, "读书")
        val updated = first.copy(title = "读书（改过名）")
        var list = HistoryQuery.upsert(emptyList(), first)
        list = HistoryQuery.upsert(list, updated)
        assertEquals(1, list.size)
        assertEquals("读书（改过名）", list[0].title)
        // 取消勾选后删除
        list = HistoryQuery.remove(list, 9, day.toEpochDay())
        assertTrue(list.isEmpty())
    }

    @Test
    fun removingOtherDayKeepsRecord() {
        val d1 = LocalDate.of(2026, 10, 20)
        val d2 = LocalDate.of(2026, 10, 21)
        val list = listOf(rec(5, d1, "A"), rec(5, d2, "A"))
        val after = HistoryQuery.remove(list, 5, d1.toEpochDay())
        assertEquals(1, after.size)
        assertEquals(d2.toEpochDay(), after[0].epochDay)
    }

    @Test
    fun recordFormatsTimeAndDate() {
        val r = rec(1, LocalDate.of(2026, 9, 3), "早读", start = 7 * 60 + 5)
        assertEquals("07:05", r.timeText)
        assertEquals("2026/9/3", r.dateText)
        assertFalse(r.timeText.isEmpty())
        assertNull(null)
    }
}
