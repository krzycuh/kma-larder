package pl.kmazurek.larder.infrastructure.catalog

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.client.RestClient
import pl.kmazurek.larder.application.service.ProductMetadataUnavailableException
import pl.kmazurek.larder.domain.model.stock.Barcode
import java.net.http.HttpClient
import java.time.Duration

class OpenFoodFactsProductMetadataProviderTest {
    private lateinit var server: MockWebServer
    private lateinit var provider: OpenFoodFactsProductMetadataProvider

    @BeforeEach
    fun setup() {
        server = MockWebServer()
        server.start()
        val requestFactory =
            JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build())
                .apply { setReadTimeout(Duration.ofMillis(500)) }
        provider =
            OpenFoodFactsProductMetadataProvider(
                RestClient.builder().baseUrl(server.url("/").toString().trimEnd('/')).requestFactory(requestFactory).build(),
            )
    }

    @AfterEach
    fun tearDown() = server.shutdown()

    private fun fixture(name: String) = javaClass.getResource("/catalog/openfoodfacts/$name.json")!!.readText()

    private fun json(body: String) = MockResponse().setHeader("Content-Type", "application/json").setBody(body)

    @Test
    fun `maps a known product, preferring the Polish name and the first brand`() {
        server.enqueue(json(fixture("product-5900020022745")))

        val metadata = provider.lookupByBarcode(Barcode("5900020022745"))!!

        assertEquals("Pomidory krojone bez skórki w soku pomidorowym", metadata.name)
        assertEquals("Pudliszki", metadata.brand)
        assertEquals("400 g", metadata.packageSize)
        assertEquals("https://images.openfoodfacts.org/images/products/590/002/002/2745/front_pl.4.200.jpg", metadata.imageUrl)
        assertEquals("OPEN_FOOD_FACTS", metadata.source)
        val request = server.takeRequest()
        assertTrue(request.path!!.startsWith("/api/v2/product/5900020022745?fields="))
    }

    @Test
    fun `falls back to the generic name and leaves empty fields null`() {
        server.enqueue(json(fixture("product-no-polish-name")))

        val metadata = provider.lookupByBarcode(Barcode("0036000291452"))!!

        assertEquals("Kidney Beans", metadata.name)
        assertNull(metadata.brand)
        assertNull(metadata.packageSize)
        assertNull(metadata.imageUrl)
    }

    @Test
    fun `unknown codes answer null, whether as 404 or as status 0`() {
        server.enqueue(MockResponse().setResponseCode(404).setHeader("Content-Type", "application/json").setBody(fixture("not-found")))
        assertNull(provider.lookupByBarcode(Barcode("5901234123457")))

        server.enqueue(json(fixture("not-found")))
        assertNull(provider.lookupByBarcode(Barcode("5901234123457")))
    }

    @Test
    fun `retries once on a server error, then gives up`() {
        server.enqueue(MockResponse().setResponseCode(503))
        server.enqueue(json(fixture("product-5900020022745")))
        assertEquals("Pudliszki", provider.lookupByBarcode(Barcode("5900020022745"))?.brand)

        server.enqueue(MockResponse().setResponseCode(503))
        server.enqueue(MockResponse().setResponseCode(503))
        assertThrows<ProductMetadataUnavailableException> { provider.lookupByBarcode(Barcode("5900020022745")) }
    }

    @Test
    fun `a timeout is unavailable, not unknown`() {
        server.enqueue(json(fixture("product-5900020022745")).setHeadersDelay(2, java.util.concurrent.TimeUnit.SECONDS))
        assertThrows<ProductMetadataUnavailableException> { provider.lookupByBarcode(Barcode("5900020022745")) }
    }
}
