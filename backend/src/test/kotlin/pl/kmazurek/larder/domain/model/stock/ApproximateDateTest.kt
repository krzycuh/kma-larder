package pl.kmazurek.larder.domain.model.stock

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class ApproximateDateTest {
    @Test
    fun `period ends on the last day of the year, month or the day itself`() {
        assertEquals(LocalDate.of(2027, 12, 31), ApproximateDate(2027).endDate)
        assertEquals(LocalDate.of(2027, 2, 28), ApproximateDate(2027, 2).endDate)
        assertEquals(LocalDate.of(2028, 2, 29), ApproximateDate(2028, 2).endDate)
        assertEquals(LocalDate.of(2027, 5, 12), ApproximateDate(2027, 5, 12).endDate)
        assertEquals(LocalDate.of(2027, 1, 1), ApproximateDate(2027).startDate)
    }

    @Test
    fun `rejects impossible dates`() {
        assertThrows<IllegalArgumentException> { ApproximateDate(1800) }
        assertThrows<IllegalArgumentException> { ApproximateDate(2027, 13) }
        assertThrows<IllegalArgumentException> { ApproximateDate(2027, 2, 30) }
        assertThrows<IllegalArgumentException> { ApproximateDate(2027, null, 5) }
    }

    @Test
    fun `shifting by months keeps the precision`() {
        assertEquals(ApproximateDate(2027), ApproximateDate(2026).plusMonths(12))
        assertEquals(ApproximateDate(2028), ApproximateDate(2026).plusMonths(24))
        assertEquals(ApproximateDate(2027), ApproximateDate(2026).plusMonths(6))
        assertEquals(ApproximateDate(2027, 6), ApproximateDate(2026, 6).plusMonths(12))
        assertEquals(ApproximateDate(2027, 2), ApproximateDate(2026, 11).plusMonths(3))
        assertEquals(ApproximateDate(2027, 6, 15), ApproximateDate(2026, 6, 15).plusMonths(12))
        assertEquals(ApproximateDate(2027, 2, 28), ApproximateDate(2026, 8, 31).plusMonths(6))
    }

    @Test
    fun `compares by end date and prints like a label`() {
        assertTrue(ApproximateDate(2027, 5) < ApproximateDate(2027))
        assertTrue(ApproximateDate(2027, 5, 12) < ApproximateDate(2027, 5))
        assertEquals("2027", ApproximateDate(2027).toString())
        assertEquals("05.2027", ApproximateDate(2027, 5).toString())
        assertEquals("12.05.2027", ApproximateDate(2027, 5, 12).toString())
        assertTrue(ApproximateDate(2026, 1).isBefore(LocalDate.of(2026, 2, 1)))
    }
}
