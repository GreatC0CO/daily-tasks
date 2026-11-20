package com.deepseek.dailytasks.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/** 身体数据（体重 / 三围）的本地存储。 */
class BodyStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadRecords(): List<BodyRecord> {
        val raw = prefs.getString(KEY_RECORDS, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val date = runCatching { LocalDate.parse(o.optString("date")) }.getOrNull()
                        ?: continue
                    val record = BodyRecord(
                        date = date,
                        weightKg = o.optNullableDouble("weight"),
                        armCm = o.optNullableDouble("arm"),
                        waistCm = o.optNullableDouble("waist"),
                        chestCm = o.optNullableDouble("chest"),
                    )
                    if (!record.isBlank) add(record)
                }
            }.sortedBy { it.date }
        }.getOrDefault(emptyList())
    }

    fun saveRecords(records: List<BodyRecord>) {
        val arr = JSONArray()
        records.sortedBy { it.date }.forEach { record ->
            arr.put(
                JSONObject().apply {
                    put("date", record.date.toString())
                    record.weightKg?.let { put("weight", it) }
                    record.armCm?.let { put("arm", it) }
                    record.waistCm?.let { put("waist", it) }
                    record.chestCm?.let { put("chest", it) }
                },
            )
        }
        prefs.edit().putString(KEY_RECORDS, arr.toString()).apply()
    }

    /** 某一天的记录（没有就返回 null）。 */
    fun recordOf(records: List<BodyRecord>, date: LocalDate): BodyRecord? =
        records.firstOrNull { it.date == date }

    private fun JSONObject.optNullableDouble(key: String): Double? =
        if (!has(key) || isNull(key)) null else optDouble(key)

    companion object {
        private const val PREFS_NAME = "body_metrics"
        private const val KEY_RECORDS = "records_json"
    }
}
