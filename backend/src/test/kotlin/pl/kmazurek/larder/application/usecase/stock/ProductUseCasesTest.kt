package pl.kmazurek.larder.application.usecase.stock

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import pl.kmazurek.larder.application.dto.ApproximateDateDto
import pl.kmazurek.larder.application.dto.BatchRequest
import pl.kmazurek.larder.application.dto.ProductRequest
import pl.kmazurek.larder.application.usecase.pantry.DuplicateProductException
import pl.kmazurek.larder.application.usecase.pantry.PantryAccessDeniedException
import pl.kmazurek.larder.application.usecase.pantry.ProductNotFoundException
import pl.kmazurek.larder.domain.model.pantry.ProductId
import pl.kmazurek.larder.domain.model.stock.FreshnessStatus
import pl.kmazurek.larder.domain.model.stock.ProductCategory
import pl.kmazurek.larder.domain.model.stock.ProductKind
import java.time.LocalDate

class ProductUseCasesTest {
    private val s = StockTestSupport()
    private val create = CreateProduct(s.productRepository, s.guard, s.access)
    private val update = UpdateProduct(s.productRepository, s.batchRepository, s.access)
    private val list = ListProducts(s.productRepository, s.batchRepository, s.guard, s.access)
    private val search = SearchProducts(s.productRepository, s.batchRepository, s.guard, s.access)
    private val get = GetProduct(s.batchRepository, s.movementRepository, s.access)
    private val addBatch = AddBatch(s.batchRepository, s.movementRepository, s.access)

    private fun request(
        name: String,
        kind: ProductKind = ProductKind.STORE_BOUGHT,
        category: ProductCategory = ProductCategory.OTHER,
        barcode: String? = null,
        shelfLife: Int? = null,
        minQuantity: Int = 0,
    ) = ProductRequest(name, kind, category, barcode = barcode, defaultShelfLifeMonths = shelfLife, minQuantity = minQuantity)

    @Test
    fun `creates a product for a member, refuses outsiders`() {
        val dto = create.execute(s.owner.id, s.pantry.id, request("  Mąka pszenna ", barcode = "5901234123457", minQuantity = 2))

        assertEquals("Mąka pszenna", dto.name)
        assertEquals("5901234123457", dto.barcode)
        assertEquals(0, dto.quantity)
        assertEquals(true, dto.belowMinimum)
        assertEquals(FreshnessStatus.UNKNOWN, dto.freshness)
        assertThrows<PantryAccessDeniedException> { create.execute(s.outsider.id, s.pantry.id, request("Cukier")) }
    }

    @Test
    fun `rejects duplicate names, duplicate barcodes and invalid barcodes`() {
        create.execute(s.owner.id, s.pantry.id, request("Ogórki kiszone", barcode = "5901234123457"))

        assertThrows<DuplicateProductException> { create.execute(s.owner.id, s.pantry.id, request("ogorki   KISZONE")) }
        assertThrows<DuplicateProductException> { create.execute(s.owner.id, s.pantry.id, request("Inne", barcode = "5901234123457")) }
        assertThrows<IllegalArgumentException> { create.execute(s.owner.id, s.pantry.id, request("Inne", barcode = "123")) }
    }

    @Test
    fun `updating the shelf life moves derived dates of existing batches`() {
        val jam = create.execute(s.owner.id, s.pantry.id, request("Dżem", ProductKind.HOMEMADE, ProductCategory.PRESERVES, shelfLife = 12))
        val id = ProductId.fromString(jam.id)
        addBatch.execute(s.owner.id, id, BatchRequest(quantity = 4, producedOn = ApproximateDateDto(2026, 8)))

        val updated =
            update.execute(s.owner.id, id, request("Dżem truskawkowy", ProductKind.HOMEMADE, ProductCategory.PRESERVES, shelfLife = 24))

        assertEquals("Dżem truskawkowy", updated.name)
        assertEquals(LocalDate.of(2028, 8, 31), updated.nextExpiresOn)
        assertEquals(4, updated.quantity)
        assertEquals(LocalDate.of(2028, 8, 31), s.batches.values.single().expiresOn)
    }

    @Test
    fun `update keeps the own name and barcode but refuses another product's`() {
        val a = create.execute(s.owner.id, s.pantry.id, request("Woda", barcode = "5901234123457"))
        create.execute(s.owner.id, s.pantry.id, request("Sok", barcode = "96385074"))

        update.execute(s.owner.id, ProductId.fromString(a.id), request("woda", barcode = "5901234123457"))
        assertThrows<DuplicateProductException> { update.execute(s.owner.id, ProductId.fromString(a.id), request("Sok")) }
        assertThrows<DuplicateProductException> {
            update.execute(
                s.owner.id,
                ProductId.fromString(a.id),
                request("Woda", barcode = "96385074"),
            )
        }
    }

