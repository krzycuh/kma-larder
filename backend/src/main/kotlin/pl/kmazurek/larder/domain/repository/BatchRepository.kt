package pl.kmazurek.larder.domain.repository

import pl.kmazurek.larder.domain.model.pantry.BatchId
import pl.kmazurek.larder.domain.model.pantry.PantryId
import pl.kmazurek.larder.domain.model.pantry.ProductId
import pl.kmazurek.larder.domain.model.stock.Batch

/**
 * Repository interface (port) for the Batch aggregate
 */
interface BatchRepository {
    fun findById(id: BatchId): Batch?

    /** Every batch of a product, including empty ones, soonest expiry first (undated last). */
    fun findByProduct(productId: ProductId): List<Batch>

    /** Batches with at least one unit, across the whole pantry, soonest expiry first (undated last). */
    fun findInStockByPantry(pantryId: PantryId): List<Batch>

    fun save(batch: Batch): Batch

    fun saveAll(batches: Collection<Batch>): List<Batch>

    fun deleteById(id: BatchId)
}
