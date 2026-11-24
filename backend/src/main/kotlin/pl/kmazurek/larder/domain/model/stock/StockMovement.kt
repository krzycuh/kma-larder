package pl.kmazurek.larder.domain.model.stock

import pl.kmazurek.larder.domain.model.pantry.BatchId
import pl.kmazurek.larder.domain.model.pantry.PantryId
import pl.kmazurek.larder.domain.model.pantry.ProductId
import pl.kmazurek.larder.domain.model.pantry.StockMovementId
import pl.kmazurek.larder.domain.model.user.UserId
import java.time.LocalDate
import java.time.LocalDateTime

enum class MovementType {
    ADDED,
    CONSUMED,
    DISCARDED,
    ADJUSTED,
}

/**
 * Entity (Stock context): one change of a batch's quantity, kept forever as the pantry's history.
 * [quantity] is the signed change: +6 when six jars were added, -1 when one was eaten.
 * Answers "how many jars of jam do we eat a year", which says how many to make.
 */
data class StockMovement(
    val id: StockMovementId,
    val pantryId: PantryId,
    val productId: ProductId,
    val batchId: BatchId?,
    val type: MovementType,
    val quantity: Int,
    val occurredOn: LocalDate,
    val userId: UserId?,
    val createdAt: LocalDateTime,
) {
    init {
        require(quantity != 0) { "A movement must change the quantity" }
        when (type) {
            MovementType.ADDED -> require(quantity > 0) { "ADDED must be positive" }
            MovementType.CONSUMED, MovementType.DISCARDED -> require(quantity < 0) { "$type must be negative" }
            MovementType.ADJUSTED -> Unit
        }
    }

    companion object {
        fun added(
            batch: Batch,
            units: Int,
            userId: UserId?,
            occurredOn: LocalDate = LocalDate.now(),
        ) = of(batch, MovementType.ADDED, units, userId, occurredOn)

        fun consumed(
            batch: Batch,
            units: Int,
            userId: UserId?,
            occurredOn: LocalDate = LocalDate.now(),
        ) = of(batch, MovementType.CONSUMED, -units, userId, occurredOn)

        fun discarded(
            batch: Batch,
            units: Int,
            userId: UserId?,
            occurredOn: LocalDate = LocalDate.now(),
        ) = of(batch, MovementType.DISCARDED, -units, userId, occurredOn)

        fun adjusted(
            batch: Batch,
            delta: Int,
            userId: UserId?,
            occurredOn: LocalDate = LocalDate.now(),
        ) = of(batch, MovementType.ADJUSTED, delta, userId, occurredOn)

        private fun of(
            batch: Batch,
            type: MovementType,
            quantity: Int,
            userId: UserId?,
            occurredOn: LocalDate,
        ) = StockMovement(
            id = StockMovementId.generate(),
            pantryId = batch.pantryId,
            productId = batch.productId,
            batchId = batch.id,
            type = type,
            quantity = quantity,
            occurredOn = occurredOn,
            userId = userId,
            createdAt = LocalDateTime.now(),
        )
    }
}
