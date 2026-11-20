package com.deepseek.dailytasks

import com.deepseek.dailytasks.data.BodyLogic
import com.deepseek.dailytasks.data.BodyMetric
import com.deepseek.dailytasks.data.BodyPoint
import com.deepseek.dailytasks.data.BodyRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class BodyMetricsTest {

    private fun record(
        date: LocalDate,
        weight: Double? = null,
        arm: Double? = null,
        waist: Double? = null,
        chest: Double? = null,
    ) = BodyRecord(date, weight, arm, waist, chest)

    @Test
    fun weekStartIsMonday() {
        val thursday = LocalDate.of(2026, 11, 12)
        assertEquals(LocalDate.of(2026, 11, 9), BodyRecord.weekStart(thursday))
        assertEquals(LocalDate.of(2026, 11, 9), BodyRecord.weekStart(LocalDate.of(2026, 11, 9)))
        assertEquals(LocalDate.of(2026, 11, 2), BodyRecord.weekStart(LocalDate.of(2026, 11, 8)))
    }

    @Test
    fun weeklyAverageGroupsRecordsOfSameWeek() {
        val records = listOf(
            record(LocalDate.of(2026, 11, 9), weight = 70.0),
            record(LocalDate.of(2026, 11, 11), weight = 71.0),
            record(LocalDate.of(2026, 11, 15), weight = 72.0),
        )
        val weeks = BodyLogic.weeklyAverages(records)
        assertEquals(1, weeks.size)
        assertEquals(71.0, weeks[0].averageWeight, 0.001)
        assertEquals(3, weeks[0].days)
        assertEquals("11/9 起", weeks[0].label)
    }

    @Test
    fun weeklyAverageSkipsRecordsWithoutWeight() {
        val records = listOf(
            record(LocalDate.of(2026, 11, 9), weight = 70.0),
            record(LocalDate.of(2026, 11, 10), arm = 35.0),
            record(LocalDate.of(2026, 11, 11), weight = 72.0),
        )
        val weeks = BodyLogic.weeklyAverages(records)
        assertEquals(1, weeks.size)
        assertEquals(71.0, weeks[0].averageWeight, 0.001)
        assertEquals(2, weeks[0].days)
    }

    @Test
    fun weeklyAveragesAreSortedByWeek() {
        val records = listOf(
            record(LocalDate.of(2026, 11, 10), weight = 70.0),
            record(LocalDate.of(2026, 11, 3), weight = 72.0),
            record(LocalDate.of(2026, 11, 17), weight = 69.0),
        )
        val weeks = BodyLogic.weeklyAverages(records)
        assertEquals(listOf(72.0, 70.0, 69.0), weeks.map { it.averageWeight })
    }

    @Test
    fun upsertReplacesSameDayRecord() {
        val day = LocalDate.of(2026, 11, 12)
        val records = listOf(record(day, weight = 70.0), record(day.minusDays(1), weight = 71.0))
        val updated = BodyLogic.upsert(records, record(day, weight = 69.5, waist = 80.0))
        assertEquals(2, updated.size)
        assertEquals(69.5, updated.last().weightKg!!, 0.001)
        assertEquals(80.0, updated.last().waistCm!!, 0.001)
        assertEquals(day.minusDays(1), updated.first().date)
    }

    @Test
    fun pointsOnlyIncludeRecordsWithThatMetric() {
        val records = listOf(
            record(LocalDate.of(2026, 11, 9), weight = 70.0, arm = 36.0),
            record(LocalDate.of(2026, 11, 10), weight = 70.5),
            record(LocalDate.of(2026, 11, 11), arm = 35.5),
        )
        val arms = BodyLogic.points(records, BodyMetric.ARM)
        assertEquals(listOf(BodyPoint(LocalDate.of(2026, 11, 9), 36.0), BodyPoint(LocalDate.of(2026, 11, 11), 35.5)), arms)
        assertEquals(2, BodyLogic.points(records, BodyMetric.WEIGHT).size)
        assertEquals(0, BodyLogic.points(records, BodyMetric.CHEST).size)
    }

    @Test
    fun weightChangeComparesLastTwoWeights() {
        val records = listOf(
            record(LocalDate.of(2026, 11, 9), weight = 72.0),
            record(LocalDate.of(2026, 11, 10), arm = 36.0),
            record(LocalDate.of(2026, 11, 12), weight = 71.0),
        )
        assertEquals(-1.0, BodyLogic.weightChange(records)!!, 0.001)
        assertNull(BodyLogic.weightChange(listOf(record(LocalDate.of(2026, 11, 12), weight = 71.0))))
    }

    @Test
    fun recentWeekAverageUsesLastSevenDays() {
        val today = LocalDate.of(2026, 11, 12)
        val records = listOf(
            record(today.minusDays(6), weight = 70.0),
            record(today, weight = 72.0),
            record(today.minusDays(7), weight = 90.0),
        )
        assertEquals(71.0, BodyLogic.recentWeekAverage(records, today)!!, 0.001)
        assertNull(BodyLogic.recentWeekAverage(emptyList(), today))
    }

    @Test
    fun changeTextFormatsSignAndFlat() {
        assertEquals("+1.0kg", BodyLogic.changeText(1.0))
        assertEquals("-0.5kg", BodyLogic.changeText(-0.5))
        assertEquals("持平", BodyLogic.changeText(0.02))
        assertEquals("—", BodyLogic.changeText(null))
    }
}