    @Test
    fun `list sums stock per product and sorts by name, expiry or quantity`() {
        val flour = create.execute(s.owner.id, s.pantry.id, request("Mąka", category = ProductCategory.FLOUR_GRAINS))
        val water = create.execute(s.owner.id, s.pantry.id, request("Woda", category = ProductCategory.BEVERAGES))
        create.execute(s.owner.id, s.pantry.id, request("Cukier"))
        addBatch.execute(s.owner.id, ProductId.fromString(flour.id), BatchRequest(2, bestBefore = ApproximateDateDto(2027, 3)))
        addBatch.execute(s.owner.id, ProductId.fromString(flour.id), BatchRequest(1, bestBefore = ApproximateDateDto(2026, 12, 1)))
        addBatch.execute(s.owner.id, ProductId.fromString(water.id), BatchRequest(12))

        val byName = list.execute(ListProductsQuery(s.owner.id, s.pantry.id))
        assertEquals(listOf("Cukier", "Mąka", "Woda"), byName.items.map { it.name })
        assertEquals(3, byName.items.first { it.name == "Mąka" }.quantity)
        assertEquals(2, byName.items.first { it.name == "Mąka" }.batchCount)
        assertEquals(LocalDate.of(2026, 12, 1), byName.items.first { it.name == "Mąka" }.nextExpiresOn)
        assertEquals(FreshnessStatus.EXPIRING_SOON, byName.items.first { it.name == "Mąka" }.freshness)
        assertEquals(FreshnessStatus.UNKNOWN, byName.items.first { it.name == "Woda" }.freshness)

        val byExpiry = list.execute(ListProductsQuery(s.owner.id, s.pantry.id, sort = ProductSort.EXPIRY))
        assertEquals(listOf("Mąka", "Woda", "Cukier"), byExpiry.items.map { it.name })

        val byQuantity = list.execute(ListProductsQuery(s.owner.id, s.pantry.id, sort = ProductSort.QUANTITY))
        assertEquals(listOf("Woda", "Mąka", "Cukier"), byQuantity.items.map { it.name })

        val paged = list.execute(ListProductsQuery(s.owner.id, s.pantry.id, page = 1, pageSize = 2))
        assertEquals(listOf("Woda"), paged.items.map { it.name })
        assertEquals(3, paged.totalItems)
        assertEquals(2, paged.totalPages)

        val filtered = list.execute(ListProductsQuery(s.owner.id, s.pantry.id, category = ProductCategory.BEVERAGES))
        assertEquals(listOf("Woda"), filtered.items.map { it.name })
    }

    @Test
    fun `search ranks products starting with the phrase first and ignores diacritics`() {
        create.execute(s.owner.id, s.pantry.id, request("Ogórki kiszone"))
        create.execute(s.owner.id, s.pantry.id, request("Sałatka z ogórków"))
        create.execute(s.owner.id, s.pantry.id, request("Miód"))

        assertEquals(listOf("Ogórki kiszone", "Sałatka z ogórków"), search.execute(s.owner.id, s.pantry.id, "ogor").map { it.name })
        assertTrue(search.execute(s.owner.id, s.pantry.id, "  ").isEmpty())
    }

    @Test
    fun `product detail lists batches, history and consumption per year`() {
        val jam = create.execute(s.owner.id, s.pantry.id, request("Dżem", ProductKind.HOMEMADE, ProductCategory.PRESERVES, shelfLife = 12))
        val id = ProductId.fromString(jam.id)
        val batch = addBatch.execute(s.owner.id, id, BatchRequest(5, producedOn = ApproximateDateDto(2026)))
        val consume = ConsumeBatch(s.batchRepository, s.movementRepository, s.access)
        consume.execute(
            s.owner.id,
            pl.kmazurek.larder.domain.model.pantry.BatchId.fromString(batch.id),
            pl.kmazurek.larder.application.dto.QuantityChangeRequest(2),
        )
        consume.execute(
            s.owner.id,
            pl.kmazurek.larder.domain.model.pantry.BatchId.fromString(batch.id),
            pl.kmazurek.larder.application.dto.QuantityChangeRequest(1, occurredOn = LocalDate.of(2025, 12, 30)),
        )

        val detail = get.execute(s.owner.id, id)

        assertEquals(2, detail.product.quantity)
        assertEquals(1, detail.batches.size)
        assertEquals(3, detail.recentMovements.size)
        assertEquals("Anna", detail.recentMovements.first().userName)
        assertEquals(listOf(2026 to 2, 2025 to 1), detail.consumedByYear.map { it.year to it.quantity })
        assertThrows<ProductNotFoundException> { get.execute(s.owner.id, ProductId.generate()) }
        assertThrows<PantryAccessDeniedException> { get.execute(s.outsider.id, id) }
    }

    @Test
    fun `delete removes the product`() {
        val dto = create.execute(s.owner.id, s.pantry.id, request("Woda"))
        DeleteProduct(s.productRepository, s.access).execute(s.owner.id, ProductId.fromString(dto.id))
        assertNull(s.products[dto.id])
    }
}
