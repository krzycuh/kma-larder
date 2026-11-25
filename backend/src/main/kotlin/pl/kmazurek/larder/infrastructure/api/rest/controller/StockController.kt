package pl.kmazurek.larder.infrastructure.api.rest.controller

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import pl.kmazurek.larder.application.dto.BarcodeLookupDto
import pl.kmazurek.larder.application.dto.BatchDto
import pl.kmazurek.larder.application.dto.BatchRequest
import pl.kmazurek.larder.application.dto.BatchWithProductDto
import pl.kmazurek.larder.application.dto.DashboardDto
import pl.kmazurek.larder.application.dto.PagedResponse
import pl.kmazurek.larder.application.dto.ProductDetailDto
import pl.kmazurek.larder.application.dto.ProductDto
import pl.kmazurek.larder.application.dto.ProductRequest
import pl.kmazurek.larder.application.dto.QuantityChangeRequest
import pl.kmazurek.larder.application.dto.ReplenishmentItemDto
import pl.kmazurek.larder.application.dto.StockMovementDto
import pl.kmazurek.larder.application.dto.UpdateBatchRequest
import pl.kmazurek.larder.application.usecase.stock.AddBatch
import pl.kmazurek.larder.application.usecase.stock.AdjustBatch
import pl.kmazurek.larder.application.usecase.stock.ConsumeBatch
import pl.kmazurek.larder.application.usecase.stock.CreateProduct
import pl.kmazurek.larder.application.usecase.stock.DeleteProduct
import pl.kmazurek.larder.application.usecase.stock.DiscardBatch
import pl.kmazurek.larder.application.usecase.stock.ExpiringQuery
import pl.kmazurek.larder.application.usecase.stock.GetDashboard
import pl.kmazurek.larder.application.usecase.stock.GetProduct
import pl.kmazurek.larder.application.usecase.stock.ListExpiring
import pl.kmazurek.larder.application.usecase.stock.ListMovements
import pl.kmazurek.larder.application.usecase.stock.ListProducts
import pl.kmazurek.larder.application.usecase.stock.ListProductsQuery
import pl.kmazurek.larder.application.usecase.stock.ListReplenishment
import pl.kmazurek.larder.application.usecase.stock.LookupBarcode
import pl.kmazurek.larder.application.usecase.stock.ProductSort
import pl.kmazurek.larder.application.usecase.stock.RemoveBatch
import pl.kmazurek.larder.application.usecase.stock.SearchProducts
import pl.kmazurek.larder.application.usecase.stock.UpdateBatch
import pl.kmazurek.larder.application.usecase.stock.UpdateProduct
import pl.kmazurek.larder.domain.model.pantry.BatchId
import pl.kmazurek.larder.domain.model.pantry.PantryId
import pl.kmazurek.larder.domain.model.pantry.ProductId
import pl.kmazurek.larder.domain.model.stock.Freshness
import pl.kmazurek.larder.domain.model.stock.ProductCategory
import pl.kmazurek.larder.domain.model.stock.ProductKind
import pl.kmazurek.larder.infrastructure.logging.AuditLogger

/**
 * REST Controller for a pantry's stock: product list, lists of what to use up and what to buy, history, dashboard
 */
