package com.deepseek.dailytasks.data

import java.time.LocalDate
import java.util.Locale

/**
 * 一条任务。
 *
 * @param startMinutes 从当天 0 点起的分钟数（0..1439）
 * @param priority 必要级：2 = 必要，1 = 重要，0 = 普通
 * @param repeatEveryDays 0 = 一次性（直到勾完才消失）；1 = 每天；n = 每 n 天一次
 * @param anchorEpochDay 重复周期的基准日（创建当天），用来算"隔几天"的相位
 * @param categoryId 分类 id，空字符串表示未分类
 * @param finishedEpochDay 一次性任务被勾完的那天，用来让它在第二天自动消失
 * @param skipEpochDay 被推迟掉的那一天（0 = 没有），这一天不再显示
 * @param showEpochDay 被推迟到的那一天（0 = 没有），这一天额外显示一次
 */
data class Task(
    val id: Long,
    val title: String,
    val startMinutes: Int,
    val note: String = "",
    val remind: Boolean = true,
    val repeatEveryDays: Int = 1,
    val anchorEpochDay: Long = 0L,
    val categoryId: String = "",
    val finishedEpochDay: Long? = null,
    val priority: Int = PRIORITY_NORMAL,
    val skipEpochDay: Long = 0L,
    val showEpochDay: Long = 0L,
) {
    val timeText: String
        get() = formatMinutes(startMinutes)

    val repeatLabel: String
        get() = when {
            repeatEveryDays <= 0 -> "一次性"
            repeatEveryDays == 1 -> "每天"
            else -> "每${repeatEveryDays}天"
        }

    val priorityLabel: String
        get() = priorityLabelOf(priority)

    /** 这一天要不要出现在清单里。 */
    fun isVisibleOn(date: LocalDate): Boolean {
        val day = date.toEpochDay()
        // 推迟：被推迟掉的那天不显示；推迟到的第二天额外显示一次（勾了就不显示）
        if (skipEpochDay != 0L && day == skipEpochDay) return finishedEpochDay == day
        if (showEpochDay != 0L && day == showEpochDay) return finishedEpochDay == null || finishedEpochDay == day
        if (repeatEveryDays <= 0) {
            // 一次性任务：做好之前一直挂着，勾完当天还显示（划线），第二天自动隐藏
            if (finishedEpochDay == null) return true
            if (finishedEpochDay == day) return true
            // 还没到创建日（理论上不会发生）时不显示
            return false
        }
        if (anchorEpochDay > day) return false
        val diff = day - anchorEpochDay
        return diff % repeatEveryDays == 0L
    }

    /** 从 [from] 起（含）第一次出现的日期，最多往后找 [maxDays] 天。 */
    fun nextVisibleDate(from: LocalDate, maxDays: Long = 90): LocalDate? {
        var date = from
        var i = 0L
        while (i <= maxDays) {
            if (isVisibleOn(date)) return date
            date = date.plusDays(1)
            i++
        }
        return null
    }

    companion object {
        /** 普通 */
        const val PRIORITY_NORMAL = 0

        /** 重要 */
        const val PRIORITY_IMPORTANT = 1

        /** 必要（必须做） */
        const val PRIORITY_NECESSARY = 2

        /** 从高到低，供选择器使用 */
        val PRIORITY_LEVELS = listOf(PRIORITY_NECESSARY, PRIORITY_IMPORTANT, PRIORITY_NORMAL)

        fun priorityLabelOf(level: Int): String = when (level) {
            PRIORITY_NECESSARY -> "必要"
            PRIORITY_IMPORTANT -> "重要"
            else -> "普通"
        }

        fun formatMinutes(minutes: Int): String {
            val safe = minutes.coerceIn(0, 24 * 60 - 1)
            return String.format(Locale.US, "%02d:%02d", safe / 60, safe % 60)
        }
    }
}

/** 列表展示顺序：必要级高的在前，同级别按开始时间。 */
fun sortTasksForDisplay(tasks: List<Task>): List<Task> =
    tasks.sortedWith(compareByDescending<Task> { it.priority }.thenBy { it.startMinutes })

/** 关键词匹配：名称 / 备注 / 分类名都能搜到。 */
fun Task.matchesQuery(query: String, categoryName: String?): Boolean {
    val q = query.trim()
    if (q.isEmpty()) return true
    return title.contains(q, ignoreCase = true) ||
        note.contains(q, ignoreCase = true) ||
        (categoryName ?: "").contains(q, ignoreCase = true)
}
