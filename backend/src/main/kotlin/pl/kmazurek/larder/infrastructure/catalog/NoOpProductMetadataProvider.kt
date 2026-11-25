package pl.kmazurek.larder.infrastructure.catalog

import pl.kmazurek.larder.application.service.ProductMetadata
import pl.kmazurek.larder.application.service.ProductMetadataProvider
import pl.kmazurek.larder.domain.model.stock.Barcode

/**
 * Offline adapter: knows nothing. Used when app.catalog.provider = none (and in tests).
 */
class NoOpProductMetadataProvider : ProductMetadataProvider {
    override fun lookupByBarcode(barcode: Barcode): ProductMetadata? = null
}