@RestController
@RequestMapping("/api/pantries/{pantryId}")
class PantryStockController(
    private val listProducts: ListProducts,
    private val searchProducts: SearchProducts,
    private val createProduct: CreateProduct,
    private val listExpiring: ListExpiring,
    private val listReplenishment: ListReplenishment,
    private val listMovements: ListMovements,
    private val getDashboard: GetDashboard,
    private val lookupBarcode: LookupBarcode,
    private val auditLogger: AuditLogger,
) {
    @GetMapping("/products")
    fun products(
        authentication: Authentication,
        @PathVariable pantryId: String,
        @RequestParam(required = false) search: String?,
        @RequestParam(required = false) category: ProductCategory?,
        @RequestParam(required = false) kind: ProductKind?,
        @RequestParam(required = false, defaultValue = "false") includeArchived: Boolean,
        @RequestParam(required = false, defaultValue = "NAME") sort: ProductSort,
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "50") pageSize: Int,
    ): ResponseEntity<PagedResponse<ProductDto>> =
        ResponseEntity.ok(
            listProducts.execute(
                ListProductsQuery(
                    userId = authentication.userId(),
                    pantryId = PantryId.fromString(pantryId),
                    searchTerm = search,
                    category = category,
                    kind = kind,
                    includeArchived = includeArchived,
                    sort = sort,
                    page = page,
                    pageSize = pageSize,
                ),
            ),
        )

    @GetMapping("/products/search")
    fun search(
        authentication: Authentication,
        @PathVariable pantryId: String,
        @RequestParam q: String,
        @RequestParam(required = false, defaultValue = "10") limit: Int,
    ): ResponseEntity<List<ProductDto>> =
        ResponseEntity.ok(searchProducts.execute(authentication.userId(), PantryId.fromString(pantryId), q, limit))

    /**
     * 200 with the pantry's product and/or external data; 400 invalid code; 404 nobody knows it; 503 database down.
     */
    @GetMapping("/products/lookup")
    fun lookup(
        authentication: Authentication,
        @PathVariable pantryId: String,
        @RequestParam barcode: String,
    ): ResponseEntity<BarcodeLookupDto> =
        ResponseEntity.ok(lookupBarcode.execute(authentication.userId(), PantryId.fromString(pantryId), barcode))

    @PostMapping("/products")
    fun create(
        authentication: Authentication,
        @PathVariable pantryId: String,
        @Valid @RequestBody request: ProductRequest,
    ): ResponseEntity<ProductDto> {
        val dto = createProduct.execute(authentication.userId(), PantryId.fromString(pantryId), request)
        auditLogger.log("PRODUCT_CREATED", details = mapOf("productId" to dto.id, "pantryId" to pantryId))
        return ResponseEntity.status(HttpStatus.CREATED).body(dto)
    }

    @GetMapping("/expiring")
    fun expiring(
        authentication: Authentication,
        @PathVariable pantryId: String,
        @RequestParam(required = false) withinDays: Int?,
        @RequestParam(required = false, defaultValue = "false") expiredOnly: Boolean,
        @RequestParam(required = false, defaultValue = "false") undatedOnly: Boolean,
        @RequestParam(required = false, defaultValue = "${Freshness.DEFAULT_SOON_DAYS}") soonDays: Int,
    ): ResponseEntity<List<BatchWithProductDto>> =
        ResponseEntity.ok(
            listExpiring.execute(
                ExpiringQuery(
                    userId = authentication.userId(),
                    pantryId = PantryId.fromString(pantryId),
                    withinDays = withinDays,
                    expiredOnly = expiredOnly,
                    undatedOnly = undatedOnly,
                    soonDays = soonDays.coerceIn(1, 3650),
                ),
            ),
        )

    @GetMapping("/replenishment")
    fun replenishment(
        authentication: Authentication,
        @PathVariable pantryId: String,
    ): ResponseEntity<List<ReplenishmentItemDto>> =
        ResponseEntity.ok(listReplenishment.execute(authentication.userId(), PantryId.fromString(pantryId)))

    @GetMapping("/movements")
    fun movements(
        authentication: Authentication,
        @PathVariable pantryId: String,
        @RequestParam(required = false) productId: String?,
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "20") pageSize: Int,
    ): ResponseEntity<PagedResponse<StockMovementDto>> =
        ResponseEntity.ok(
            listMovements.execute(
                authentication.userId(),
                PantryId.fromString(pantryId),
                productId?.let { ProductId.fromString(it) },
                page,
                pageSize,
            ),
        )

    @GetMapping("/dashboard")
    fun dashboard(
        authentication: Authentication,
        @PathVariable pantryId: String,
        @RequestParam(required = false, defaultValue = "${Freshness.DEFAULT_SOON_DAYS}") soonDays: Int,
    ): ResponseEntity<DashboardDto> =
        ResponseEntity.ok(getDashboard.execute(authentication.userId(), PantryId.fromString(pantryId), soonDays.coerceIn(1, 3650)))
}

/**
 * REST Controller for single products and their batches
 */
