package pl.kmazurek.larder.infrastructure.catalog

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import pl.kmazurek.larder.application.service.ProductMetadata
import pl.kmazurek.larder.application.service.ProductMetadataProvider
import pl.kmazurek.larder.application.service.ProductMetadataUnavailableException
import pl.kmazurek.larder.domain.model.stock.Barcode

class CachingProductMetadataProviderTest {
    private var calls = 0
    private var answer: (() -> ProductMetadata?) = { null }
    private val delegate =
        object : ProductMetadataProvider {
            override fun lookupByBarcode(barcode: Barcode): ProductMetadata? {
                calls++
                return answer()
            }
        }
    private val provider = CachingProductMetadataProvider(delegate, ConcurrentMapCacheManager(CachingProductMetadataProvider.CACHE_NAME))
    private val code = Barcode("5900020022745")

    @Test
    fun `answers and misses are cached, failures are not`() {
        answer = { ProductMetadata(code.value, "Pomidory", null, null, null, "OPEN_FOOD_FACTS") }
        assertEquals("Pomidory", provider.lookupByBarcode(code)?.name)
        assertEquals("Pomidory", provider.lookupByBarcode(code)?.name)
        assertEquals(1, calls)

        val other = Barcode("5901234123457")
        answer = { null }
        assertNull(provider.lookupByBarcode(other))
        assertNull(provider.lookupByBarcode(other))
        assertEquals(2, calls)

        val third = Barcode("96385074")
        answer = { throw ProductMetadataUnavailableException("down") }
        assertThrows<ProductMetadataUnavailableException> { provider.lookupByBarcode(third) }
        assertThrows<ProductMetadataUnavailableException> { provider.lookupByBarcode(third) }
        assertEquals(4, calls)
    }
}
