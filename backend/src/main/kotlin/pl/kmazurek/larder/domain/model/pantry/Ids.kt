package pl.kmazurek.larder.domain.model.pantry

import java.util.UUID

@JvmInline
value class PantryId(val value: UUID) {
    companion object {
        fun generate() = PantryId(UUID.randomUUID())

        fun fromString(value: String) = PantryId(UUID.fromString(value))
    }

    override fun toString() = value.toString()
}

@JvmInline
value class ProductId(val value: UUID) {
    companion object {
        fun generate() = ProductId(UUID.randomUUID())

        fun fromString(value: String) = ProductId(UUID.fromString(value))
    }

    override fun toString() = value.toString()
}

@JvmInline
value class BatchId(val value: UUID) {
    companion object {
        fun generate() = BatchId(UUID.randomUUID())

        fun fromString(value: String) = BatchId(UUID.fromString(value))
    }

    override fun toString() = value.toString()
}

@JvmInline
value class StockMovementId(val value: UUID) {
    companion object {
        fun generate() = StockMovementId(UUID.randomUUID())

        fun fromString(value: String) = StockMovementId(UUID.fromString(value))
    }

    override fun toString() = value.toString()
}
