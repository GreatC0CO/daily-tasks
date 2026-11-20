package com.deepseek.dailytasks.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/** 任务打勾的流水记录，用来查询"以往哪天做完了什么"。 */
class HistoryStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadAll(): List<TaskRecord> {
        val raw = prefs.getString(KEY_RECORDS, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val title = o.optString("title")
                    if (title.isBlank()) continue
                    add(
                        TaskRecord(
                            taskId = o.optLong("taskId"),
                            epochDay = o.optLong("epochDay"),
                            title = title,
                            categoryName = o.optString("category"),
                            startMinutes = o.optInt("start"),
                            priority = o.optInt("priority", Task.PRIORITY_NORMAL),
                            atMillis = o.optLong("at"),
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun saveAll(records: List<TaskRecord>) {
        val arr = JSONArray()
        records.forEach { r ->
            arr.put(
                JSONObject().apply {
                    put("taskId", r.taskId)
                    put("epochDay", r.epochDay)
                    put("title", r.title)
                    put("category", r.categoryName)
                    put("start", r.startMinutes)
                    put("priority", r.priority)
                    put("at", r.atMillis)
                }
            )
        }
        prefs.edit().putString(KEY_RECORDS, arr.toString()).apply()
    }

    /** 勾上任务时记一笔（同一任务同一天只留一条）。 */
    fun record(record: TaskRecord) {
        saveAll(HistoryQuery.upsert(loadAll(), record))
    }

    /** 取消勾选时把这条记录删掉。 */
    fun remove(taskId: Long, epochDay: Long) {
        val all = loadAll()
        val next = HistoryQuery.remove(all, taskId, epochDay)
        if (next.size != all.size) saveAll(next)
    }

    /** 查询过往记录：关键词 + 时间范围（epochDay 传默认值表示不限）。 */
    fun query(
        keyword: String,
        fromEpochDay: Long = Long.MIN_VALUE,
        toEpochDay: Long = Long.MAX_VALUE,
    ): List<TaskRecord> = HistoryQuery.filter(loadAll(), keyword, fromEpochDay, toEpochDay)

    /** 只保留最近 [keepDays] 天的流水，避免无限增长。 */
    fun prune(today: LocalDate = LocalDate.now(), keepDays: Long = 365) {
        val threshold = today.minusDays(keepDays).toEpochDay()
        val all = loadAll()
        val next = all.filter { it.epochDay >= threshold }
        if (next.size != all.size) saveAll(next)
    }

    private companion object {
        const val PREFS_NAME = "task_history"
        const val KEY_RECORDS = "records_json"
    }
}
