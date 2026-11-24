package pl.kmazurek.larder.domain.repository

import pl.kmazurek.larder.domain.model.pantry.PantryId
import pl.kmazurek.larder.domain.model.pantry.ProductId
import pl.kmazurek.larder.domain.model.stock.MovementType
import pl.kmazurek.larder.domain.model.stock.StockMovement

/**
 * Repository interface (port) for the pantry's history of stock changes
 */
interface StockMovementRepository {
    fun save(movement: StockMovement): StockMovement

    /** Newest first; optionally only one product. Returns (movements, totalCount). */
    fun findByPantry(
        pantryId: PantryId,
        productId: ProductId?,
        page: Int,
        pageSize: Int,
    ): Pair<List<StockMovement>, Long>

    /** Units per calendar year for one movement type, e.g. how many jars were CONSUMED each year (positive numbers). */
    fun unitsByYear(
        productId: ProductId,
        type: MovementType,
    ): Map<Int, Int>
}
