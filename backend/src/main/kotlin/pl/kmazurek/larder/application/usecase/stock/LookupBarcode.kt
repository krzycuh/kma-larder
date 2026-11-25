package pl.kmazurek.larder.application.usecase.stock

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import pl.kmazurek.larder.application.dto.BarcodeLookupDto
import pl.kmazurek.larder.application.dto.ProductMetadataDto
import pl.kmazurek.larder.application.service.ProductMetadataProvider
import pl.kmazurek.larder.application.service.ProductMetadataUnavailableException
import pl.kmazurek.larder.application.usecase.pantry.PantryAccessGuard
import pl.kmazurek.larder.application.usecase.pantry.ProductNotFoundException
import pl.kmazurek.larder.domain.model.pantry.PantryId
import pl.kmazurek.larder.domain.model.stock.Barcode
import pl.kmazurek.larder.domain.model.user.UserId
import pl.kmazurek.larder.domain.repository.ProductRepository

/**
 * Use Case: a scanned barcode – first the pantry's own product with that code, then the external database.
 * The external answer is only a suggestion for a new product; nothing is saved here.
 */
@Service
class LookupBarcode(
    private val productRepository: ProductRepository,
    private val metadataProvider: ProductMetadataProvider,
    private val pantryAccessGuard: PantryAccessGuard,
    private val access: StockAccess,
) {
    private val logger = LoggerFactory.getLogger(LookupBarcode::class.java)

    fun execute(
        userId: UserId,
        pantryId: PantryId,
        rawBarcode: String,
    ): BarcodeLookupDto {
        pantryAccessGuard.requireAccess(userId, pantryId)
        val barcode = Barcode.parse(rawBarcode) ?: throw IllegalArgumentException("Invalid barcode: $rawBarcode")
        val existing = productRepository.findByBarcode(pantryId, barcode)?.let { access.productDto(it) }
        val metadata =
            try {
                metadataProvider.lookupByBarcode(barcode)
            } catch (ex: ProductMetadataUnavailableException) {
                // The pantry's own product is still worth answering with; only a complete miss is a 503
                if (existing != null) {
                    logger.warn("Barcode {}: external database unavailable, answering with the pantry's product", barcode)
                    null
                } else {
                    throw ex
                }
            }
        if (existing == null && metadata == null) throw ProductNotFoundException("No product known for barcode $barcode")
        return BarcodeLookupDto(barcode.value, existing, metadata?.let { ProductMetadataDto.fromMetadata(it) })
    }
}
