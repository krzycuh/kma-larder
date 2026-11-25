package pl.kmazurek.larder.application.usecase.stock

import org.springframework.stereotype.Service
import pl.kmazurek.larder.application.dto.BatchDto
import pl.kmazurek.larder.application.dto.ProductDto
import pl.kmazurek.larder.application.dto.StockMovementDto
import pl.kmazurek.larder.application.dto.StockSummary
import pl.kmazurek.larder.application.usecase.pantry.BatchNotFoundException
import pl.kmazurek.larder.application.usecase.pantry.PantryAccessGuard
import pl.kmazurek.larder.application.usecase.pantry.ProductNotFoundException
import pl.kmazurek.larder.domain.model.pantry.BatchId
import pl.kmazurek.larder.domain.model.pantry.ProductId
import pl.kmazurek.larder.domain.model.stock.Batch
import pl.kmazurek.larder.domain.model.stock.Freshness
import pl.kmazurek.larder.domain.model.stock.Product
import pl.kmazurek.larder.domain.model.stock.StockMovement
import pl.kmazurek.larder.domain.model.user.UserId
import pl.kmazurek.larder.domain.repository.BatchRepository
import pl.kmazurek.larder.domain.repository.ProductRepository
import pl.kmazurek.larder.domain.repository.UserRepository
import java.time.Clock
import java.time.LocalDate

/**
 * Shared helper for stock use cases: loads products and batches the user may access and maps them to DTOs
 * (stock summaries and freshness are computed against the injected clock).
 */
@Service
class StockAccess(
    private val productRepository: ProductRepository,
    private val batchRepository: BatchRepository,
    private val userRepository: UserRepository,
    private val pantryAccessGuard: PantryAccessGuard,
    private val clock: Clock,
) {
    fun today(): LocalDate = LocalDate.now(clock)

    fun loadProduct(
        userId: UserId,
        productId: ProductId,
    ): Product {
        val product = productRepository.findById(productId) ?: throw ProductNotFoundException("Product not found")
        pantryAccessGuard.requireAccess(userId, product.pantryId)
        return product
    }

    fun loadBatch(
        userId: UserId,
        batchId: BatchId,
    ): Pair<Batch, Product> {
        val batch = batchRepository.findById(batchId) ?: throw BatchNotFoundException("Batch not found")
        val product = productRepository.findById(batch.productId) ?: throw ProductNotFoundException("Product of batch missing")
        pantryAccessGuard.requireAccess(userId, product.pantryId)
        return batch to product
    }

    fun productDto(
        product: Product,
        soonDays: Int = Freshness.DEFAULT_SOON_DAYS,
    ): ProductDto = productDto(product, batchRepository.findByProduct(product.id), soonDays)

    fun productDto(
        product: Product,
        batches: List<Batch>,
        soonDays: Int = Freshness.DEFAULT_SOON_DAYS,
    ): ProductDto = ProductDto.fromDomain(product, StockSummary.of(batches, today(), soonDays))

    /** DTOs for many products with one batch query: every in-stock batch of the pantry grouped by product. */
    fun productDtos(
        products: List<Product>,
        inStock: List<Batch>,
        soonDays: Int = Freshness.DEFAULT_SOON_DAYS,
    ): List<ProductDto> {
        val byProduct = inStock.groupBy { it.productId }
        return products.map { productDto(it, byProduct[it.id].orEmpty(), soonDays) }
    }

    fun batchDto(
        batch: Batch,
        soonDays: Int = Freshness.DEFAULT_SOON_DAYS,
    ): BatchDto = BatchDto.fromDomain(batch, today(), soonDays)

    fun movementDtos(movements: List<StockMovement>): List<StockMovementDto> {
        val products = productRepository.findByIds(movements.map { it.productId }.toSet())
        val users = movements.mapNotNull { it.userId }.toSet().associateWith { userRepository.findById(it)?.name }
        return movements.map { movement ->
            StockMovementDto.fromDomain(
                movement,
                productName = products[movement.productId]?.name?.value ?: "?",
                userName = movement.userId?.let { users[it] },
            )
        }
    }
}
