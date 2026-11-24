package pl.kmazurek.larder.domain.repository

import pl.kmazurek.larder.domain.model.pantry.PantryId
import pl.kmazurek.larder.domain.model.pantry.ProductId
import pl.kmazurek.larder.domain.model.stock.Barcode
import pl.kmazurek.larder.domain.model.stock.Product
import pl.kmazurek.larder.domain.model.stock.ProductCategory
import pl.kmazurek.larder.domain.model.stock.ProductKind

data class ProductFilter(
    val searchTerm: String? = null,
    val category: ProductCategory? = null,
    val kind: ProductKind? = null,
    val includeArchived: Boolean = false,
)

/**
 * Repository interface (port) for the Product aggregate.
 * A pantry holds at most a few hundred products, so listing returns them all and the use case sorts and pages.
 */
interface ProductRepository {
    fun findById(id: ProductId): Product?

    fun findByIds(ids: Collection<ProductId>): Map<ProductId, Product>

    fun findByPantry(
        pantryId: PantryId,
        filter: ProductFilter,
    ): List<Product>

    fun findByBarcode(
        pantryId: PantryId,
        barcode: Barcode,
    ): Product?

    fun findByNormalizedName(
        pantryId: PantryId,
        normalizedName: String,
    ): Product?

    fun save(product: Product): Product

    fun deleteById(id: ProductId)
}
