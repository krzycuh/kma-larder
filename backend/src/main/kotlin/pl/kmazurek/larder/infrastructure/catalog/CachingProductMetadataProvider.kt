package pl.kmazurek.larder.infrastructure.catalog

import org.springframework.cache.CacheManager
import pl.kmazurek.larder.application.service.ProductMetadata
import pl.kmazurek.larder.application.service.ProductMetadataProvider
import pl.kmazurek.larder.domain.model.stock.Barcode

/**
 * Caches lookups by barcode, including "not found" answers. Failures (exceptions) are not cached.
 */
class CachingProductMetadataProvider(
    private val delegate: ProductMetadataProvider,
    cacheManager: CacheManager,
) : ProductMetadataProvider {
    private val cache = requireNotNull(cacheManager.getCache(CACHE_NAME)) { "Cache $CACHE_NAME is not available" }

    override fun lookupByBarcode(barcode: Barcode): ProductMetadata? {
        cache.get(barcode.value)?.let { return it.get() as ProductMetadata? }
        val metadata = delegate.lookupByBarcode(barcode)
        cache.put(barcode.value, metadata)
        return metadata
    }

    companion object {
        const val CACHE_NAME = "product-metadata"
    }
}
