package pl.kmazurek.larder.application.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import pl.kmazurek.larder.application.service.ProductMetadata
import pl.kmazurek.larder.domain.model.stock.ApproximateDate
import pl.kmazurek.larder.domain.model.stock.Batch
import pl.kmazurek.larder.domain.model.stock.Freshness
import pl.kmazurek.larder.domain.model.stock.FreshnessStatus
import pl.kmazurek.larder.domain.model.stock.MovementType
import pl.kmazurek.larder.domain.model.stock.Product
import pl.kmazurek.larder.domain.model.stock.ProductCategory
import pl.kmazurek.larder.domain.model.stock.ProductKind
import pl.kmazurek.larder.domain.model.stock.StockMovement
import java.time.LocalDate
import java.time.LocalDateTime

data class ApproximateDateDto(
    @field:NotNull(message = "Year is required")
    @field:Min(value = ApproximateDate.MIN_YEAR.toLong(), message = "Year is out of range")
    @field:Max(value = ApproximateDate.MAX_YEAR.toLong(), message = "Year is out of range")
    val year: Int,
    @field:Min(1) @field:Max(12)
    val month: Int? = null,
    @field:Min(1) @field:Max(31)
    val day: Int? = null,
) {
    fun toDomain() = ApproximateDate(year, month, day)

    companion object {
        fun fromDomain(date: ApproximateDate?) = date?.let { ApproximateDateDto(it.year, it.month, it.day) }
    }
}

/**
 * How much of a product is on the shelves right now, summed over its batches with units left.
 */
data class StockSummary(
    val quantity: Int,
    val batchCount: Int,
    val nextExpiresOn: LocalDate?,
    val freshness: FreshnessStatus,
) {
    companion object {
        val EMPTY = StockSummary(0, 0, null, FreshnessStatus.UNKNOWN)

        fun of(
            batches: List<Batch>,
            today: LocalDate,
            soonDays: Int,
        ): StockSummary {
            val inStock = batches.filter { it.quantity > 0 }
            if (inStock.isEmpty()) return EMPTY
            return StockSummary(
                quantity = inStock.sumOf { it.quantity },
                batchCount = inStock.size,
                nextExpiresOn = inStock.mapNotNull { it.expiresOn }.minOrNull(),
                freshness = Freshness.worst(inStock.map { it.freshness(today, soonDays) }),
            )
        }
    }
}

data class ProductDto(
    val id: String,
    val pantryId: String,
    val name: String,
    val kind: ProductKind,
    val category: ProductCategory,
    val barcode: String?,
    val packageSize: String?,
    val defaultShelfLifeMonths: Int?,
    val minQuantity: Int,
    val notes: String?,
    val imageUrl: String?,
    val archived: Boolean,
    val quantity: Int,
    val batchCount: Int,
    val nextExpiresOn: LocalDate?,
    val freshness: FreshnessStatus,
    val belowMinimum: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        fun fromDomain(
            product: Product,
            stock: StockSummary,
        ) = ProductDto(
            id = product.id.toString(),
            pantryId = product.pantryId.toString(),
            name = product.name.value,
            kind = product.kind,
            category = product.category,
            barcode = product.barcode?.value,
            packageSize = product.packageSize,
            defaultShelfLifeMonths = product.defaultShelfLifeMonths,
            minQuantity = product.minQuantity,
            notes = product.notes,
            imageUrl = product.imageUrl,
            archived = product.archived,
            quantity = stock.quantity,
            batchCount = stock.batchCount,
            nextExpiresOn = stock.nextExpiresOn,
            freshness = stock.freshness,
            belowMinimum = product.minQuantity > 0 && stock.quantity < product.minQuantity,
            createdAt = product.createdAt,
            updatedAt = product.updatedAt,
        )
    }
}

data class BatchDto(
    val id: String,
    val pantryId: String,
    val productId: String,
    val quantity: Int,
    val bestBefore: ApproximateDateDto?,
    val producedOn: ApproximateDateDto?,
    val expiresOn: LocalDate?,
    val freshness: FreshnessStatus,
    val daysLeft: Long?,
    val location: String?,
    val notes: String?,
    val addedOn: LocalDate,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        fun fromDomain(
            batch: Batch,
            today: LocalDate,
            soonDays: Int,
        ) = BatchDto(
            id = batch.id.toString(),
            pantryId = batch.pantryId.toString(),
            productId = batch.productId.toString(),
            quantity = batch.quantity,
            bestBefore = ApproximateDateDto.fromDomain(batch.bestBefore),
            producedOn = ApproximateDateDto.fromDomain(batch.producedOn),
            expiresOn = batch.expiresOn,
            freshness = batch.freshness(today, soonDays),
            daysLeft = Freshness.daysLeft(batch.expiresOn, today),
            location = batch.location,
            notes = batch.notes,
            addedOn = batch.addedOn,
            createdAt = batch.createdAt,
            updatedAt = batch.updatedAt,
        )
    }
}

