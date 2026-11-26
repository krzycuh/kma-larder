package pl.kmazurek.larder.infrastructure.catalog

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import org.slf4j.LoggerFactory
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import pl.kmazurek.larder.application.service.ProductMetadata
import pl.kmazurek.larder.application.service.ProductMetadataProvider
import pl.kmazurek.larder.application.service.ProductMetadataUnavailableException
import pl.kmazurek.larder.domain.model.stock.Barcode

/**
 * Open Food Facts adapter: GET /api/v2/product/{barcode}?fields=...
 * Known code: {"status":1,"product":{...}}; unknown code: HTTP 404 (older servers answer 200 with "status":0).
 * The Polish name is preferred when the database has one; the category tags are mapped to a pantry category
 * by [OpenFoodFactsCategoryMapper].
 */
class OpenFoodFactsProductMetadataProvider(
    private val restClient: RestClient,
) : ProductMetadataProvider {
    private val logger = LoggerFactory.getLogger(OpenFoodFactsProductMetadataProvider::class.java)

    override fun lookupByBarcode(barcode: Barcode): ProductMetadata? {
        val response =
            try {
                retryOnceOnServerError {
                    restClient.get()
                        .uri("/api/v2/product/{code}?fields={fields}", barcode.value, FIELDS)
                        .retrieve()
                        .body(OpenFoodFactsResponse::class.java)
                }
            } catch (ex: HttpClientErrorException.NotFound) {
                return null
            } catch (ex: RestClientException) {
                logger.warn("Open Food Facts lookup failed for {}: {}", barcode, ex.message)
                throw ProductMetadataUnavailableException("Open Food Facts is unavailable", ex)
            }
        val product = response?.takeIf { it.status == 1 }?.product ?: return null
        val name =
            listOfNotNull(
                product.productNamePl,
                product.productName,
            ).map { it.trim() }.firstOrNull { it.isNotEmpty() } ?: return null
        return ProductMetadata(
            barcode = barcode.value,
            name = name,
            brand = product.brands?.split(",")?.map { it.trim() }?.firstOrNull { it.isNotEmpty() },
            packageSize = product.quantity?.trim()?.takeIf { it.isNotEmpty() },
            imageUrl = product.imageFrontSmallUrl?.let(::toHttps),
            category = OpenFoodFactsCategoryMapper.categoryFor(product.categoriesTags),
            source = SOURCE,
        )
    }

    companion object {
        const val SOURCE = "OPEN_FOOD_FACTS"
        private const val FIELDS = "code,status,product_name,product_name_pl,brands,quantity,image_front_small_url,categories_tags"
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class OpenFoodFactsResponse(
        val status: Int? = null,
        val product: OpenFoodFactsProduct? = null,
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class OpenFoodFactsProduct(
        @JsonProperty("product_name") val productName: String? = null,
        @JsonProperty("product_name_pl") val productNamePl: String? = null,
        val brands: String? = null,
        val quantity: String? = null,
        @JsonProperty("image_front_small_url") val imageFrontSmallUrl: String? = null,
        @JsonProperty("categories_tags") val categoriesTags: List<String>? = null,
    )
}

/**
 * The app is served over HTTPS; an http:// image link would be mixed content.
 */
internal fun toHttps(url: String): String = if (url.startsWith("http://")) "https://" + url.removePrefix("http://") else url
