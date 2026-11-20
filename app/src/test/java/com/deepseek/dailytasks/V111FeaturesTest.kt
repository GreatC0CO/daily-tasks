package com.deepseek.dailytasks

import com.deepseek.dailytasks.data.Consumable
import com.deepseek.dailytasks.data.ConsumableStore
import com.deepseek.dailytasks.data.PricePoint
import com.deepseek.dailytasks.data.StatMetric
import com.deepseek.dailytasks.data.Task
import com.deepseek.dailytasks.data.buildStatSlices
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** v1.11 的纯逻辑：购买价格历史、任意日期推迟、列表/分类两种视图。 */
class V111FeaturesTest {

    private val today = LocalDate.of(2026, 11, 11)

    @Test
    fun withPriceAppendsRecordAndUpdatesCurrentPrice() {
        val item = Consumable(id = 1, name = "洗发水", price = 59.0)
        val first = item.withPrice(today, 59.0)
        val second = first.withPrice(today.plusDays(30), 65.5)

        assertEquals(2, second.priceHistory.size)
        assertEquals(65.5, second.price, 0.001)
        assertEquals(LocalDate.of(2026, 12, 11), second.priceHistory.last().date)
    }

    @Test
    fun withPriceSameValueDoesNotDuplicateRecord() {
        val item = Consumable(id = 1, name = "牙膏", price = 12.0)
            .withPrice(today, 12.0)
        val again = item.withPrice(today.plusDays(5), 12.0)

        assertEquals(1, again.priceHistory.size)
        assertEquals(12.0, again.price, 0.001)
    }

    @Test
    fun averagePriceUsesHistoryAndLastDeltaComparesLastTwo() {
        val item = Consumable(id = 1, name = "米", price = 30.0)
            .withPrice(today, 30.0)
            .withPrice(today.plusDays(10), 36.0)
            .withPrice(today.plusDays(20), 33.0)

        assertEquals(33.0, item.averagePrice, 0.001)
        assertEquals(-3.0, item.lastPriceDelta ?: 0.0, 0.001)
        assertEquals("3 次购买", item.priceRecordText)
    }

    @Test
    fun priceRecordTextIsEmptyHintWithoutHistory() {
        val item = Consumable(id = 1, name = "纸巾", price = 9.9)
        assertEquals("暂无记录", item.priceRecordText)
        assertNull(item.lastPriceDelta)
        assertEquals(9.9, item.averagePrice, 0.001)
    }

    @Test
    fun deferralHidesBaseDayAndShowsNextDay() {
        val task = Task(
            id = 7,
            title = "倒垃圾",
            startMinutes = 20 * 60,
            repeatEveryDays = 1,
            anchorEpochDay = today.toEpochDay(),
        )
        val deferred = task.copy(
            skipEpochDay = today.toEpochDay(),
            showEpochDay = today.plusDays(1).toEpochDay(),
        )

        assertFalse(deferred.isVisibleOn(today))
        assertTrue(deferred.isVisibleOn(today.plusDays(1)))
        assertTrue(deferred.isVisibleOn(today.plusDays(2)))
        assertEquals(today.toEpochDay(), deferred.anchorEpochDay)
        assertEquals(today.plusDays(1), deferred.nextVisibleDate(today))
    }

    @Test
    fun deferralOnFutureDateMovesOnlyThatOccurrence() {
        val base = today.plusDays(5)
        val task = Task(
            id = 8,
            title = "整理书架",
            startMinutes = 9 * 60,
            repeatEveryDays = 3,
            anchorEpochDay = base.toEpochDay(),
        )
        val deferred = task.copy(
            skipEpochDay = base.toEpochDay(),
            showEpochDay = base.plusDays(1).toEpochDay(),
        )

        assertFalse(deferred.isVisibleOn(base))
        assertTrue(deferred.isVisibleOn(base.plusDays(1)))
        // 再隔 3 天应该恢复原来的周期。
        assertTrue(deferred.isVisibleOn(base.plusDays(3)))
    }

    @Test
    fun viewModeConstantsAreDistinct() {
        assertEquals(0, ConsumableStore.VIEW_LIST)
        assertEquals(1, ConsumableStore.VIEW_GROUPS)
    }

    @Test
    fun countMetricGroupsByTypeLabel() {
        val items = listOf(
            Consumable(id = 1, name = "洗发水", type = "洗护", price = 59.0),
            Consumable(id = 2, name = "护发素", type = "洗护", price = 39.0),
            Consumable(id = 3, name = "大米", type = "食品", price = 50.0),
            Consumable(id = 4, name = "抹布", type = "", price = 5.0),
        )
        val slices = buildStatSlices(items, StatMetric.COUNT)

        assertEquals(3, slices.size)
        val washing = slices.first { it.label == "洗护" }
        assertEquals(2.0, washing.value, 0.001)
        assertEquals(2, washing.count)
        assertTrue(slices.first().label == "洗护")
    }

    @Test
    fun amountMetricSkipsTypesWithoutPrice() {
        val items = listOf(
            Consumable(id = 1, name = "牙膏", type = "洗护", price = 12.0),
            Consumable(id = 2, name = "扫帚", type = "清洁", price = 0.0),
        )
        val slices = buildStatSlices(items, StatMetric.AMOUNT)

        assertEquals(1, slices.size)
        assertEquals("洗护", slices.single().label)
    }

    @Test
    fun pricePointsCarryTheirOwnDate() {
        val point = PricePoint(today.toEpochDay(), 19.9)
        assertEquals(today, point.date)
        assertEquals("¥19.90", point.priceText)
    }
}
