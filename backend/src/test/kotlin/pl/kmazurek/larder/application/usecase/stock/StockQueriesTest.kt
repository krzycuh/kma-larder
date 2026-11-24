package pl.kmazurek.larder.application.usecase.stock

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import pl.kmazurek.larder.application.dto.ApproximateDateDto
import pl.kmazurek.larder.application.dto.BatchRequest
import pl.kmazurek.larder.application.dto.ProductRequest
import pl.kmazurek.larder.application.dto.QuantityChangeRequest
import pl.kmazurek.larder.application.usecase.pantry.PantryAccessDeniedException
import pl.kmazurek.larder.domain.model.pantry.BatchId
import pl.kmazurek.larder.domain.model.pantry.ProductId
import pl.kmazurek.larder.domain.model.stock.FreshnessStatus
import pl.kmazurek.larder.domain.model.stock.ProductCategory
import pl.kmazurek.larder.domain.model.stock.ProductKind

class StockQueriesTest {
    private val s = StockTestSupport()
    private val create = CreateProduct(s.productRepository, s.guard, s.access)
    private val addBatch = AddBatch(s.batchRepository, s.movementRepository, s.access)
    private val expiring = ListExpiring(s.productRepository, s.batchRepository, s.guard, s.access)
    private val replenishment = ListReplenishment(s.productRepository, s.batchRepository, s.guard, s.access)
    private val dashboard =
        GetDashboard(s.productRepository, s.batchRepository, s.movementRepository, s.guard, expiring, replenishment, s.access)

    private fun product(
        name: String,
        minQuantity: Int = 0,
        kind: ProductKind = ProductKind.STORE_BOUGHT,
        shelfLife: Int? = null,
    ) = ProductId.fromString(
        create.execute(
            s.owner.id,
            s.pantry.id,
            ProductRequest(name, kind, ProductCategory.OTHER, minQuantity = minQuantity, defaultShelfLifeMonths = shelfLife),
        ).id,
    )

    private fun seed() {
        val cans = product("Fasola w puszce", minQuantity = 4)
        val water = product("Woda", minQuantity = 6)
        val honey = product("Miód", kind = ProductKind.HOMEMADE, shelfLife = 24)
        product("Cukier", minQuantity = 1)
        addBatch.execute(s.owner.id, cans, BatchRequest(2, bestBefore = ApproximateDateDto(2026, 10))) // expired
        addBatch.execute(s.owner.id, cans, BatchRequest(1, bestBefore = ApproximateDateDto(2026, 12, 10))) // 16 days
        addBatch.execute(s.owner.id, water, BatchRequest(6)) // undated, enough
        addBatch.execute(s.owner.id, honey, BatchRequest(3, producedOn = ApproximateDateDto(2025))) // -> 2027, fine
        addBatch.execute(s.owner.id, honey, BatchRequest(1, producedOn = ApproximateDateDto(2024, 11))) // -> 11.2026, 6 days left
    }

    @Test
    fun `expiring lists dated batches soonest first with filters`() {
        seed()

        val all = expiring.execute(ExpiringQuery(s.owner.id, s.pantry.id))
        assertEquals(listOf("Fasola w puszce", "Miód", "Fasola w puszce", "Miód"), all.map { it.product.name })
        assertEquals(
            listOf(
                FreshnessStatus.EXPIRED,
                FreshnessStatus.EXPIRING_SOON,
                FreshnessStatus.EXPIRING_SOON,
                FreshnessStatus.OK,
            ),
            all.map {
                it.freshness
            },
        )

        assertEquals(1, expiring.execute(ExpiringQuery(s.owner.id, s.pantry.id, expiredOnly = true)).size)
        assertEquals(3, expiring.execute(ExpiringQuery(s.owner.id, s.pantry.id, withinDays = 40)).size)
        assertEquals(listOf("Woda"), expiring.execute(ExpiringQuery(s.owner.id, s.pantry.id, undatedOnly = true)).map { it.product.name })
        assertEquals(
            FreshnessStatus.EXPIRING_SOON,
            expiring.execute(ExpiringQuery(s.owner.id, s.pantry.id, soonDays = 410)).last().freshness,
        )
        assertThrows<PantryAccessDeniedException> { expiring.execute(ExpiringQuery(s.outsider.id, s.pantry.id)) }
    }

    @Test
    fun `expiring skips archived products`() {
        seed()
        val honey = s.products.values.first { it.name.value == "Miód" }
        s.products[honey.id.toString()] = honey.archive()

        assertEquals(2, expiring.execute(ExpiringQuery(s.owner.id, s.pantry.id)).size)
    }

    @Test
    fun `replenishment lists products below their minimum, biggest gap first`() {
        seed()

        val items = replenishment.execute(s.owner.id, s.pantry.id)

        assertEquals(listOf("Cukier" to 1, "Fasola w puszce" to 1), items.map { it.product.name to it.missing })
        assertEquals(3, items.last().quantity)
        assertEquals(4, items.last().minQuantity)
    }

    @Test
    fun `dashboard counts units, statuses and lists the top entries`() {
        seed()
        ConsumeBatch(s.batchRepository, s.movementRepository, s.access)
            .execute(s.owner.id, BatchId.fromString(s.batches.values.first { it.quantity == 6 }.id.toString()), QuantityChangeRequest(1))

        val dto = dashboard.execute(s.owner.id, s.pantry.id)

        assertEquals(4, dto.productCount)
        assertEquals(12, dto.unitCount)
        assertEquals(1, dto.expiredCount)
        assertEquals(2, dto.expiringSoonCount)
        assertEquals(3, dto.belowMinimumCount)
        assertEquals(3, dto.expiring.size)
        assertEquals(listOf("Cukier", "Fasola w puszce", "Woda"), dto.belowMinimum.map { it.product.name })
        assertEquals(5, dto.recentMovements.size)
        assertEquals("Woda", dto.recentMovements.first().productName)
        assertEquals(30, dto.soonDays)
    }
}
