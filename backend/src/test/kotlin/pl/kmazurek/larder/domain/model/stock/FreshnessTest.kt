package pl.kmazurek.larder.domain.model.stock

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class FreshnessTest {
    private val today = LocalDate.of(2026, 11, 24)

    @Test
    fun `status depends on how far the date is from today`() {
        assertEquals(FreshnessStatus.UNKNOWN, Freshness.status(null, today))
        assertEquals(FreshnessStatus.EXPIRED, Freshness.status(today.minusDays(1), today))
        assertEquals(FreshnessStatus.EXPIRING_SOON, Freshness.status(today, today))
        assertEquals(FreshnessStatus.EXPIRING_SOON, Freshness.status(today.plusDays(30), today))
        assertEquals(FreshnessStatus.OK, Freshness.status(today.plusDays(31), today))
        assertEquals(FreshnessStatus.EXPIRING_SOON, Freshness.status(today.plusDays(80), today, soonDays = 90))
    }

    @Test
    fun `days left is signed and null without a date`() {
        assertEquals(7L, Freshness.daysLeft(today.plusDays(7), today))
        assertEquals(-3L, Freshness.daysLeft(today.minusDays(3), today))
        assertEquals(null, Freshness.daysLeft(null, today))
    }

    @Test
    fun `worst status wins, unknown only when nothing is dated`() {
        assertEquals(FreshnessStatus.EXPIRED, Freshness.worst(listOf(FreshnessStatus.OK, FreshnessStatus.EXPIRED, FreshnessStatus.UNKNOWN)))
        assertEquals(FreshnessStatus.EXPIRING_SOON, Freshness.worst(listOf(FreshnessStatus.OK, FreshnessStatus.EXPIRING_SOON)))
        assertEquals(FreshnessStatus.OK, Freshness.worst(listOf(FreshnessStatus.OK, FreshnessStatus.UNKNOWN)))
        assertEquals(FreshnessStatus.UNKNOWN, Freshness.worst(listOf(FreshnessStatus.UNKNOWN)))
        assertEquals(FreshnessStatus.UNKNOWN, Freshness.worst(emptyList()))
    }
}
