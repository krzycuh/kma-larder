package pl.kmazurek.larder.application.usecase.notification

import io.mockk.every
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import pl.kmazurek.larder.application.dto.ApproximateDateDto
import pl.kmazurek.larder.application.dto.BatchRequest
import pl.kmazurek.larder.application.dto.ProductRequest
import pl.kmazurek.larder.application.service.NotificationService
import pl.kmazurek.larder.application.usecase.stock.AddBatch
import pl.kmazurek.larder.application.usecase.stock.CreateProduct
import pl.kmazurek.larder.application.usecase.stock.StockTestSupport
import pl.kmazurek.larder.domain.model.pantry.ProductId
import pl.kmazurek.larder.domain.model.stock.ProductCategory
import pl.kmazurek.larder.domain.model.stock.ProductKind

class ExpiryDigestTest {
    private val s = StockTestSupport()
    private val sent = mutableListOf<Triple<String, String, Int>>()
    private val notifications =
        object : NotificationService {
            override fun notifyUserRegistered(
                userName: String,
                userEmail: String,
            ) = Unit

            override fun notify(
                title: String,
                message: String,
                priority: Int,
                tags: List<String>,
            ): Boolean {
                sent += Triple(title, message, priority)
                return true
            }
        }
    private val useCase =
        SendExpiryDigests(s.pantryRepository, s.productRepository, s.batchRepository, notifications, s.clock, soonDays = 30)

    private fun product(
        name: String,
        minQuantity: Int = 0,
    ) = ProductId.fromString(
        CreateProduct(s.productRepository, s.guard, s.access)
            .execute(
                s.owner.id,
                s.pantry.id,
                ProductRequest(name, ProductKind.STORE_BOUGHT, ProductCategory.OTHER, minQuantity = minQuantity),
            )
            .id,
    )

    private fun batch(
        product: ProductId,
        quantity: Int,
        year: Int? = null,
        month: Int? = null,
        day: Int? = null,
    ) = AddBatch(s.batchRepository, s.movementRepository, s.access)
        .execute(s.owner.id, product, BatchRequest(quantity, bestBefore = year?.let { ApproximateDateDto(it, month, day) }))

    @Test
    fun `digest lists expired, soon and missing products in a readable message`() {
        every { s.pantryRepository.findAll() } returns listOf(s.pantry)
        val beans = product("Fasola", minQuantity = 3)
        val water = product("Woda", minQuantity = 6)
        val honey = product("Miód")
        batch(beans, 2, 2026, 10)
        batch(beans, 1, 2026, 12, 5)
        batch(honey, 3, 2028)
        batch(water, 6)

        assertEquals(1, useCase.execute())
        val (title, message, priority) = sent.single()
        assertEquals("Spiżarnia Anny: 1 partia po terminie", title)
        assertEquals(4, priority)
        assertEquals(
            """
            **Po terminie**
            - 2 × Fasola (10.2026)

            **Zużyć w ciągu 30 dni**
            - 1 × Fasola (05.12.2026)
            """.trimIndent(),
            message,
        )
    }

    @Test
    fun `nothing is sent when there is nothing to report`() {
        every { s.pantryRepository.findAll() } returns listOf(s.pantry)
        val water = product("Woda", minQuantity = 2)
        batch(water, 6, 2028)

        assertEquals(0, useCase.execute())
        assertTrue(sent.isEmpty())
    }

    @Test
    fun `missing products get their own title when nothing expires`() {
        every { s.pantryRepository.findAll() } returns listOf(s.pantry)
        product("Cukier", minQuantity = 2)
        product("Sól", minQuantity = 1)

        assertEquals(1, useCase.execute())
        assertEquals("Spiżarnia Anny: 2 produkty do kupienia", sent.single().first)
        assertEquals(3, sent.single().third)
        assertEquals("**Do kupienia**\n- Cukier: brakuje 2\n- Sól: brakuje 1", sent.single().second)
    }

    @Test
    fun `plural forms follow Polish rules`() {
        every { s.pantryRepository.findAll() } returns listOf(s.pantry)
        val beans = product("Fasola")
        repeat(5) { batch(beans, 1, 2026, 10, it + 1) }

        useCase.execute()
        assertEquals("Spiżarnia Anny: 5 partii po terminie", sent.single().first)
    }
}
