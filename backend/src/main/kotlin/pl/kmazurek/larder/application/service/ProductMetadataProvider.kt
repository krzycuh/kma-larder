package pl.kmazurek.larder.application.service

import pl.kmazurek.larder.domain.model.stock.Barcode
import pl.kmazurek.larder.domain.model.stock.ProductCategory

/**
 * What an external product database knows about a barcode: enough to prefill a new product.
 */
data class ProductMetadata(
    val barcode: String,
    val name: String,
    val brand: String?,
    val packageSize: String?,
    val imageUrl: String?,
    /** The pantry category suggested by the database's own classification; null when nothing matched */
    val category: ProductCategory? = null,
    val source: String,
)

/**
 * Port: looks a barcode up in an external product database (Open Food Facts).
 * Returns null when the database does not know the code; throws ProductMetadataUnavailableException
 * when it cannot be asked (timeout, 5xx, network).
 */
interface ProductMetadataProvider {
    fun lookupByBarcode(barcode: Barcode): ProductMetadata?
}

class ProductMetadataUnavailableException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
