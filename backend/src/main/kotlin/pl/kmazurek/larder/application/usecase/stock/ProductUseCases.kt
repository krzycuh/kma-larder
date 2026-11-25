package pl.kmazurek.larder.application.usecase.stock

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pl.kmazurek.larder.application.dto.PagedResponse
import pl.kmazurek.larder.application.dto.ProductDetailDto
import pl.kmazurek.larder.application.dto.ProductDto
import pl.kmazurek.larder.application.dto.ProductRequest
import pl.kmazurek.larder.application.dto.YearQuantityDto
import pl.kmazurek.larder.application.usecase.pantry.DuplicateProductException
import pl.kmazurek.larder.application.usecase.pantry.PantryAccessGuard
import pl.kmazurek.larder.domain.model.pantry.PantryId
import pl.kmazurek.larder.domain.model.pantry.ProductId
import pl.kmazurek.larder.domain.model.stock.Barcode
import pl.kmazurek.larder.domain.model.stock.MovementType
import pl.kmazurek.larder.domain.model.stock.Product
import pl.kmazurek.larder.domain.model.stock.ProductCategory
import pl.kmazurek.larder.domain.model.stock.ProductKind
import pl.kmazurek.larder.domain.model.stock.ProductName
import pl.kmazurek.larder.domain.model.user.UserId
import pl.kmazurek.larder.domain.repository.BatchRepository
import pl.kmazurek.larder.domain.repository.ProductFilter
import pl.kmazurek.larder.domain.repository.ProductRepository
import pl.kmazurek.larder.domain.repository.StockMovementRepository

enum class ProductSort {
    NAME,
    EXPIRY,
    QUANTITY,
}

data class ListProductsQuery(
    val userId: UserId,
    val pantryId: PantryId,
    val searchTerm: String? = null,
    val category: ProductCategory? = null,
    val kind: ProductKind? = null,
    val includeArchived: Boolean = false,
    val sort: ProductSort = ProductSort.NAME,
    val page: Int = 0,
    val pageSize: Int = 50,
)

/**
 * Use Case: the stock list – every product of the pantry with how many units are on the shelves.
 * Sorting and paging happen in memory: a household pantry has at most a few hundred products.
 */
@Service
class ListProducts(
    private val productRepository: ProductRepository,
    private val batchRepository: BatchRepository,
    private val pantryAccessGuard: PantryAccessGuard,
    private val access: StockAccess,
) {
    fun execute(query: ListProductsQuery): PagedResponse<ProductDto> {
        pantryAccessGuard.requireAccess(query.userId, query.pantryId)
        val filter =
            ProductFilter(
                searchTerm = query.searchTerm?.trim()?.takeIf { it.isNotEmpty() },
                category = query.category,
                kind = query.kind,
                includeArchived = query.includeArchived,
            )
        val products = productRepository.findByPantry(query.pantryId, filter)
        val dtos = access.productDtos(products, batchRepository.findInStockByPantry(query.pantryId))
        val sorted =
            when (query.sort) {
                ProductSort.NAME -> dtos.sortedBy { it.name.lowercase() }
                ProductSort.EXPIRY ->
                    dtos.sortedWith(compareBy(nullsLast()) { it.nextExpiresOn }).let { byDate ->
                        byDate.filter { it.quantity > 0 } + byDate.filter { it.quantity == 0 }
                    }
                ProductSort.QUANTITY -> dtos.sortedWith(compareByDescending<ProductDto> { it.quantity }.thenBy { it.name.lowercase() })
            }
        val pageSize = query.pageSize.coerceIn(1, 200)
        val page = query.page.coerceAtLeast(0)
        return PagedResponse.of(sorted.drop(page * pageSize).take(pageSize), page, pageSize, sorted.size.toLong())
    }
}

/**
 * Use Case: autocomplete while adding stock – active products whose name contains the phrase, best matches first.
 */
@Service
class SearchProducts(
    private val productRepository: ProductRepository,
    private val batchRepository: BatchRepository,
    private val pantryAccessGuard: PantryAccessGuard,
    private val access: StockAccess,
) {
    fun execute(
        userId: UserId,
        pantryId: PantryId,
        term: String,
        limit: Int = 10,
    ): List<ProductDto> {
        pantryAccessGuard.requireAccess(userId, pantryId)
        val normalized = ProductName.normalize(term)
        if (normalized.isEmpty()) return emptyList()
        val products = productRepository.findByPantry(pantryId, ProductFilter(searchTerm = term.trim()))
        val ranked =
            products.sortedWith(
                compareBy<Product>({ if (it.name.normalized.startsWith(normalized)) 0 else 1 }, { it.name.value.lowercase() }),
            )
        return access.productDtos(ranked.take(limit.coerceIn(1, 50)), batchRepository.findInStockByPantry(pantryId))
    }
}

