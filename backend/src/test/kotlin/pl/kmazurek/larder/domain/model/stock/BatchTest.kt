package pl.kmazurek.larder.domain.model.stock

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import pl.kmazurek.larder.domain.model.pantry.PantryId
import pl.kmazurek.larder.domain.model.user.UserId
import java.time.LocalDate

class BatchTest {
    private val pantryId = PantryId.generate()
    private val cucumbers =
        Product.create(
            pantryId,
            ProductName.of("Ogórki kiszone"),
            ProductKind.HOMEMADE,
            ProductCategory.PRESERVES,
            defaultShelfLifeMonths = 12,
        )
    private val flour = Product.create(pantryId, ProductName.of("Mąka"), ProductKind.STORE_BOUGHT, ProductCategory.FLOUR_GRAINS)

    @Test
    fun `a home-made batch derives its expiry from the production year and shelf life`() {
        val batch = Batch.create(cucumbers, quantity = 6, producedOn = ApproximateDate(2026))
        assertEquals(LocalDate.of(2027, 12, 31), batch.expiresOn)
        assertEquals(pantryId, batch.pantryId)
        assertEquals(FreshnessStatus.OK, batch.freshness(LocalDate.of(2027, 1, 1)))
        assertEquals(FreshnessStatus.EXPIRING_SOON, batch.freshness(LocalDate.of(2027, 12, 15)))
        assertEquals(FreshnessStatus.EXPIRED, batch.freshness(LocalDate.of(2028, 1, 1)))
    }

    @Test
    fun `a bought batch uses the date on the packaging and is undated without one`() {
        assertEquals(LocalDate.of(2028, 5, 31), Batch.create(flour, 2, bestBefore = ApproximateDate(2028, 5)).expiresOn)
        val undated = Batch.create(flour, 2)
        assertNull(undated.expiresOn)
        assertEquals(FreshnessStatus.UNKNOWN, undated.freshness(LocalDate.now()))
    }

    @Test
    fun `taking units never goes below zero`() {
        val batch = Batch.create(flour, 2)
        val one = batch.take(1)
        assertEquals(1, one.quantity)
        assertEquals(0, one.take(1).quantity)
        assertEquals(true, one.take(1).isEmpty)
        assertThrows<IllegalStateException> { one.take(2) }
        assertThrows<IllegalArgumentException> { one.take(0) }
        assertEquals(5, one.add(4).quantity)
        assertEquals(9, one.adjustTo(9).quantity)
        assertThrows<IllegalArgumentException> { Batch.create(flour, 0) }
    }

    @Test
    fun `changing the shelf life moves the derived date but not the printed one`() {
        val derived = Batch.create(cucumbers, 6, producedOn = ApproximateDate(2026, 8))
        val printed = Batch.create(cucumbers, 6, bestBefore = ApproximateDate(2027, 3), producedOn = ApproximateDate(2026, 8))
        val longer = cucumbers.copy(defaultShelfLifeMonths = 24)

        assertEquals(LocalDate.of(2028, 8, 31), derived.refreshExpiry(longer).expiresOn)
        assertEquals(LocalDate.of(2027, 3, 31), printed.refreshExpiry(longer).expiresOn)
        assertThrows<IllegalArgumentException> { derived.refreshExpiry(flour) }
    }

    @Test
    fun `update replaces the dates and trims texts`() {
        val batch = Batch.create(flour, 1, location = " piwnica ")
        assertEquals("piwnica", batch.location)
        val updated = batch.update(flour, ApproximateDate(2029), null, "", "  uwaga ")
        assertEquals(LocalDate.of(2029, 12, 31), updated.expiresOn)
        assertNull(updated.location)
        assertEquals("uwaga", updated.notes)
    }

    @Test
    fun `movements carry the signed change and refuse the wrong sign`() {
        val batch = Batch.create(flour, 3)
        val user = UserId.generate()
        assertEquals(3, StockMovement.added(batch, 3, user).quantity)
        assertEquals(-1, StockMovement.consumed(batch, 1, user).quantity)
        assertEquals(-2, StockMovement.discarded(batch, 2, user).quantity)
        assertEquals(-1, StockMovement.adjusted(batch, -1, user).quantity)
        assertThrows<IllegalArgumentException> { StockMovement.adjusted(batch, 0, user) }
        assertThrows<IllegalArgumentException> { StockMovement.consumed(batch, -1, user) }
    }
}
