package pl.kmazurek.larder.infrastructure.persistence.mapper

import pl.kmazurek.larder.domain.model.pantry.BatchId
import pl.kmazurek.larder.domain.model.pantry.PantryId
import pl.kmazurek.larder.domain.model.pantry.ProductId
import pl.kmazurek.larder.domain.model.pantry.StockMovementId
import pl.kmazurek.larder.domain.model.stock.ApproximateDate
import pl.kmazurek.larder.domain.model.stock.Barcode
import pl.kmazurek.larder.domain.model.stock.Batch
import pl.kmazurek.larder.domain.model.stock.MovementType
import pl.kmazurek.larder.domain.model.stock.Product
import pl.kmazurek.larder.domain.model.stock.ProductCategory
import pl.kmazurek.larder.domain.model.stock.ProductKind
import pl.kmazurek.larder.domain.model.stock.ProductName
import pl.kmazurek.larder.domain.model.stock.StockMovement
import pl.kmazurek.larder.domain.model.user.UserId
import pl.kmazurek.larder.infrastructure.persistence.entity.BatchJpaEntity
import pl.kmazurek.larder.infrastructure.persistence.entity.ProductJpaEntity
import pl.kmazurek.larder.infrastructure.persistence.entity.StockMovementJpaEntity

object ProductMapper {
    fun toDomain(jpa: ProductJpaEntity) =
        Product(
            id = ProductId(jpa.id),
            pantryId = PantryId(jpa.pantryId),
            name = ProductName(jpa.name),
            kind = ProductKind.valueOf(jpa.kind),
            category = ProductCategory.valueOf(jpa.category),
            barcode = jpa.barcode?.let { Barcode(it) },
            packageSize = jpa.packageSize,
            defaultShelfLifeMonths = jpa.defaultShelfLifeMonths,
            minQuantity = jpa.minQuantity,
            notes = jpa.notes,
            archived = jpa.archived,
            createdByUserId = jpa.createdByUserId?.let { UserId(it) },
            createdAt = jpa.createdAt,
            updatedAt = jpa.updatedAt,
        )

    fun toJpa(domain: Product) =
        ProductJpaEntity(
            id = domain.id.value,
            pantryId = domain.pantryId.value,
            name = domain.name.value,
            normalizedName = domain.name.normalized,
            kind = domain.kind.name,
            category = domain.category.name,
            barcode = domain.barcode?.value,
            packageSize = domain.packageSize,
            defaultShelfLifeMonths = domain.defaultShelfLifeMonths,
            minQuantity = domain.minQuantity,
            notes = domain.notes,
            archived = domain.archived,
            createdByUserId = domain.createdByUserId?.value,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt,
        )
}

object BatchMapper {
    fun toDomain(jpa: BatchJpaEntity) =
        Batch(
            id = BatchId(jpa.id),
            pantryId = PantryId(jpa.pantryId),
            productId = ProductId(jpa.productId),
            quantity = jpa.quantity,
            bestBefore = approximateDate(jpa.bestBeforeYear, jpa.bestBeforeMonth, jpa.bestBeforeDay),
            producedOn = approximateDate(jpa.producedYear, jpa.producedMonth, jpa.producedDay),
            expiresOn = jpa.expiresOn,
            location = jpa.location,
            notes = jpa.notes,
            addedOn = jpa.addedOn,
            createdAt = jpa.createdAt,
            updatedAt = jpa.updatedAt,
        )

    fun toJpa(domain: Batch) =
        BatchJpaEntity(
            id = domain.id.value,
            pantryId = domain.pantryId.value,
            productId = domain.productId.value,
            quantity = domain.quantity,
            bestBeforeYear = domain.bestBefore?.year?.toShort(),
            bestBeforeMonth = domain.bestBefore?.month?.toShort(),
            bestBeforeDay = domain.bestBefore?.day?.toShort(),
            producedYear = domain.producedOn?.year?.toShort(),
            producedMonth = domain.producedOn?.month?.toShort(),
            producedDay = domain.producedOn?.day?.toShort(),
            expiresOn = domain.expiresOn,
            location = domain.location,
            notes = domain.notes,
            addedOn = domain.addedOn,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt,
        )

    private fun approximateDate(
        year: Short?,
        month: Short?,
        day: Short?,
    ): ApproximateDate? = year?.let { ApproximateDate(it.toInt(), month?.toInt(), day?.toInt()) }
}

object StockMovementMapper {
    fun toDomain(jpa: StockMovementJpaEntity) =
        StockMovement(
            id = StockMovementId(jpa.id),
            pantryId = PantryId(jpa.pantryId),
            productId = ProductId(jpa.productId),
            batchId = jpa.batchId?.let { BatchId(it) },
            type = MovementType.valueOf(jpa.type),
            quantity = jpa.quantity,
            occurredOn = jpa.occurredOn,
            userId = jpa.userId?.let { UserId(it) },
            createdAt = jpa.createdAt,
        )

    fun toJpa(domain: StockMovement) =
        StockMovementJpaEntity(
            id = domain.id.value,
            pantryId = domain.pantryId.value,
            productId = domain.productId.value,
            batchId = domain.batchId?.value,
            type = domain.type.name,
            quantity = domain.quantity,
            occurredOn = domain.occurredOn,
            userId = domain.userId?.value,
            createdAt = domain.createdAt,
        )
}