@RestController
@RequestMapping("/api/products")
class ProductController(
    private val getProduct: GetProduct,
    private val updateProduct: UpdateProduct,
    private val deleteProduct: DeleteProduct,
    private val addBatch: AddBatch,
    private val auditLogger: AuditLogger,
) {
    @GetMapping("/{id}")
    fun get(
        authentication: Authentication,
        @PathVariable id: String,
    ): ResponseEntity<ProductDetailDto> = ResponseEntity.ok(getProduct.execute(authentication.userId(), ProductId.fromString(id)))

    @PutMapping("/{id}")
    fun update(
        authentication: Authentication,
        @PathVariable id: String,
        @Valid @RequestBody request: ProductRequest,
    ): ResponseEntity<ProductDto> {
        val dto = updateProduct.execute(authentication.userId(), ProductId.fromString(id), request)
        auditLogger.log("PRODUCT_UPDATED", details = mapOf("productId" to id))
        return ResponseEntity.ok(dto)
    }

    @DeleteMapping("/{id}")
    fun delete(
        authentication: Authentication,
        @PathVariable id: String,
    ): ResponseEntity<Void> {
        deleteProduct.execute(authentication.userId(), ProductId.fromString(id))
        auditLogger.log("PRODUCT_DELETED", details = mapOf("productId" to id))
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{id}/batches")
    fun addBatch(
        authentication: Authentication,
        @PathVariable id: String,
        @Valid @RequestBody request: BatchRequest,
    ): ResponseEntity<BatchDto> {
        val dto = addBatch.execute(authentication.userId(), ProductId.fromString(id), request)
        auditLogger.log("BATCH_ADDED", details = mapOf("batchId" to dto.id, "productId" to id, "quantity" to request.quantity))
        return ResponseEntity.status(HttpStatus.CREATED).body(dto)
    }
}

/**
 * REST Controller for single batches: dates, consumption, discarding, stock-taking
 */
@RestController
@RequestMapping("/api/batches")
class BatchController(
    private val updateBatch: UpdateBatch,
    private val consumeBatch: ConsumeBatch,
    private val discardBatch: DiscardBatch,
    private val adjustBatch: AdjustBatch,
    private val removeBatch: RemoveBatch,
    private val auditLogger: AuditLogger,
) {
    @PutMapping("/{id}")
    fun update(
        authentication: Authentication,
        @PathVariable id: String,
        @Valid @RequestBody request: UpdateBatchRequest,
    ): ResponseEntity<BatchDto> {
        val dto = updateBatch.execute(authentication.userId(), BatchId.fromString(id), request)
        auditLogger.log("BATCH_UPDATED", details = mapOf("batchId" to id))
        return ResponseEntity.ok(dto)
    }

    @PostMapping("/{id}/consume")
    fun consume(
        authentication: Authentication,
        @PathVariable id: String,
        @Valid @RequestBody request: QuantityChangeRequest,
    ): ResponseEntity<BatchDto> {
        val dto = consumeBatch.execute(authentication.userId(), BatchId.fromString(id), request)
        auditLogger.log("BATCH_CONSUMED", details = mapOf("batchId" to id, "quantity" to request.quantity))
        return ResponseEntity.ok(dto)
    }

    @PostMapping("/{id}/discard")
    fun discard(
        authentication: Authentication,
        @PathVariable id: String,
        @Valid @RequestBody request: QuantityChangeRequest,
    ): ResponseEntity<BatchDto> {
        val dto = discardBatch.execute(authentication.userId(), BatchId.fromString(id), request)
        auditLogger.log("BATCH_DISCARDED", details = mapOf("batchId" to id, "quantity" to request.quantity))
        return ResponseEntity.ok(dto)
    }

    @PostMapping("/{id}/adjust")
    fun adjust(
        authentication: Authentication,
        @PathVariable id: String,
        @Valid @RequestBody request: QuantityChangeRequest,
    ): ResponseEntity<BatchDto> {
        val dto = adjustBatch.execute(authentication.userId(), BatchId.fromString(id), request)
        auditLogger.log("BATCH_ADJUSTED", details = mapOf("batchId" to id, "quantity" to request.quantity))
        return ResponseEntity.ok(dto)
    }

    @DeleteMapping("/{id}")
    fun delete(
        authentication: Authentication,
        @PathVariable id: String,
    ): ResponseEntity<Void> {
        removeBatch.execute(authentication.userId(), BatchId.fromString(id))
        auditLogger.log("BATCH_REMOVED", details = mapOf("batchId" to id))
        return ResponseEntity.noContent().build()
    }
}
