package pl.kmazurek.larder.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "products")
class ProductJpaEntity(
    @Id
    @Column(columnDefinition = "UUID")
    var id: UUID = UUID.randomUUID(),
    @Column(name = "pantry_id", nullable = false, columnDefinition = "UUID")
    var pantryId: UUID,
    @Column(nullable = false, length = 255)
    var name: String,
    @Column(name = "normalized_name", nullable = false, length = 255)
    var normalizedName: String,
    @Column(nullable = false, length = 20)
    var kind: String,
    @Column(nullable = false, length = 30)
    var category: String,
    @Column(length = 14)
    var barcode: String? = null,
    @Column(name = "package_size", length = 50)
    var packageSize: String? = null,
    @Column(name = "default_shelf_life_months")
    var defaultShelfLifeMonths: Int? = null,
    @Column(name = "min_quantity", nullable = false)
    var minQuantity: Int = 0,
    @Column(columnDefinition = "TEXT")
    var notes: String? = null,
    @Column(nullable = false)
    var archived: Boolean = false,
    @Column(name = "created_by_user_id", columnDefinition = "UUID")
    var createdByUserId: UUID? = null,
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
)

@Entity
@Table(name = "batches")
class BatchJpaEntity(
    @Id
    @Column(columnDefinition = "UUID")
    var id: UUID = UUID.randomUUID(),
    @Column(name = "pantry_id", nullable = false, columnDefinition = "UUID")
    var pantryId: UUID,
    @Column(name = "product_id", nullable = false, columnDefinition = "UUID")
    var productId: UUID,
    @Column(nullable = false)
    var quantity: Int,
    @Column(name = "best_before_year")
    var bestBeforeYear: Short? = null,
    @Column(name = "best_before_month")
    var bestBeforeMonth: Short? = null,
    @Column(name = "best_before_day")
    var bestBeforeDay: Short? = null,
    @Column(name = "produced_year")
    var producedYear: Short? = null,
    @Column(name = "produced_month")
    var producedMonth: Short? = null,
    @Column(name = "produced_day")
    var producedDay: Short? = null,
    @Column(name = "expires_on")
    var expiresOn: LocalDate? = null,
    @Column(length = 100)
    var location: String? = null,
    @Column(columnDefinition = "TEXT")
    var notes: String? = null,
    @Column(name = "added_on", nullable = false)
    var addedOn: LocalDate = LocalDate.now(),
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
)

@Entity
@Table(name = "stock_movements")
class StockMovementJpaEntity(
    @Id
    @Column(columnDefinition = "UUID")
    var id: UUID = UUID.randomUUID(),
    @Column(name = "pantry_id", nullable = false, columnDefinition = "UUID")
    var pantryId: UUID,
    @Column(name = "product_id", nullable = false, columnDefinition = "UUID")
    var productId: UUID,
    @Column(name = "batch_id", columnDefinition = "UUID")
    var batchId: UUID? = null,
    @Column(nullable = false, length = 20)
    var type: String,
    @Column(nullable = false)
    var quantity: Int,
    @Column(name = "occurred_on", nullable = false)
    var occurredOn: LocalDate,
    @Column(name = "user_id", columnDefinition = "UUID")
    var userId: UUID? = null,
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),
)
