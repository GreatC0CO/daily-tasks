package com.deepseek.dailytasks.data

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * 一件消耗品（洗面奶、牙膏、米、洗衣液……）。
 *
 * @param type 用户自己填的类型，比如「洗护」「清洁」「食品」
 * @param startDate 开始使用的时间
 * @param endDate 用完的时间；null 表示还在用
 * @param quantity 这一件的用量（配合 unit，如 500 毫升、1 支）
 * @param price 每次购买的价格（元）
 * @param totalAmount 总量（可选，和用量同一个单位），填了才能算消耗比例
 * @param remainingAmount 剩余量（可选）；和「已用天数」一样是"用的进度"的另一种说法
 * @param metricMode 列表里优先显示哪个：0 = 已用天数，1 = 剩余量（点一下可以互换）
 */
data class Consumable(
    val id: Long,
    val name: String,
    val type: String = "",
    val startDate: LocalDate = LocalDate.now(),
    val endDate: LocalDate? = null,
    val quantity: Double = 1.0,
    val unit: String = "个",
    val price: Double = 0.0,
    val note: String = "",
    val totalAmount: Double? = null,
    val remainingAmount: Double? = null,
    val metricMode: Int = MODE_DAYS,
    /** 每次购买的价格记录（时间顺序，最后一条是最近一次）。 */
    val priceHistory: List<PricePoint> = emptyList(),
) {
    val isUsing: Boolean get() = endDate == null

    /** 用了多少天（还在用就算到今天）。 */
    val daysUsed: Long
        get() = ChronoUnit.DAYS.between(startDate, endDate ?: LocalDate.now()).coerceAtLeast(0)

    val typeLabel: String get() = type.ifBlank { UNCATEGORIZED }

    val quantityText: String get() = trimNumber(quantity) + unit

    val priceText: String get() = "¥" + String.format(Locale.US, "%.2f", price)

    val dateRangeText: String
        get() = if (endDate == null) {
            "${startDate.format(FMT)} 开始 · 还在用"
        } else {
            "${startDate.format(FMT)} → ${endDate.format(FMT)}"
        }

    // ---------------- 剩余量 / 已用天数（可互换的同类项） ----------------

    /** 有没有填剩余量。 */
    val hasAmount: Boolean get() = remainingAmount != null

    /** 已经消耗掉多少 = 总量 − 剩余量（没填总量时返回 null）。 */
    val consumedAmount: Double?
        get() {
            val total = totalAmount ?: return null
            val remain = remainingAmount ?: return null
            return (total - remain).coerceAtLeast(0.0)
        }

    /** 剩余比例 0..1；没填总量或总量为 0 时返回 null。 */
    val remainingRatio: Float?
        get() {
            val total = totalAmount ?: return null
            val remain = remainingAmount ?: return null
            if (total <= 0.0) return null
            return (remain / total).coerceIn(0.0, 1.0).toFloat()
        }

    val remainingText: String
        get() = remainingAmount?.let { trimNumber(it) + unit } ?: "未填"

    val consumedText: String
        get() = consumedAmount?.let { trimNumber(it) + unit } ?: "—"

    /** 主显示：按当前模式显示天数和剩余量中的一个。 */
    val metricMainText: String
        get() = if (metricMode == MODE_AMOUNT) {
            "剩 $remainingText" + (remainingRatio?.let { "（${(it * 100).toInt()}%）" } ?: "")
        } else {
            "已用 $daysUsed 天"
        }

    /** 副显示：另一个同类项，方便一眼对照。 */
    val metricSubText: String
        get() = if (metricMode == MODE_AMOUNT) {
            "已用 $daysUsed 天"
        } else if (hasAmount) {
            "剩 $remainingText" + (remainingRatio?.let { "（${(it * 100).toInt()}%）" } ?: "")
        } else {
            "已消耗 $consumedText"
        }

    /** 互换显示模式：天数 ⇄ 剩余量。 */
    fun toggleMetric(): Consumable = copy(metricMode = if (metricMode == MODE_AMOUNT) MODE_DAYS else MODE_AMOUNT)

    // ---------------- 价格记录 ----------------

    val priceRecordText: String
        get() = if (priceHistory.isEmpty()) "暂无记录" else "${priceHistory.size} 次购买"

    /** 历史平均价；没有历史时就是当前价。 */
    val averagePrice: Double
        get() = if (priceHistory.isEmpty()) price else priceHistory.map { it.price }.average()

    /** 最近一次相对上一次是涨还是跌；没有对比返回 null。 */
    val lastPriceDelta: Double?
        get() = if (priceHistory.size < 2) null else priceHistory.last().price - priceHistory[priceHistory.size - 2].price

    /** 这件东西累计花了多少钱：把每次购买的价格加总（没记录过购买时按当前价格算一次）。 */
    val totalSpent: Double
        get() = if (priceHistory.isEmpty()) price else priceHistory.sumOf { it.price }

    val totalSpentText: String get() = "¥" + String.format(Locale.US, "%.2f", totalSpent)

    /** 买过几次（没有记录时，价格 > 0 算 1 次）。 */
    val purchaseCount: Int
        get() = if (priceHistory.isEmpty()) (if (price > 0.0) 1 else 0) else priceHistory.size

    /**
     * 记录一次购买价：价格没变就不新增记录，变了就追加一条（日期记为今天）。
     */
    fun withPrice(recordedOn: LocalDate, newPrice: Double): Consumable {
        if (newPrice <= 0.0) return copy(price = 0.0)
        val last = priceHistory.lastOrNull()
        return if (last == null || last.price != newPrice) {
            copy(price = newPrice, priceHistory = priceHistory + PricePoint(recordedOn.toEpochDay(), newPrice))
        } else {
            copy(price = newPrice)
        }
    }

    /**
     * 记录一次新的购买（「＋ 记一笔」走这里）：价格追加进价格历史、购买次数 +1，
     * 数量累加到用量上；如果这件之前已经标记为用完，就重新开始使用。
     */
    fun withPurchase(recordedOn: LocalDate, newPrice: Double, amount: Double): Consumable {
        val newTotal = (totalAmount ?: quantity) + amount
        val newRemaining = when {
            endDate == null -> (remainingAmount ?: quantity) + amount
            else -> amount
        }
        val freshPrice = if (newPrice > 0.0) newPrice else price
        // 只有真填了价格才追加一条价格记录（价格 0 视为「不知道多少钱」，数量照加）
        val history = if (newPrice > 0.0) {
            priceHistory + PricePoint(recordedOn.toEpochDay(), newPrice)
        } else {
            priceHistory
        }
        return copy(
            price = freshPrice,
            priceHistory = history,
            quantity = newTotal,
            totalAmount = newTotal,
            remainingAmount = newRemaining,
            startDate = if (endDate != null) recordedOn else startDate,
            endDate = null,
        )
    }

    companion object {
        const val UNCATEGORIZED = "未分类"
        const val MODE_DAYS = 0
        const val MODE_AMOUNT = 1
        val FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/M/d", Locale.CHINA)

        /** 1.0 -> "1"，1.5 -> "1.5"，不显示多余的零。 */
        fun trimNumber(value: Double): String {
            if (value == value.toLong().toDouble()) return value.toLong().toString()
            return String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
        }
    }
}

/** 一次购买价格的记录点：[epochDay] 是购买当天，[price] 是当时的价格（元）。 */
data class PricePoint(val epochDay: Long, val price: Double) {
    val date: LocalDate get() = LocalDate.ofEpochDay(epochDay)

    val priceText: String get() = "¥" + String.format(Locale.US, "%.2f", price)
}