/** A batch with its product, for lists spanning many products (expiring, dashboard). */
data class BatchWithProductDto(
    val id: String,
    val pantryId: String,
    val productId: String,
    val quantity: Int,
    val bestBefore: ApproximateDateDto?,
    val producedOn: ApproximateDateDto?,
    val expiresOn: LocalDate?,
    val freshness: FreshnessStatus,
    val daysLeft: Long?,
    val location: String?,
    val notes: String?,
    val addedOn: LocalDate,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val product: ProductDto,
) {
    companion object {
        fun of(
            batch: BatchDto,
            product: ProductDto,
        ) = BatchWithProductDto(
            id = batch.id,
            pantryId = batch.pantryId,
            productId = batch.productId,
            quantity = batch.quantity,
            bestBefore = batch.bestBefore,
            producedOn = batch.producedOn,
            expiresOn = batch.expiresOn,
            freshness = batch.freshness,
            daysLeft = batch.daysLeft,
            location = batch.location,
            notes = batch.notes,
            addedOn = batch.addedOn,
            createdAt = batch.createdAt,
            updatedAt = batch.updatedAt,
            product = product,
        )
    }
}

data class StockMovementDto(
    val id: String,
    val pantryId: String,
    val productId: String,
    val productName: String,
    val batchId: String?,
    val type: MovementType,
    val quantity: Int,
    val occurredOn: LocalDate,
    val userName: String?,
    val createdAt: LocalDateTime,
) {
    companion object {
        fun fromDomain(
            movement: StockMovement,
            productName: String,
            userName: String?,
        ) = StockMovementDto(
            id = movement.id.toString(),
            pantryId = movement.pantryId.toString(),
            productId = movement.productId.toString(),
            productName = productName,
            batchId = movement.batchId?.toString(),
            type = movement.type,
            quantity = movement.quantity,
            occurredOn = movement.occurredOn,
            userName = userName,
            createdAt = movement.createdAt,
        )
    }
}

data class YearQuantityDto(val year: Int, val quantity: Int)

data class ProductDetailDto(
    val product: ProductDto,
    val batches: List<BatchDto>,
    val recentMovements: List<StockMovementDto>,
    val consumedByYear: List<YearQuantityDto>,
)

data class ReplenishmentItemDto(
    val product: ProductDto,
    val quantity: Int,
    val minQuantity: Int,
    val missing: Int,
)

data class DashboardDto(
    val pantryId: String,
    val soonDays: Int,
    val productCount: Int,
    val unitCount: Int,
    val expiredCount: Int,
    val expiringSoonCount: Int,
    val belowMinimumCount: Int,
    val expiring: List<BatchWithProductDto>,
    val belowMinimum: List<ReplenishmentItemDto>,
    val recentMovements: List<StockMovementDto>,
)

data class ProductRequest(
    @field:NotBlank(message = "Name is required")
    @field:Size(max = 255, message = "Name cannot exceed 255 characters")
    val name: String,
    @field:NotNull(message = "Kind is required")
    val kind: ProductKind,
    @field:NotNull(message = "Category is required")
    val category: ProductCategory,
    @field:Size(max = 20, message = "Barcode is too long")
    val barcode: String? = null,
    @field:Size(max = 50, message = "Package size cannot exceed 50 characters")
    val packageSize: String? = null,
    @field:Min(value = 1, message = "Shelf life must be at least 1 month")
    @field:Max(value = 600, message = "Shelf life cannot exceed 600 months")
    val defaultShelfLifeMonths: Int? = null,
    @field:Min(value = 0, message = "Minimum quantity cannot be negative")
    val minQuantity: Int = 0,
    @field:Size(max = 2000, message = "Notes cannot exceed 2000 characters")
    val notes: String? = null,
    @field:Size(max = 500, message = "Image URL cannot exceed 500 characters")
    val imageUrl: String? = null,
    val archived: Boolean = false,
)

data class BatchRequest(
    @field:Min(value = 1, message = "Quantity must be at least 1")
    @field:Max(value = 100000, message = "Quantity is too large")
    val quantity: Int,
    @field:Valid
    val bestBefore: ApproximateDateDto? = null,
    @field:Valid
    val producedOn: ApproximateDateDto? = null,
    @field:Size(max = 100, message = "Location cannot exceed 100 characters")
    val location: String? = null,
    @field:Size(max = 2000, message = "Notes cannot exceed 2000 characters")
    val notes: String? = null,
    val addedOn: LocalDate? = null,
)

data class UpdateBatchRequest(
    @field:Valid
    val bestBefore: ApproximateDateDto? = null,
    @field:Valid
    val producedOn: ApproximateDateDto? = null,
    @field:Size(max = 100, message = "Location cannot exceed 100 characters")
    val location: String? = null,
    @field:Size(max = 2000, message = "Notes cannot exceed 2000 characters")
    val notes: String? = null,
)

/** Consume / discard: how many units; adjust: the new absolute quantity. */
data class QuantityChangeRequest(
    @field:Min(value = 0, message = "Quantity cannot be negative")
    @field:Max(value = 100000, message = "Quantity is too large")
    val quantity: Int,
    val occurredOn: LocalDate? = null,
)

data class ProductMetadataDto(
    val barcode: String,
    val name: String,
    val brand: String?,
    val packageSize: String?,
    val imageUrl: String?,
    /** The pantry category the external database's tags point at; null when they do not match any */
    val category: ProductCategory?,
    val source: String,
) {
    companion object {
        fun fromMetadata(metadata: ProductMetadata) =
            ProductMetadataDto(
                metadata.barcode,
                metadata.name,
                metadata.brand,
                metadata.packageSize,
                metadata.imageUrl,
                metadata.category,
                metadata.source,
            )
    }
}

/** What a scanned code means: a product already in the pantry, and/or what an external database knows about it. */
data class BarcodeLookupDto(
    val barcode: String,
    val product: ProductDto?,
    val metadata: ProductMetadataDto?,
)
