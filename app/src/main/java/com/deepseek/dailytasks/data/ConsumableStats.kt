package com.deepseek.dailytasks.data

/** 饼图统计口径。 */
enum class StatMetric(val label: String) {
    AMOUNT("金额"),
    QUANTITY("数量"),
    COUNT("次数"),
}

/** 饼图的一块：某个类型占多少。 */
data class StatSlice(
    val label: String,
    val value: Double,
    val count: Int,
    val colorArgb: Long,
)

/** 和分类界面一致的配色，保证同一类型颜色稳定。 */
internal val STAT_PALETTE = listOf(
    0xFF2E6BE6L,
    0xFF2FA84FL,
    0xFFF0A020L,
    0xFFD64545L,
    0xFF8E5BEFL,
    0xFF12A0A8L,
    0xFFE0609BL,
    0xFF6E7B8BL,
)

/**
 * 把消耗品按"类型"聚合成饼图数据，并按所选口径排序。
 * 值为 0 的类型不会出现在饼图里（否则扇区无从绘制）。
 */
fun buildStatSlices(items: List<Consumable>, metric: StatMetric): List<StatSlice> {
    val grouped = LinkedHashMap<String, MutableList<Consumable>>()
    items.forEach { item ->
        grouped.getOrPut(item.typeLabel) { mutableListOf() }.add(item)
    }
    return grouped.entries
        .map { (label, list) ->
            val value = when (metric) {
                StatMetric.AMOUNT -> list.sumOf { it.totalSpent }
                StatMetric.QUANTITY -> list.sumOf { it.quantity }
                StatMetric.COUNT -> list.size.toDouble()
            }
            StatSlice(label = label, value = value, count = list.size, colorArgb = 0L)
        }
        .filter { it.value > 0.0 }
        .sortedByDescending { it.value }
        .mapIndexed { index, slice ->
            slice.copy(colorArgb = STAT_PALETTE[index % STAT_PALETTE.size])
        }
}
