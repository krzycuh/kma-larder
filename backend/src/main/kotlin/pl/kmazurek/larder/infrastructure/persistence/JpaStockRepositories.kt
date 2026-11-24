package pl.kmazurek.larder.infrastructure.persistence

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import pl.kmazurek.larder.infrastructure.persistence.entity.BatchJpaEntity
import pl.kmazurek.larder.infrastructure.persistence.entity.ProductJpaEntity
import pl.kmazurek.larder.infrastructure.persistence.entity.StockMovementJpaEntity
import java.util.UUID

@Repository
interface JpaProductRepository : JpaRepository<ProductJpaEntity, UUID> {
    @Query(
        """
        SELECT p FROM ProductJpaEntity p
        WHERE p.pantryId = :pantryId
          AND (:includeArchived = true OR p.archived = false)
          AND (:category IS NULL OR p.category = :category)
          AND (:kind IS NULL OR p.kind = :kind)
          AND (:pattern IS NULL OR LOWER(p.name) LIKE :pattern OR p.normalizedName LIKE :pattern)
        ORDER BY p.name ASC
        """,
    )
    fun findByPantry(
        @Param("pantryId") pantryId: UUID,
        @Param("includeArchived") includeArchived: Boolean,
        @Param("category") category: String?,
        @Param("kind") kind: String?,
        @Param("pattern") pattern: String?,
    ): List<ProductJpaEntity>

    fun findByPantryIdAndBarcode(
        pantryId: UUID,
        barcode: String,
    ): ProductJpaEntity?

    fun findByPantryIdAndNormalizedName(
        pantryId: UUID,
        normalizedName: String,
    ): ProductJpaEntity?
}

@Repository
interface JpaBatchRepository : JpaRepository<BatchJpaEntity, UUID> {
    @Query("SELECT b FROM BatchJpaEntity b WHERE b.productId = :productId ORDER BY b.expiresOn ASC NULLS LAST, b.addedOn ASC")
    fun findByProduct(
        @Param("productId") productId: UUID,
    ): List<BatchJpaEntity>

    @Query(
        "SELECT b FROM BatchJpaEntity b WHERE b.pantryId = :pantryId AND b.quantity > 0 ORDER BY b.expiresOn ASC NULLS LAST, b.addedOn ASC",
    )
    fun findInStockByPantry(
        @Param("pantryId") pantryId: UUID,
    ): List<BatchJpaEntity>
}

@Repository
interface JpaStockMovementRepository : JpaRepository<StockMovementJpaEntity, UUID> {
    @Query(
        value = """
        SELECT m FROM StockMovementJpaEntity m
        WHERE m.pantryId = :pantryId AND (:productId IS NULL OR m.productId = :productId)
        ORDER BY m.occurredOn DESC, m.createdAt DESC
        """,
        countQuery = """
        SELECT COUNT(m) FROM StockMovementJpaEntity m
        WHERE m.pantryId = :pantryId AND (:productId IS NULL OR m.productId = :productId)
        """,
    )
    fun findByPantry(
        @Param("pantryId") pantryId: UUID,
        @Param("productId") productId: UUID?,
        pageable: Pageable,
    ): Page<StockMovementJpaEntity>

    @Query(
        """
        SELECT YEAR(m.occurredOn) AS year, SUM(m.quantity) AS units
        FROM StockMovementJpaEntity m
        WHERE m.productId = :productId AND m.type = :type
        GROUP BY YEAR(m.occurredOn)
        """,
    )
    fun unitsByYear(
        @Param("productId") productId: UUID,
        @Param("type") type: String,
    ): List<Array<Any>>
}
