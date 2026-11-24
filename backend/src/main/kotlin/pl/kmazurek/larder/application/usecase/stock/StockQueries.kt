package pl.kmazurek.larder.application.usecase.stock

import org.springframework.stereotype.Service
import pl.kmazurek.larder.application.dto.BatchWithProductDto
import pl.kmazurek.larder.application.dto.DashboardDto
import pl.kmazurek.larder.application.dto.PagedResponse
import pl.kmazurek.larder.application.dto.ReplenishmentItemDto
import pl.kmazurek.larder.application.dto.StockMovementDto
import pl.kmazurek.larder.application.usecase.pantry.PantryAccessGuard
import pl.kmazurek.larder.domain.model.pantry.PantryId
import pl.kmazurek.larder.domain.model.pantry.ProductId
import pl.kmazurek.larder.domain.model.stock.Freshness
import pl.kmazurek.larder.domain.model.stock.FreshnessStatus
import pl.kmazurek.larder.domain.model.user.UserId
import pl.kmazurek.larder.domain.repository.BatchRepository
import pl.kmazurek.larder.domain.repository.ProductFilter
import pl.kmazurek.larder.domain.repository.ProductRepository
import pl.kmazurek.larder.domain.repository.StockMovementRepository

data class ExpiringQuery(
    val userId: UserId,
    val pantryId: PantryId,
    /** Only batches expiring within this many days (expired included); null = every dated batch */
    val withinDays: Int? = null,
    val expiredOnly: Boolean = false,
    val undatedOnly: Boolean = false,
    val soonDays: Int = Freshness.DEFAULT_SOON_DAYS,
)

/**
 * Use Case: what to use up first – in-stock batches sorted by best-before date (expired first, undated last).
 */
@Service
class ListExpiring(
    private val productRepository: ProductRepository,
    private val batchRepository: BatchRepository,
    private val pantryAccessGuard: PantryAccessGuard,
    private val access: StockAccess,
) {
    fun execute(query: ExpiringQuery): List<BatchWithProductDto> {
        pantryAccessGuard.requireAccess(query.userId, query.pantryId)
        val today = access.today()
        val inStock = batchRepository.findInStockByPantry(query.pantryId)
        val products = productRepository.findByIds(inStock.map { it.productId }.toSet()).filterValues { !it.archived }
        val horizon = query.withinDays?.let { today.plusDays(it.toLong()) }
        val selected =
            inStock
                .filter { it.productId in products }
                .filter { batch ->
                    when {
                        query.undatedOnly -> batch.expiresOn == null
                        query.expiredOnly -> batch.expiresOn != null && batch.expiresOn.isBefore(today)
                        horizon != null -> batch.expiresOn != null && !batch.expiresOn.isAfter(horizon)
                        else -> batch.expiresOn != null
                    }
                }
                .sortedWith(compareBy(nullsLast()) { it.expiresOn })
        val productDtos = access.productDtos(products.values.toList(), inStock, query.soonDays).associateBy { it.id }
        return selected.map { batch ->
            BatchWithProductDto.of(access.batchDto(batch, query.soonDays), productDtos.getValue(batch.productId.toString()))
        }
    }
}

/**
 * Use Case: the shopping list – active products with fewer units than their minimum.
 */
@Service
class ListReplenishment(
    private val productRepository: ProductRepository,
    private val batchRepository: BatchRepository,
    private val pantryAccessGuard: PantryAccessGuard,
    private val access: StockAccess,
) {
    fun execute(
        userId: UserId,
        pantryId: PantryId,
    ): List<ReplenishmentItemDto> {
        pantryAccessGuard.requireAccess(userId, pantryId)
        val products = productRepository.findByPantry(pantryId, ProductFilter()).filter { it.minQuantity > 0 }
        return access.productDtos(products, batchRepository.findInStockByPantry(pantryId))
            .filter { it.belowMinimum }
            .map { ReplenishmentItemDto(it, it.quantity, it.minQuantity, it.minQuantity - it.quantity) }
            .sortedWith(compareByDescending<ReplenishmentItemDto> { it.missing }.thenBy { it.product.name.lowercase() })
    }
}

/**
 * Use Case: the pantry's history, newest first, optionally for one product.
 */
@Service
class ListMovements(
    private val movementRepository: StockMovementRepository,
    private val pantryAccessGuard: PantryAccessGuard,
    private val access: StockAccess,
) {
    fun execute(
        userId: UserId,
        pantryId: PantryId,
        productId: ProductId?,
        page: Int,
        pageSize: Int,
    ): PagedResponse<StockMovementDto> {
        pantryAccessGuard.requireAccess(userId, pantryId)
        val size = pageSize.coerceIn(1, 100)
        val (movements, total) = movementRepository.findByPantry(pantryId, productId, page.coerceAtLeast(0), size)
        return PagedResponse.of(access.movementDtos(movements), page, size, total)
    }
}

/**
 * Use Case: data for the home screen – counts, what to use up first, what to buy, what changed lately.
 */
@Service
class GetDashboard(
    private val productRepository: ProductRepository,
    private val batchRepository: BatchRepository,
    private val movementRepository: StockMovementRepository,
    private val pantryAccessGuard: PantryAccessGuard,
    private val listExpiring: ListExpiring,
    private val listReplenishment: ListReplenishment,
    private val access: StockAccess,
) {
    fun execute(
        userId: UserId,
        pantryId: PantryId,
        soonDays: Int = Freshness.DEFAULT_SOON_DAYS,
    ): DashboardDto {
        pantryAccessGuard.requireAccess(userId, pantryId)
        val products = productRepository.findByPantry(pantryId, ProductFilter())
        val inStock = batchRepository.findInStockByPantry(pantryId)
        val activeIds = products.map { it.id }.toSet()
        val today = access.today()
        val statuses = inStock.filter { it.productId in activeIds }.map { it.freshness(today, soonDays) }
        val expiring = listExpiring.execute(ExpiringQuery(userId, pantryId, withinDays = soonDays, soonDays = soonDays))
        val replenishment = listReplenishment.execute(userId, pantryId)
        val (recent, _) = movementRepository.findByPantry(pantryId, null, 0, RECENT_MOVEMENTS)
        return DashboardDto(
            pantryId = pantryId.toString(),
            soonDays = soonDays,
            productCount = products.size,
            unitCount = inStock.filter { it.productId in activeIds }.sumOf { it.quantity },
            expiredCount = statuses.count { it == FreshnessStatus.EXPIRED },
            expiringSoonCount = statuses.count { it == FreshnessStatus.EXPIRING_SOON },
            belowMinimumCount = replenishment.size,
            expiring = expiring.take(TOP),
            belowMinimum = replenishment.take(TOP),
            recentMovements = access.movementDtos(recent),
        )
    }

    companion object {
        const val TOP = 5
        const val RECENT_MOVEMENTS = 5
    }
}
