package pl.kmazurek.larder.application.usecase.notification

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import pl.kmazurek.larder.application.service.NotificationService
import pl.kmazurek.larder.domain.model.pantry.Pantry
import pl.kmazurek.larder.domain.model.stock.Batch
import pl.kmazurek.larder.domain.model.stock.Freshness
import pl.kmazurek.larder.domain.model.stock.FreshnessStatus
import pl.kmazurek.larder.domain.model.stock.Product
import pl.kmazurek.larder.domain.repository.BatchRepository
import pl.kmazurek.larder.domain.repository.PantryRepository
import pl.kmazurek.larder.domain.repository.ProductFilter
import pl.kmazurek.larder.domain.repository.ProductRepository
import java.time.Clock
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * The weekly message: what is past its date, what runs out within [soonDays], what is below its minimum.
 * Pure text building, so it can be read in a test and in the log.
 */
data class ExpiryDigest(
    val pantryName: String,
    val expired: List<Pair<Product, Batch>>,
    val expiringSoon: List<Pair<Product, Batch>>,
    val belowMinimum: List<Pair<Product, Int>>,
    val soonDays: Int,
) {
    val isEmpty: Boolean get() = expired.isEmpty() && expiringSoon.isEmpty() && belowMinimum.isEmpty()

    val title: String
        get() =
            when {
                expired.isNotEmpty() -> "$pantryName: ${count(expired.size, "partia", "partie", "partii")} po terminie"
                expiringSoon.isNotEmpty() -> "$pantryName: ${count(
                    expiringSoon.size,
                    "partia",
                    "partie",
                    "partii",
                )} kończy się w $soonDays dni"
                else -> "$pantryName: ${count(belowMinimum.size, "produkt", "produkty", "produktów")} do kupienia"
            }

    /** Markdown body (ntfy renders it); every line is one thing to do */
    fun message(): String {
        val sections = mutableListOf<String>()
        if (expired.isNotEmpty()) {
            sections += "**Po terminie**\n" +
                expired.joinToString(
                    "\n",
                ) { (p, b) -> "- ${b.quantity} × ${p.name} (${date(b)})" }
        }
        if (expiringSoon.isNotEmpty()) {
            sections += "**Zużyć w ciągu $soonDays dni**\n" +
                expiringSoon.joinToString(
                    "\n",
                ) { (p, b) -> "- ${b.quantity} × ${p.name} (${date(b)})" }
        }
        if (belowMinimum.isNotEmpty()) {
            sections += "**Do kupienia**\n" +
                belowMinimum.joinToString(
                    "\n",
                ) { (p, missing) -> "- ${p.name}: brakuje $missing" }
        }
        return sections.joinToString("\n\n")
    }

    private fun date(batch: Batch) = batch.bestBefore?.toString() ?: batch.expiresOn?.format(DATE) ?: "?"

    companion object {
        private val DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

        private fun count(
            n: Int,
            one: String,
            few: String,
            many: String,
        ) = "$n " +
            if (n == 1) {
                one
            } else if (n % 10 in 2..4 && n % 100 !in 12..14) {
                few
            } else {
                many
            }

        fun build(
            pantry: Pantry,
            products: List<Product>,
            inStock: List<Batch>,
            today: LocalDate,
            soonDays: Int,
        ): ExpiryDigest {
            val byId = products.associateBy { it.id }
            val dated = inStock.filter { it.productId in byId }.sortedWith(compareBy(nullsLast()) { it.expiresOn })
            val expired = dated.filter { Freshness.status(it.expiresOn, today, soonDays) == FreshnessStatus.EXPIRED }
            val soon = dated.filter { Freshness.status(it.expiresOn, today, soonDays) == FreshnessStatus.EXPIRING_SOON }
            val quantities = inStock.groupBy { it.productId }.mapValues { (_, b) -> b.sumOf { it.quantity } }
            val below =
                products.filter { it.minQuantity > 0 && (quantities[it.id] ?: 0) < it.minQuantity }
                    .map { it to it.minQuantity - (quantities[it.id] ?: 0) }
                    .sortedWith(compareByDescending<Pair<Product, Int>> { it.second }.thenBy { it.first.name.value.lowercase() })
            return ExpiryDigest(
                pantryName = pantry.name,
                expired = expired.map { byId.getValue(it.productId) to it },
                expiringSoon = soon.map { byId.getValue(it.productId) to it },
                belowMinimum = below,
                soonDays = soonDays,
            )
        }
    }
}

/**
 * Use Case: build and push the digest of every pantry (a household has one; nothing is sent for an empty digest).
 * Returns the number of digests sent.
 */
@Service
class SendExpiryDigests(
    private val pantryRepository: PantryRepository,
    private val productRepository: ProductRepository,
    private val batchRepository: BatchRepository,
    private val notificationService: NotificationService,
    private val clock: Clock,
    @Value("\${app.notifications.digest.soon-days:30}") private val soonDays: Int = Freshness.DEFAULT_SOON_DAYS,
) {
    private val logger = LoggerFactory.getLogger(SendExpiryDigests::class.java)

    fun execute(): Int {
        val today = LocalDate.now(clock)
        var sent = 0
        for (pantry in pantryRepository.findAll()) {
            val digest =
                ExpiryDigest.build(
                    pantry = pantry,
                    products = productRepository.findByPantry(pantry.id, ProductFilter()),
                    inStock = batchRepository.findInStockByPantry(pantry.id),
                    today = today,
                    soonDays = soonDays,
                )
            if (digest.isEmpty) {
                logger.info("Digest for '{}': nothing to report", pantry.name)
                continue
            }
            val priority = if (digest.expired.isNotEmpty()) 4 else 3
            if (notificationService.notify(digest.title, digest.message(), priority, listOf("jar"))) sent++
        }
        return sent
    }
}

/**
 * Runs the digest on a schedule (app.notifications.digest.cron, household time), when enabled.
 */
@Component
class ExpiryDigestScheduler(
    private val sendExpiryDigests: SendExpiryDigests,
    @Value("\${app.notifications.digest.enabled:true}") private val enabled: Boolean,
) {
    private val logger = LoggerFactory.getLogger(ExpiryDigestScheduler::class.java)

    @Scheduled(cron = "\${app.notifications.digest.cron:0 0 18 * * SUN}", zone = "Europe/Warsaw")
    fun run() {
        if (!enabled) return
        try {
            logger.info("Expiry digest: sent {} notification(s)", sendExpiryDigests.execute())
        } catch (ex: Exception) {
            logger.error("Expiry digest failed", ex)
        }
    }
}
