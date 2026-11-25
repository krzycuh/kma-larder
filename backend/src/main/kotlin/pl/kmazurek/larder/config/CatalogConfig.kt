package pl.kmazurek.larder.config

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.CacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.client.RestClient
import pl.kmazurek.larder.application.service.ProductMetadataProvider
import pl.kmazurek.larder.infrastructure.catalog.CachingProductMetadataProvider
import pl.kmazurek.larder.infrastructure.catalog.NoOpProductMetadataProvider
import pl.kmazurek.larder.infrastructure.catalog.OpenFoodFactsProductMetadataProvider
import java.net.ProxySelector
import java.net.http.HttpClient
import java.time.Duration

/**
 * External product database for barcode lookup: app.catalog.provider = openfoodfacts (default) or none.
 */
@Configuration
class CatalogConfig(
    @Value("\${app.catalog.provider:openfoodfacts}") private val provider: String,
    @Value("\${app.catalog.timeout:3s}") private val timeout: Duration,
    @Value("\${app.catalog.openfoodfacts.base-url:https://world.openfoodfacts.org}") private val openFoodFactsBaseUrl: String,
) {
    private val logger = LoggerFactory.getLogger(CatalogConfig::class.java)

    @Bean
    fun productMetadataProvider(
        restClientBuilder: RestClient.Builder,
        cacheManager: CacheManager,
    ): ProductMetadataProvider {
        val adapter =
            when (val name = provider.trim().lowercase()) {
                "", "none" -> {
                    logger.info("Product metadata provider: none (barcode lookup disabled)")
                    return NoOpProductMetadataProvider()
                }
                "openfoodfacts" -> OpenFoodFactsProductMetadataProvider(restClient(restClientBuilder, openFoodFactsBaseUrl))
                else -> throw IllegalStateException("Unknown app.catalog.provider '$name' (expected openfoodfacts or none)")
            }
        logger.info("Product metadata provider: {} (timeout {})", provider, timeout)
        return CachingProductMetadataProvider(adapter, cacheManager)
    }

    private fun restClient(
        builder: RestClient.Builder,
        baseUrl: String,
    ): RestClient {
        val httpClient =
            HttpClient.newBuilder()
                .connectTimeout(timeout)
                .proxy(ProxySelector.getDefault())
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build()
        val requestFactory = JdkClientHttpRequestFactory(httpClient).apply { setReadTimeout(timeout) }
        return builder.clone()
            .baseUrl(baseUrl)
            .requestFactory(requestFactory)
            // Open Food Facts asks every client for an identifying User-Agent
            .defaultHeader("User-Agent", "KMA-Larder/1.0 (https://github.com/krzycuh/kma-larder)")
            .build()
    }
}
