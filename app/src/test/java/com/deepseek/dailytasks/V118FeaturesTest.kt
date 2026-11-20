package com.deepseek.dailytasks

import com.deepseek.dailytasks.data.Consumable
import com.deepseek.dailytasks.data.PricePoint
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** v1.18：加号记账 —— 同名自动归档，价格/次数/累计花费一起累加。 */
class V118FeaturesTest {

    private val day = LocalDate.of(2026, 11, 1)

    private fun item(
        price: Double = 20.0,
        quantity: Double = 100.0,
        remaining: Double? = 40.0,
        endDate: LocalDate? = null,
        history: List<PricePoint> = emptyList(),
    ) = Consumable(
        id = 1L,
        name = "洗面奶",
        type = "洗护",
        startDate = day,
        endDate = endDate,
        quantity = quantity,
        unit = "毫升",
        price = price,
        totalAmount = quantity,
        remainingAmount = remaining,
        priceHistory = history,
    )

    @Test
    fun purchaseAppendsPriceRecord() {
        val next = item(history = listOf(PricePoint(day.toEpochDay(), 20.0)))
            .withPurchase(day.plusDays(10), newPrice = 25.0, amount = 120.0)
        assertEquals(2, next.purchaseCount)
        assertEquals(2, next.priceHistory.size)
        assertEquals(25.0, next.priceHistory.last().price, 0.001)
        assertEquals(45.0, next.totalSpent, 0.001)
    }

    @Test
    fun purchaseAccumulatesAmounts() {
        val next = item(quantity = 100.0, remaining = 40.0).withPurchase(day, 20.0, 120.0)
        assertEquals(220.0, next.quantity, 0.001)
        assertEquals(220.0, next.totalAmount!!, 0.001)
        assertEquals(160.0, next.remainingAmount!!, 0.001)
    }

    @Test
    fun purchaseRestartsFinishedItem() {
        val finished = item(endDate = day.plusDays(30), remaining = 0.0)
        val next = finished.withPurchase(day.plusDays(31), 30.0, 500.0)
        assertNull(next.endDate)
        assertTrue(next.isUsing)
        assertEquals(day.plusDays(31), next.startDate)
        assertEquals(500.0, next.remainingAmount!!, 0.001)
    }

    @Test
    fun zeroPriceKeepsPreviousPriceAndNoRecord() {
        val base = item(price = 12.0, history = listOf(PricePoint(day.toEpochDay(), 12.0)))
        val next = base.withPurchase(day.plusDays(3), 0.0, 10.0)
        assertEquals(12.0, next.price, 0.001)
        assertEquals(1, next.priceHistory.size)
        assertEquals(12.0, next.totalSpent, 0.001)
    }

    @Test
    fun fallbackPurchaseCountUsesCurrentPrice() {
        val noHistory = item(price = 18.0, history = emptyList())
        assertEquals(1, noHistory.purchaseCount)
        assertEquals(18.0, noHistory.totalSpent, 0.001)
        val zeroPrice = noHistory.copy(price = 0.0)
        assertEquals(0, zeroPrice.purchaseCount)
    }
}
