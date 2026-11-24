package pl.kmazurek.larder.application.usecase.stock

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import pl.kmazurek.larder.application.dto.ApproximateDateDto
import pl.kmazurek.larder.application.dto.BatchRequest
import pl.kmazurek.larder.application.dto.ProductRequest
import pl.kmazurek.larder.application.dto.QuantityChangeRequest
import pl.kmazurek.larder.application.dto.UpdateBatchRequest
import pl.kmazurek.larder.application.usecase.pantry.BatchNotFoundException
import pl.kmazurek.larder.application.usecase.pantry.InsufficientStockException
import pl.kmazurek.larder.application.usecase.pantry.PantryAccessDeniedException
import pl.kmazurek.larder.domain.model.pantry.BatchId
import pl.kmazurek.larder.domain.model.pantry.ProductId
import pl.kmazurek.larder.domain.model.stock.FreshnessStatus
import pl.kmazurek.larder.domain.model.stock.MovementType
import pl.kmazurek.larder.domain.model.stock.ProductCategory
import pl.kmazurek.larder.domain.model.stock.ProductKind
import java.time.LocalDate

class BatchUseCasesTest {
    private val s = StockTestSupport()
    private val addBatch = AddBatch(s.batchRepository, s.movementRepository, s.access)
    private val consume = ConsumeBatch(s.batchRepository, s.movementRepository, s.access)
    private val discard = DiscardBatch(s.batchRepository, s.movementRepository, s.access)
    private val adjust = AdjustBatch(s.batchRepository, s.movementRepository, s.access)
    private val updateBatch = UpdateBatch(s.batchRepository, s.access)
    private val remove = RemoveBatch(s.batchRepository, s.access)

    private val cucumbers =
        CreateProduct(s.productRepository, s.guard, s.access).execute(
            s.owner.id,
            s.pantry.id,
            ProductRequest("Ogórki kiszone", ProductKind.HOMEMADE, ProductCategory.PRESERVES, defaultShelfLifeMonths = 12),
        ).let { ProductId.fromString(it.id) }

    @Test
    fun `adding a batch records an ADDED movement dated like the batch`() {
        val dto =
            addBatch.execute(
                s.owner.id,
                cucumbers,
                BatchRequest(6, producedOn = ApproximateDateDto(2026), addedOn = LocalDate.of(2026, 9, 1)),
            )

        assertEquals(6, dto.quantity)
        assertEquals(LocalDate.of(2027, 12, 31), dto.expiresOn)
        assertEquals(FreshnessStatus.OK, dto.freshness)
        assertEquals(402L, dto.daysLeft)
        assertEquals(LocalDate.of(2026, 9, 1), dto.addedOn)
        val movement = s.movements.single()
        assertEquals(MovementType.ADDED, movement.type)
        assertEquals(6, movement.quantity)
        assertEquals(LocalDate.of(2026, 9, 1), movement.occurredOn)
        assertEquals(s.owner.id, movement.userId)
        assertThrows<PantryAccessDeniedException> { addBatch.execute(s.outsider.id, cucumbers, BatchRequest(1)) }
    }

    @Test
    fun `consuming and discarding take units and refuse more than there is`() {
        val batch = BatchId.fromString(addBatch.execute(s.owner.id, cucumbers, BatchRequest(3)).id)

        assertEquals(2, consume.execute(s.owner.id, batch, QuantityChangeRequest(1)).quantity)
        assertEquals(0, discard.execute(s.owner.id, batch, QuantityChangeRequest(2)).quantity)
        assertThrows<InsufficientStockException> { consume.execute(s.owner.id, batch, QuantityChangeRequest(1)) }
        assertThrows<IllegalArgumentException> { consume.execute(s.owner.id, batch, QuantityChangeRequest(0)) }
        assertEquals(
            listOf(MovementType.ADDED to 3, MovementType.CONSUMED to -1, MovementType.DISCARDED to -2),
            s.movements.map {
                it.type to it.quantity
            },
        )
        assertEquals(s.today, s.movements.last().occurredOn)
    }

    @Test
    fun `adjusting records only the difference and nothing when equal`() {
        val batch = BatchId.fromString(addBatch.execute(s.owner.id, cucumbers, BatchRequest(5)).id)

        assertEquals(7, adjust.execute(s.owner.id, batch, QuantityChangeRequest(7)).quantity)
        assertEquals(7, adjust.execute(s.owner.id, batch, QuantityChangeRequest(7)).quantity)
        assertEquals(0, adjust.execute(s.owner.id, batch, QuantityChangeRequest(0)).quantity)
        assertEquals(listOf(5, 2, -7), s.movements.map { it.quantity })
        assertEquals(MovementType.ADJUSTED, s.movements.last().type)
    }

    @Test
    fun `updating a batch changes its dates and removing deletes it`() {
        val batch = BatchId.fromString(addBatch.execute(s.owner.id, cucumbers, BatchRequest(2, producedOn = ApproximateDateDto(2026))).id)

        val updated =
            updateBatch.execute(
                s.owner.id,
                batch,
                UpdateBatchRequest(bestBefore = ApproximateDateDto(2027, 6), location = "piwnica"),
            )
        assertEquals(LocalDate.of(2027, 6, 30), updated.expiresOn)
        assertEquals("piwnica", updated.location)
        assertNull(updated.producedOn)

        remove.execute(s.owner.id, batch)
        assertThrows<BatchNotFoundException> { consume.execute(s.owner.id, batch, QuantityChangeRequest(1)) }
    }
}