/**
 * Use Case: a product with its batches and history
 */
@Service
class GetProduct(
    private val batchRepository: BatchRepository,
    private val movementRepository: StockMovementRepository,
    private val access: StockAccess,
) {
    fun execute(
        userId: UserId,
        productId: ProductId,
    ): ProductDetailDto {
        val product = access.loadProduct(userId, productId)
        val batches = batchRepository.findByProduct(productId)
        val (movements, _) = movementRepository.findByPantry(product.pantryId, productId, 0, RECENT_MOVEMENTS)
        val consumed = movementRepository.unitsByYear(productId, MovementType.CONSUMED)
        return ProductDetailDto(
            product = access.productDto(product, batches),
            batches = batches.map { access.batchDto(it) },
            recentMovements = access.movementDtos(movements),
            consumedByYear = consumed.entries.sortedByDescending { it.key }.map { YearQuantityDto(it.key, it.value) },
        )
    }

    companion object {
        const val RECENT_MOVEMENTS = 20
    }
}

/**
 * Use Case: define a new product in the pantry. The name must be unique within the pantry (ignoring case,
 * diacritics and punctuation), and so must the barcode.
 */
@Service
class CreateProduct(
    private val productRepository: ProductRepository,
    private val pantryAccessGuard: PantryAccessGuard,
    private val access: StockAccess,
) {
    @Transactional
    fun execute(
        userId: UserId,
        pantryId: PantryId,
        request: ProductRequest,
    ): ProductDto {
        pantryAccessGuard.requireAccess(userId, pantryId)
        val name = ProductName.of(request.name)
        val barcode = parseBarcode(request.barcode)
        productRepository.findByNormalizedName(pantryId, name.normalized)?.let {
            throw DuplicateProductException("Product '${it.name}' already exists in this pantry")
        }
        barcode?.let { code ->
            productRepository.findByBarcode(pantryId, code)?.let {
                throw DuplicateProductException("Product '${it.name}' already has barcode $code")
            }
        }
        val product =
            Product.create(
                pantryId = pantryId,
                name = name,
                kind = request.kind,
                category = request.category,
                barcode = barcode,
                packageSize = request.packageSize,
                defaultShelfLifeMonths = request.defaultShelfLifeMonths,
                minQuantity = request.minQuantity,
                notes = request.notes,
                createdByUserId = userId,
            )
        return access.productDto(productRepository.save(product), emptyList())
    }
}

/**
 * Use Case: edit a product. A changed shelf life moves the derived best-before dates of its batches.
 */
@Service
class UpdateProduct(
    private val productRepository: ProductRepository,
    private val batchRepository: BatchRepository,
    private val access: StockAccess,
) {
    @Transactional
    fun execute(
        userId: UserId,
        productId: ProductId,
        request: ProductRequest,
    ): ProductDto {
        val existing = access.loadProduct(userId, productId)
        val name = ProductName.of(request.name)
        val barcode = parseBarcode(request.barcode)
        productRepository.findByNormalizedName(existing.pantryId, name.normalized)?.takeIf { it.id != productId }?.let {
            throw DuplicateProductException("Product '${it.name}' already exists in this pantry")
        }
        barcode?.let { code ->
            productRepository.findByBarcode(existing.pantryId, code)?.takeIf { it.id != productId }?.let {
                throw DuplicateProductException("Product '${it.name}' already has barcode $code")
            }
        }
        val updated =
            existing.update(
                name = name,
                kind = request.kind,
                category = request.category,
                barcode = barcode,
                packageSize = request.packageSize,
                defaultShelfLifeMonths = request.defaultShelfLifeMonths,
                minQuantity = request.minQuantity,
                notes = request.notes,
                archived = request.archived,
            )
        val saved = productRepository.save(updated)
        var batches = batchRepository.findByProduct(productId)
        if (updated.defaultShelfLifeMonths != existing.defaultShelfLifeMonths) {
            val refreshed = batches.map { it.refreshExpiry(saved) }
            batches = batchRepository.saveAll(refreshed)
        }
        return access.productDto(saved, batches)
    }
}

/**
 * Use Case: delete a product with its batches and history. Archiving (UpdateProduct) is the gentler option.
 */
@Service
class DeleteProduct(
    private val productRepository: ProductRepository,
    private val access: StockAccess,
) {
    @Transactional
    fun execute(
        userId: UserId,
        productId: ProductId,
    ) {
        access.loadProduct(userId, productId)
        productRepository.deleteById(productId)
    }
}

internal fun parseBarcode(raw: String?): Barcode? {
    val trimmed = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    return Barcode.parse(trimmed) ?: throw IllegalArgumentException("Invalid barcode: $trimmed")
}
