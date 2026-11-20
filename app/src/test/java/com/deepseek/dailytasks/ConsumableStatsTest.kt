package com.deepseek.dailytasks

import com.deepseek.dailytasks.data.Consumable
import com.deepseek.dailytasks.data.StatMetric
import com.deepseek.dailytasks.data.buildStatSlices
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ConsumableStatsTest {

    private fun item(
        id: Long,
        name: String,
        type: String,
        price: Double,
        quantity: Double = 1.0,
        unit: String = "个",
        start: LocalDate = LocalDate.of(2026, 1, 1),
        end: LocalDate? = null,
    ) = Consumable(
        id = id,
        name = name,
        type = type,
        startDate = start,
        endDate = end,
        quantity = quantity,
        unit = unit,
        price = price,
    )

    private val items = listOf(
        item(1, "洗面奶", "洗护", 59.9, 120.0, "毫升"),
        item(2, "牙膏", "洗护", 15.0, 1.0, "支"),
        item(3, "大米", "食品", 89.0, 5.0, "公斤"),
        item(4, "洗衣液", "清洁", 0.0),
    )

    @Test
    fun groupsByTypeAndSortsByAmount() {
        val slices = buildStatSlices(items, StatMetric.AMOUNT)
        assertEquals(2, slices.size)
        assertEquals("食品", slices[0].label)
        assertEquals(89.0, slices[0].value, 0.001)
        assertEquals("洗护", slices[1].label)
        assertEquals(74.9, slices[1].value, 0.001)
        assertEquals(2, slices[1].count)
        // 价格为 0 的类型不出现在金额饼图里
        assertTrue(slices.none { it.label == "清洁" })
    }

    @Test
    fun countMetricCountsRecords() {
        val slices = buildStatSlices(items, StatMetric.COUNT)
        assertEquals(3, slices.size)
        assertEquals(2.0, slices.first { it.label == "洗护" }.value, 0.001)
        assertTrue(slices.first { it.label == "清洁" }.value == 1.0)
    }

    @Test
    fun quantityMetricSumsQuantities() {
        val slices = buildStatSlices(items, StatMetric.QUANTITY)
        val wash = slices.first { it.label == "洗护" }
        assertEquals(121.0, wash.value, 0.001)
    }

    @Test
    fun blankTypeFallsBackToUncategorized() {
        val slices = buildStatSlices(listOf(item(9, "杂物", "", 5.0)), StatMetric.AMOUNT)
        assertEquals("未分类", slices.single().label)
    }

    @Test
    fun colorsAreDistinct() {
        val slices = buildStatSlices(items, StatMetric.AMOUNT)
        assertEquals(slices.size, slices.map { it.colorArgb }.distinct().size)
    }

    @Test
    fun finishedItemComputesDaysUsed() {
        val finished = item(
            id = 10,
            name = "护手霜",
            type = "洗护",
            price = 20.0,
            start = LocalDate.of(2026, 1, 1),
            end = LocalDate.of(2026, 1, 31),
        )
        assertEquals(30L, finished.daysUsed)
        assertTrue(!finished.isUsing)
        assertEquals("2026/1/1 → 2026/1/31", finished.dateRangeText)
    }

    @Test
    fun quantityAndPriceText() {
        val a = item(11, "米", "食品", 12.5, 2.5, "公斤")
        assertEquals("2.5公斤", a.quantityText)
        assertEquals("¥12.50", a.priceText)
        assertEquals("2", Consumable.trimNumber(2.0))
        assertEquals("2.5", Consumable.trimNumber(2.5))
    }
}
