package pl.kmazurek.larder.application.usecase.stock

import io.mockk.every
import io.mockk.mockk
import pl.kmazurek.larder.application.usecase.pantry.PantryAccessGuard
import pl.kmazurek.larder.domain.model.pantry.BatchId
import pl.kmazurek.larder.domain.model.pantry.Pantry
import pl.kmazurek.larder.domain.model.pantry.PantryId
import pl.kmazurek.larder.domain.model.pantry.PantryRole
import pl.kmazurek.larder.domain.model.pantry.ProductId
import pl.kmazurek.larder.domain.model.stock.Barcode
import pl.kmazurek.larder.domain.model.stock.Batch
import pl.kmazurek.larder.domain.model.stock.MovementType
import pl.kmazurek.larder.domain.model.stock.Product
import pl.kmazurek.larder.domain.model.stock.ProductName
import pl.kmazurek.larder.domain.model.stock.StockMovement
import pl.kmazurek.larder.domain.model.user.Email
import pl.kmazurek.larder.domain.model.user.HashedPassword
import pl.kmazurek.larder.domain.model.user.User
import pl.kmazurek.larder.domain.model.user.UserId
import pl.kmazurek.larder.domain.repository.BatchRepository
import pl.kmazurek.larder.domain.repository.PantryRepository
import pl.kmazurek.larder.domain.repository.ProductFilter
import pl.kmazurek.larder.domain.repository.ProductRepository
import pl.kmazurek.larder.domain.repository.StockMovementRepository
import pl.kmazurek.larder.domain.repository.UserRepository
import pl.kmazurek.larder.domain.service.PantryAccessPolicy
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.abs

/**
 * In-memory stand-ins for the stock repositories plus a fixed clock: use case tests read like a story
 * ("add six jars, eat one, check the dashboard") instead of a pile of mock expectations.
 */
class StockTestSupport(val today: LocalDate = LocalDate.of(2026, 11, 24)) {
    val owner = User.create(Email("anna@example.com"), HashedPassword("h"), "Anna")
    val outsider = User.create(Email("ola@example.com"), HashedPassword("h"), "Ola")
    val pantry = Pantry.create("Spiżarnia Anny", owner.id)
    val clock: Clock = Clock.fixed(today.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC)

    val products = linkedMapOf<String, Product>()
    val batches = linkedMapOf<String, Batch>()
    val movements = mutableListOf<StockMovement>()

    val productRepository: ProductRepository =
        object : ProductRepository {
            override fun findById(id: ProductId) = products[id.toString()]

            override fun findByIds(ids: Collection<ProductId>) = ids.mapNotNull { products[it.toString()] }.associateBy { it.id }

            override fun findByPantry(
                pantryId: PantryId,
                filter: ProductFilter,
            ) = products.values.filter { it.pantryId == pantryId }
                .filter { filter.includeArchived || !it.archived }
                .filter { filter.category == null || it.category == filter.category }
                .filter { filter.kind == null || it.kind == filter.kind }
                .filter { term -> filter.searchTerm?.let { term.name.normalized.contains(ProductName.normalize(it)) } ?: true }

            override fun findByBarcode(
                pantryId: PantryId,
                barcode: Barcode,
            ) = products.values.firstOrNull { it.pantryId == pantryId && it.barcode == barcode }

            override fun findByNormalizedName(
                pantryId: PantryId,
                normalizedName: String,
            ) = products.values.firstOrNull { it.pantryId == pantryId && it.name.normalized == normalizedName }

            override fun save(product: Product) = product.also { products[it.id.toString()] = it }

            override fun deleteById(id: ProductId) {
                products.remove(id.toString())
            }
        }

    val batchRepository: BatchRepository =
        object : BatchRepository {
            override fun findById(id: BatchId) = batches[id.toString()]

            override fun findByProduct(productId: ProductId) =
                batches.values.filter { it.productId == productId }.sortedWith(compareBy(nullsLast()) { it.expiresOn })

            override fun findInStockByPantry(pantryId: PantryId) =
                batches.values.filter { it.pantryId == pantryId && it.quantity > 0 }.sortedWith(compareBy(nullsLast()) { it.expiresOn })

            override fun save(batch: Batch) = batch.also { batches[it.id.toString()] = it }

            override fun saveAll(batches: Collection<Batch>) = batches.map { save(it) }

            override fun deleteById(id: BatchId) {
                batches.remove(id.toString())
            }
        }

    val movementRepository: StockMovementRepository =
        object : StockMovementRepository {
            override fun save(movement: StockMovement) = movement.also { movements += it }

            override fun findByPantry(
                pantryId: PantryId,
                productId: ProductId?,
                page: Int,
                pageSize: Int,
            ): Pair<List<StockMovement>, Long> {
                val all =
                    movements.filter { it.pantryId == pantryId && (productId == null || it.productId == productId) }
                        .sortedWith(compareByDescending<StockMovement> { it.occurredOn }.thenByDescending { movements.indexOf(it) })
                return all.drop(page * pageSize).take(pageSize) to all.size.toLong()
            }

            override fun unitsByYear(
                productId: ProductId,
                type: MovementType,
            ) = movements.filter { it.productId == productId && it.type == type }
                .groupBy { it.occurredOn.year }
                .mapValues { (_, list) -> list.sumOf { abs(it.quantity) } }
        }

    val pantryRepository: PantryRepository =
        mockk(relaxUnitFun = true) {
            every { findById(pantry.id) } returns pantry
            every { findMemberRole(pantry.id, any()) } returns null
            every { findMemberRole(pantry.id, owner.id) } returns PantryRole.OWNER
        }

    val userRepository: UserRepository =
        object : UserRepository {
            override fun findById(id: UserId) = listOf(owner, outsider).firstOrNull { it.id == id }

            override fun findByEmail(email: Email) = listOf(owner, outsider).firstOrNull { it.email == email }

            override fun existsByEmail(email: Email) = findByEmail(email) != null

            override fun save(user: User) = user

            override fun deleteById(id: UserId) = Unit

            override fun findAll(
                searchTerm: String?,
                page: Int,
                pageSize: Int,
            ) = listOf(owner, outsider) to 2L
        }

    val guard = PantryAccessGuard(pantryRepository, PantryAccessPolicy(pantryRepository))
    val access = StockAccess(productRepository, batchRepository, userRepository, guard, clock)
}
