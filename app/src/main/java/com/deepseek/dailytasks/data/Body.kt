package com.deepseek.dailytasks.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/** 每日身体数据：体重必填其一，臂围 / 腰围 / 胸围按需要填写。 */
data class BodyRecord(
    val date: LocalDate,
    val weightKg: Double? = null,
    val armCm: Double? = null,
    val waistCm: Double? = null,
    val chestCm: Double? = null,
) {
    val isBlank: Boolean
        get() = weightKg == null && armCm == null && waistCm == null && chestCm == null

    val weightText: String get() = weightKg?.let { formatValue(it) + "kg" } ?: "—"
    val armText: String get() = armCm?.let { formatValue(it) } ?: "—"
    val waistText: String get() = waistCm?.let { formatValue(it) } ?: "—"
    val chestText: String get() = chestCm?.let { formatValue(it) } ?: "—"
    val dateText: String get() = date.format(FMT)

    companion object {
        val FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/M/d", Locale.CHINA)
        val WEEK_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("M/d", Locale.CHINA)

        /** 去掉多余小数：62.0 → "62"，62.35 → "62.4"。 */
        fun formatValue(value: Double): String =
            if (value == value.toLong().toDouble()) value.toLong().toString()
            else String.format(Locale.US, "%.1f", value)

        /** 这一周从周一开始。 */
        fun weekStart(date: LocalDate): LocalDate =
            date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    }
}

/** 一周的平均体重（只统计填了体重的天数）。 */
data class WeeklyAverage(
    val weekStart: LocalDate,
    val averageWeight: Double,
    val days: Int,
) {
    val label: String get() = weekStart.format(BodyRecord.WEEK_FMT) + " 起"

    val averageText: String get() = String.format(Locale.US, "%.1f", averageWeight)
}

/** 曲线可以画的指标。 */
enum class BodyMetric(val label: String) {
    WEIGHT("体重（kg）"),
    ARM("臂围（cm）"),
    WAIST("腰围（cm）"),
    CHEST("胸围（cm）"),
}

data class BodyPoint(val date: LocalDate, val value: Double)

/** 身体数据的纯逻辑（方便单元测试）。 */
object BodyLogic {

    /** 同一天只保留一条：已存在则替换。返回值按日期升序。 */
    fun upsert(records: List<BodyRecord>, record: BodyRecord): List<BodyRecord> {
        val merged = records.filterNot { it.date == record.date } + record
        return merged.sortedBy { it.date }
    }

    fun remove(records: List<BodyRecord>, date: LocalDate): List<BodyRecord> =
        records.filterNot { it.date == date }.sortedBy { it.date }

    /** 按周（周一起）求平均体重，跳过没有体重的记录。 */
    fun weeklyAverages(records: List<BodyRecord>): List<WeeklyAverage> =
        records
            .mapNotNull { record -> record.weightKg?.let { BodyRecord.weekStart(record.date) to it } }
            .groupBy({ it.first }, { it.second })
            .map { (week, weights) ->
                WeeklyAverage(week, weights.average(), weights.size)
            }
            .sortedBy { it.weekStart }

    /** 某个指标的折线点（只含有值的记录），按日期升序。 */
    fun points(records: List<BodyRecord>, metric: BodyMetric): List<BodyPoint> =
        records
            .mapNotNull { record ->
                val value = when (metric) {
                    BodyMetric.WEIGHT -> record.weightKg
                    BodyMetric.ARM -> record.armCm
                    BodyMetric.WAIST -> record.waistCm
                    BodyMetric.CHEST -> record.chestCm
                }
                value?.let { BodyPoint(record.date, it) }
            }
            .sortedBy { it.date }

    /** 最近一条记录的日期。 */
    fun latest(records: List<BodyRecord>): BodyRecord? = records.maxByOrNull { it.date }

    /** 最近一次体重相对上一条有体重的记录的变化；不足两条返回 null。 */
    fun weightChange(records: List<BodyRecord>): Double? {
        val weights = records
            .mapNotNull { record -> record.weightKg?.let { record.date to it } }
            .sortedBy { it.first }
        if (weights.size < 2) return null
        return weights.last().second - weights[weights.size - 2].second
    }

    /** 最近 7 天有记录的平均体重。 */
    fun recentWeekAverage(records: List<BodyRecord>, today: LocalDate): Double? {
        val from = today.minusDays(6)
        val weights = records.filter { !it.date.isBefore(from) && !it.date.isAfter(today) }
            .mapNotNull { it.weightKg }
        return if (weights.isEmpty()) null else weights.average()
    }

    /** 点的最小 / 最大值，用来给曲线定纵轴。 */
    fun range(points: List<BodyPoint>): Pair<Double, Double>? {
        if (points.isEmpty()) return null
        val values = points.map { it.value }
        return values.min() to values.max()
    }

    fun changeText(change: Double?): String = when {
        change == null -> "—"
        change > 0.05 -> "+" + String.format(Locale.US, "%.1f", change) + "kg"
        change < -0.05 -> String.format(Locale.US, "%.1f", change) + "kg"
        else -> "持平"
    }
}
