package pl.kmazurek.larder.application.usecase.stock

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import pl.kmazurek.larder.application.dto.ProductRequest
import pl.kmazurek.larder.application.service.ProductMetadata
import pl.kmazurek.larder.application.service.ProductMetadataProvider
import pl.kmazurek.larder.application.service.ProductMetadataUnavailableException
import pl.kmazurek.larder.application.usecase.pantry.PantryAccessDeniedException
import pl.kmazurek.larder.application.usecase.pantry.ProductNotFoundException
import pl.kmazurek.larder.domain.model.stock.Barcode
import pl.kmazurek.larder.domain.model.stock.ProductCategory
import pl.kmazurek.larder.domain.model.stock.ProductKind

class LookupBarcodeTest {
    private val s = StockTestSupport()
    private val known = mutableMapOf<String, ProductMetadata>()
    private var down = false
    private val provider =
        object : ProductMetadataProvider {
            override fun lookupByBarcode(barcode: Barcode): ProductMetadata? {
                if (down) throw ProductMetadataUnavailableException("down")
                return known[barcode.value]
            }
        }
    private val lookup = LookupBarcode(s.productRepository, provider, s.guard, s.access)

    @Test
    fun `answers with the pantry's product and the external suggestion`() {
        CreateProduct(s.productRepository, s.guard, s.access).execute(
            s.owner.id,
            s.pantry.id,
            ProductRequest("Fasola", ProductKind.STORE_BOUGHT, ProductCategory.CANNED, barcode = "5901234123457"),
        )
        known["5901234123457"] = ProductMetadata("5901234123457", "Fasola czerwona", "Marka", "400 g", null, "OPEN_FOOD_FACTS")
        known["96385074"] = ProductMetadata("96385074", "Sok", null, "1 l", null, "OPEN_FOOD_FACTS")

        val both = lookup.execute(s.owner.id, s.pantry.id, "590 1234 12345 7")
        assertEquals("Fasola", both.product?.name)
        assertEquals("Fasola czerwona", both.metadata?.name)

        val external = lookup.execute(s.owner.id, s.pantry.id, "96385074")
        assertNull(external.product)
        assertEquals("1 l", external.metadata?.packageSize)
    }

    @Test
    fun `rejects invalid codes, outsiders and unknown codes`() {
        assertThrows<IllegalArgumentException> { lookup.execute(s.owner.id, s.pantry.id, "123") }
        assertThrows<PantryAccessDeniedException> { lookup.execute(s.outsider.id, s.pantry.id, "96385074") }
        assertThrows<ProductNotFoundException> { lookup.execute(s.owner.id, s.pantry.id, "96385074") }
    }

    @Test
    fun `an unavailable database is a 503 only when the pantry does not know the code either`() {
        CreateProduct(s.productRepository, s.guard, s.access).execute(
            s.owner.id,
            s.pantry.id,
            ProductRequest("Sok", ProductKind.STORE_BOUGHT, ProductCategory.BEVERAGES, barcode = "96385074"),
        )
        down = true

        assertEquals("Sok", lookup.execute(s.owner.id, s.pantry.id, "96385074").product?.name)
        assertThrows<ProductMetadataUnavailableException> { lookup.execute(s.owner.id, s.pantry.id, "5901234123457") }
    }
}
