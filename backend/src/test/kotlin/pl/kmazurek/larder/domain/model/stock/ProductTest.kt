package pl.kmazurek.larder.domain.model.stock

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import pl.kmazurek.larder.domain.model.pantry.PantryId

class ProductTest {
    private val pantryId = PantryId.generate()

    @Test
    fun `product name is trimmed and normalized for duplicate detection`() {
        val name = ProductName.of("  Ogórki   Kiszone ")
        assertEquals("Ogórki Kiszone", name.value)
        assertEquals("ogorki kiszone", name.normalized)
        assertEquals("maka pszenna 1 kg", ProductName.normalize("Mąka pszenna (1 kg)!"))
        assertEquals("zoltko lsc", ProductName.normalize("Żółtko ŁŚĆ"))
        assertThrows<IllegalArgumentException> { ProductName.of("   ") }
    }

    @Test
    fun `barcode accepts EAN-13, EAN-8 and UPC-A with a valid check digit`() {
        assertEquals("5901234123457", Barcode.parse("590 1234 12345 7")?.value)
        assertEquals("96385074", Barcode.parse("96385074")?.value)
        assertEquals("0036000291452", Barcode.parse("036000291452")?.value)
        assertNull(Barcode.parse("5901234123456"))
        assertNull(Barcode.parse("12345"))
        assertNull(Barcode.parse(null))
        assertThrows<IllegalArgumentException> { Barcode("5901234123456") }
    }

    @Test
    fun `best-before date on packaging wins over the derived one`() {
        val jam =
            Product.create(
                pantryId,
                ProductName.of("Dżem"),
                ProductKind.HOMEMADE,
                ProductCategory.PRESERVES,
                defaultShelfLifeMonths = 12,
            )
        val onLabel = ApproximateDate(2028, 3)

        assertEquals(onLabel, jam.bestBeforeFor(onLabel, ApproximateDate(2026)))
        assertEquals(ApproximateDate(2027), jam.bestBeforeFor(null, ApproximateDate(2026)))
        assertEquals(ApproximateDate(2027, 8), jam.bestBeforeFor(null, ApproximateDate(2026, 8)))
        assertNull(jam.bestBeforeFor(null, null))
    }

    @Test
    fun `without a shelf life a home-made product has no derived date`() {
        val honey = Product.create(pantryId, ProductName.of("Miód"), ProductKind.HOMEMADE, ProductCategory.HONEY)
        assertNull(honey.bestBeforeFor(null, ApproximateDate(2026)))
    }

    @Test
    fun `validates minimum quantity and shelf life`() {
        assertThrows<IllegalArgumentException> {
            Product.create(pantryId, ProductName.of("X"), ProductKind.STORE_BOUGHT, ProductCategory.OTHER, minQuantity = -1)
        }
        assertThrows<IllegalArgumentException> {
            Product.create(pantryId, ProductName.of("X"), ProductKind.STORE_BOUGHT, ProductCategory.OTHER, defaultShelfLifeMonths = 0)
        }
    }

    @Test
    fun `update trims optional texts and can archive`() {
        val product = Product.create(pantryId, ProductName.of("Woda"), ProductKind.STORE_BOUGHT, ProductCategory.BEVERAGES)
        val updated =
            product.update(
                name = ProductName.of("Woda mineralna"),
                kind = ProductKind.STORE_BOUGHT,
                category = ProductCategory.BEVERAGES,
                barcode = null,
                packageSize = "  1,5 l ",
                defaultShelfLifeMonths = null,
                minQuantity = 6,
                notes = "  ",
                archived = true,
            )
        assertEquals("1,5 l", updated.packageSize)
        assertNull(updated.notes)
        assertEquals(6, updated.minQuantity)
        assertEquals(true, updated.archived)
    }
}
