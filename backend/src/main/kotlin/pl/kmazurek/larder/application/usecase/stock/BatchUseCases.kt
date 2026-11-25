package pl.kmazurek.larder.application.usecase.stock

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pl.kmazurek.larder.application.dto.BatchDto
import pl.kmazurek.larder.application.dto.BatchRequest
import pl.kmazurek.larder.application.dto.QuantityChangeRequest
import pl.kmazurek.larder.application.dto.UpdateBatchRequest
import pl.kmazurek.larder.application.usecase.pantry.InsufficientStockException
import pl.kmazurek.larder.domain.model.pantry.BatchId
import pl.kmazurek.larder.domain.model.pantry.ProductId
import pl.kmazurek.larder.domain.model.stock.Batch
import pl.kmazurek.larder.domain.model.stock.StockMovement
import pl.kmazurek.larder.domain.model.user.UserId
import pl.kmazurek.larder.domain.repository.BatchRepository
import pl.kmazurek.larder.domain.repository.StockMovementRepository

/**
 * Use Case: put units on the shelf – a new batch of a product, recorded as an ADDED movement.
 */
@Service
class AddBatch(
    private val batchRepository: BatchRepository,
    private val movementRepository: StockMovementRepository,
    private val access: StockAccess,
) {
    @Transactional
    fun execute(
        userId: UserId,
        productId: ProductId,
        request: BatchRequest,
    ): BatchDto {
        val product = access.loadProduct(userId, productId)
        val addedOn = request.addedOn ?: access.today()
        val batch =
            Batch.create(
                product = product,
                quantity = request.quantity,
                bestBefore = request.bestBefore?.toDomain(),
                producedOn = request.producedOn?.toDomain(),
                location = request.location,
                notes = request.notes,
                addedOn = addedOn,
            )
        val saved = batchRepository.save(batch)
        movementRepository.save(StockMovement.added(saved, request.quantity, userId, addedOn))
        return access.batchDto(saved)
    }
}

/**
 * Use Case: correct a batch's dates, place or note (not its quantity – that goes through movements).
 */
@Service
class UpdateBatch(
    private val batchRepository: BatchRepository,
    private val access: StockAccess,
) {
    @Transactional
    fun execute(
        userId: UserId,
        batchId: BatchId,
        request: UpdateBatchRequest,
    ): BatchDto {
        val (batch, product) = access.loadBatch(userId, batchId)
        val updated = batch.update(product, request.bestBefore?.toDomain(), request.producedOn?.toDomain(), request.location, request.notes)
        return access.batchDto(batchRepository.save(updated))
    }
}

/**
 * Use Case: eat it – takes units off a batch (CONSUMED).
 */
@Service
class ConsumeBatch(
    private val batchRepository: BatchRepository,
    private val movementRepository: StockMovementRepository,
    private val access: StockAccess,
) {
    @Transactional
    fun execute(
        userId: UserId,
        batchId: BatchId,
        request: QuantityChangeRequest,
    ): BatchDto {
        val (batch, _) = access.loadBatch(userId, batchId)
        val taken = take(batch, request.quantity)
        val saved = batchRepository.save(taken)
        movementRepository.save(StockMovement.consumed(saved, request.quantity, userId, request.occurredOn ?: access.today()))
        return access.batchDto(saved)
    }
}

/**
 * Use Case: throw it away – takes units off a batch (DISCARDED), e.g. past its date.
 */
@Service
class DiscardBatch(
    private val batchRepository: BatchRepository,
    private val movementRepository: StockMovementRepository,
    private val access: StockAccess,
) {
    @Transactional
    fun execute(
        userId: UserId,
        batchId: BatchId,
        request: QuantityChangeRequest,
    ): BatchDto {
        val (batch, _) = access.loadBatch(userId, batchId)
        val taken = take(batch, request.quantity)
        val saved = batchRepository.save(taken)
        movementRepository.save(StockMovement.discarded(saved, request.quantity, userId, request.occurredOn ?: access.today()))
        return access.batchDto(saved)
    }
}

/**
 * Use Case: stock-taking – sets the quantity to what is really on the shelf; the difference is an ADJUSTED movement.
 */
@Service
class AdjustBatch(
    private val batchRepository: BatchRepository,
    private val movementRepository: StockMovementRepository,
    private val access: StockAccess,
) {
    @Transactional
    fun execute(
        userId: UserId,
        batchId: BatchId,
        request: QuantityChangeRequest,
    ): BatchDto {
        val (batch, _) = access.loadBatch(userId, batchId)
        val delta = request.quantity - batch.quantity
        if (delta == 0) return access.batchDto(batch)
        val saved = batchRepository.save(batch.adjustTo(request.quantity))
        movementRepository.save(StockMovement.adjusted(saved, delta, userId, request.occurredOn ?: access.today()))
        return access.batchDto(saved)
    }
}

/**
 * Use Case: remove a batch entirely (its movements stay, without the batch reference).
 */
@Service
class RemoveBatch(
    private val batchRepository: BatchRepository,
    private val access: StockAccess,
) {
    @Transactional
    fun execute(
        userId: UserId,
        batchId: BatchId,
    ) {
        access.loadBatch(userId, batchId)
        batchRepository.deleteById(batchId)
    }
}

private fun take(
    batch: Batch,
    units: Int,
): Batch {
    if (units <= 0) throw IllegalArgumentException("Quantity must be at least 1")
    if (units > batch.quantity) throw InsufficientStockException("Only ${batch.quantity} unit(s) left, cannot take $units")
    return batch.take(units)
}
