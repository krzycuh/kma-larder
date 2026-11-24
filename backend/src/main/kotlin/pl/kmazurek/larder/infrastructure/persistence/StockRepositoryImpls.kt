package pl.kmazurek.larder.infrastructure.persistence

import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import pl.kmazurek.larder.domain.model.pantry.BatchId
import pl.kmazurek.larder.domain.model.pantry.PantryId
import pl.kmazurek.larder.domain.model.pantry.ProductId
import pl.kmazurek.larder.domain.model.stock.Barcode
import pl.kmazurek.larder.domain.model.stock.Batch
import pl.kmazurek.larder.domain.model.stock.MovementType
import pl.kmazurek.larder.domain.model.stock.Product
import pl.kmazurek.larder.domain.model.stock.ProductName
import pl.kmazurek.larder.domain.model.stock.StockMovement
import pl.kmazurek.larder.domain.repository.BatchRepository
import pl.kmazurek.larder.domain.repository.ProductFilter
import pl.kmazurek.larder.domain.repository.ProductRepository
import pl.kmazurek.larder.domain.repository.StockMovementRepository
import pl.kmazurek.larder.infrastructure.persistence.mapper.BatchMapper
import pl.kmazurek.larder.infrastructure.persistence.mapper.ProductMapper
import pl.kmazurek.larder.infrastructure.persistence.mapper.StockMovementMapper
import kotlin.math.abs

@Component
class ProductRepositoryImpl(
    private val jpaRepository: JpaProductRepository,
) : ProductRepository {
    override fun findById(id: ProductId): Product? = jpaRepository.findById(id.value).map { ProductMapper.toDomain(it) }.orElse(null)

    override fun findByIds(ids: Collection<ProductId>): Map<ProductId, Product> {
        if (ids.isEmpty()) return emptyMap()
        return jpaRepository.findAllById(ids.map { it.value }).map { ProductMapper.toDomain(it) }.associateBy { it.id }
    }

    override fun findByPantry(
        pantryId: PantryId,
        filter: ProductFilter,
    ): List<Product> {
        // Matches the typed phrase against the name and its normalized form, so "ogorki" finds "Ogórki kiszone"
        val pattern = filter.searchTerm?.let { "%${ProductName.normalize(it)}%" }?.takeIf { it.length > 2 }
        return jpaRepository.findByPantry(
            pantryId = pantryId.value,
            includeArchived = filter.includeArchived,
            category = filter.category?.name,
            kind = filter.kind?.name,
            pattern = pattern,
        ).map { ProductMapper.toDomain(it) }
    }

    override fun findByBarcode(
        pantryId: PantryId,
        barcode: Barcode,
    ): Product? = jpaRepository.findByPantryIdAndBarcode(pantryId.value, barcode.value)?.let { ProductMapper.toDomain(it) }

    override fun findByNormalizedName(
        pantryId: PantryId,
        normalizedName: String,
    ): Product? = jpaRepository.findByPantryIdAndNormalizedName(pantryId.value, normalizedName)?.let { ProductMapper.toDomain(it) }

    override fun save(product: Product): Product = ProductMapper.toDomain(jpaRepository.save(ProductMapper.toJpa(product)))

    override fun deleteById(id: ProductId) = jpaRepository.deleteById(id.value)
}

@Component
class BatchRepositoryImpl(
    private val jpaRepository: JpaBatchRepository,
) : BatchRepository {
    override fun findById(id: BatchId): Batch? = jpaRepository.findById(id.value).map { BatchMapper.toDomain(it) }.orElse(null)

    override fun findByProduct(productId: ProductId): List<Batch> =
        jpaRepository.findByProduct(productId.value).map {
            BatchMapper.toDomain(it)
        }

    override fun findInStockByPantry(pantryId: PantryId): List<Batch> =
        jpaRepository.findInStockByPantry(pantryId.value).map { BatchMapper.toDomain(it) }

    override fun save(batch: Batch): Batch = BatchMapper.toDomain(jpaRepository.save(BatchMapper.toJpa(batch)))

    override fun saveAll(batches: Collection<Batch>): List<Batch> =
        jpaRepository.saveAll(batches.map { BatchMapper.toJpa(it) }).map { BatchMapper.toDomain(it) }

    override fun deleteById(id: BatchId) = jpaRepository.deleteById(id.value)
}

@Component
class StockMovementRepositoryImpl(
    private val jpaRepository: JpaStockMovementRepository,
) : StockMovementRepository {
    override fun save(movement: StockMovement): StockMovement =
        StockMovementMapper.toDomain(jpaRepository.save(StockMovementMapper.toJpa(movement)))

    override fun findByPantry(
        pantryId: PantryId,
        productId: ProductId?,
        page: Int,
        pageSize: Int,
    ): Pair<List<StockMovement>, Long> {
        val result = jpaRepository.findByPantry(pantryId.value, productId?.value, PageRequest.of(page, pageSize))
        return result.content.map { StockMovementMapper.toDomain(it) } to result.totalElements
    }

    override fun unitsByYear(
        productId: ProductId,
        type: MovementType,
    ): Map<Int, Int> =
        jpaRepository.unitsByYear(productId.value, type.name).associate { row ->
            (row[0] as Number).toInt() to abs((row[1] as Number).toInt())
        }
}
