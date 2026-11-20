package com.deepseek.dailytasks.data

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 任务打勾的流水记录（用于"查询过往记录"）。
 * 勾上时写入、取消勾选时删除，同一任务同一天只会有一条。
 */
data class TaskRecord(
    val taskId: Long,
    val epochDay: Long,
    val title: String,
    val categoryName: String,
    val startMinutes: Int,
    val priority: Int,
    val atMillis: Long,
) {
    val date: LocalDate
        get() = LocalDate.ofEpochDay(epochDay)

    val dateText: String
        get() = date.format(FMT)

    val timeText: String
        get() = Task.formatMinutes(startMinutes)

    val priorityLabel: String
        get() = Task.priorityLabelOf(priority)

    companion object {
        private val FMT = DateTimeFormatter.ofPattern("yyyy/M/d", Locale.CHINA)
    }
}

/** 历史记录查询：纯函数，方便单元测试。 */
object HistoryQuery {

    /** 关键词匹配标题/分类；[fromEpochDay]、[toEpochDay] 传默认值表示不限。 */
    fun filter(
        records: List<TaskRecord>,
        keyword: String,
        fromEpochDay: Long = Long.MIN_VALUE,
        toEpochDay: Long = Long.MAX_VALUE,
    ): List<TaskRecord> {
        val q = keyword.trim()
        return records.filter { r ->
            val inRange = (fromEpochDay == Long.MIN_VALUE || r.epochDay >= fromEpochDay) &&
                (toEpochDay == Long.MAX_VALUE || r.epochDay <= toEpochDay)
            val hit = q.isEmpty() ||
                r.title.contains(q, ignoreCase = true) ||
                r.categoryName.contains(q, ignoreCase = true)
            inRange && hit
        }.sortedWith(compareByDescending<TaskRecord> { it.epochDay }.thenBy { it.startMinutes })
    }

    /** 按日期倒序分组，组内按开始时间正序。 */
    fun groupByDate(records: List<TaskRecord>): List<Pair<LocalDate, List<TaskRecord>>> =
        records.groupBy { it.epochDay }
            .toSortedMap(reverseOrder())
            .map { (day, list) -> LocalDate.ofEpochDay(day) to list.sortedBy { it.startMinutes } }

    /** 勾选 / 取消勾选后得到的新列表：同一任务同一天不会重复。 */
    fun upsert(records: List<TaskRecord>, record: TaskRecord): List<TaskRecord> =
        records.filterNot { it.taskId == record.taskId && it.epochDay == record.epochDay } + record

    fun remove(records: List<TaskRecord>, taskId: Long, epochDay: Long): List<TaskRecord> =
        records.filterNot { it.taskId == taskId && it.epochDay == epochDay }
}
