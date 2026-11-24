package pl.kmazurek.larder.domain.model.stock

import pl.kmazurek.larder.domain.model.pantry.BatchId
import pl.kmazurek.larder.domain.model.pantry.PantryId
import pl.kmazurek.larder.domain.model.pantry.ProductId
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Batch Aggregate Root (Stock context)
 * A number of identical units of one product with the same date: six jars of this year's cucumbers,
 * three cans best before 05.2028. Quantity goes down as units are consumed or discarded; a batch with
 * zero units stays as history until it is removed.
 *
 * [expiresOn] is the last day of the best-before period, given on the packaging or derived from the
 * production date and the product's shelf life. It is stored, so lists can sort and filter by it.
 */
data class Batch(
    val id: BatchId,
    val pantryId: PantryId,
    val productId: ProductId,
    val quantity: Int,
    val bestBefore: ApproximateDate? = null,
    val producedOn: ApproximateDate? = null,
    val expiresOn: LocalDate? = null,
    val location: String? = null,
    val notes: String? = null,
    val addedOn: LocalDate,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    init {
        require(quantity >= 0) { "Quantity cannot be negative" }
        location?.let { require(it.length <= 100) { "Location cannot exceed 100 characters" } }
        notes?.let { require(it.length <= 2000) { "Notes cannot exceed 2000 characters" } }
    }

    val isEmpty: Boolean get() = quantity == 0

    fun freshness(
        today: LocalDate,
        soonDays: Int = Freshness.DEFAULT_SOON_DAYS,
    ): FreshnessStatus = Freshness.status(expiresOn, today, soonDays)

    /** Takes units off the shelf; fails when there are not enough. */
    fun take(units: Int): Batch {
        require(units > 0) { "Units must be positive" }
        check(units <= quantity) { "Only $quantity unit(s) left, cannot take $units" }
        return copy(quantity = quantity - units, updatedAt = LocalDateTime.now())
    }

    fun add(units: Int): Batch {
        require(units > 0) { "Units must be positive" }
        return copy(quantity = quantity + units, updatedAt = LocalDateTime.now())
    }

    /** Sets the quantity to what is really on the shelf (stock-taking). */
    fun adjustTo(units: Int): Batch {
        require(units >= 0) { "Quantity cannot be negative" }
        return copy(quantity = units, updatedAt = LocalDateTime.now())
    }

    /**
     * Changes the dates and the description; the product decides what the effective best-before date is.
     */
    fun update(
        product: Product,
        bestBefore: ApproximateDate?,
        producedOn: ApproximateDate?,
        location: String?,
        notes: String?,
    ): Batch {
        require(product.id == productId) { "Batch belongs to another product" }
        return copy(
            bestBefore = bestBefore,
            producedOn = producedOn,
            expiresOn = product.bestBeforeFor(bestBefore, producedOn)?.endDate,
            location = location?.trim()?.takeIf { it.isNotEmpty() },
            notes = notes?.trim()?.takeIf { it.isNotEmpty() },
            updatedAt = LocalDateTime.now(),
        )
    }

    /** Recomputes the derived date after the product's shelf life changed. */
    fun refreshExpiry(product: Product): Batch {
        require(product.id == productId) { "Batch belongs to another product" }
        val expires = product.bestBeforeFor(bestBefore, producedOn)?.endDate
        return if (expires == expiresOn) this else copy(expiresOn = expires, updatedAt = LocalDateTime.now())
    }

    companion object {
        fun create(
            product: Product,
            quantity: Int,
            bestBefore: ApproximateDate? = null,
            producedOn: ApproximateDate? = null,
            location: String? = null,
            notes: String? = null,
            addedOn: LocalDate = LocalDate.now(),
        ): Batch {
            require(quantity > 0) { "A new batch needs at least one unit" }
            val now = LocalDateTime.now()
            return Batch(
                id = BatchId.generate(),
                pantryId = product.pantryId,
                productId = product.id,
                quantity = quantity,
                bestBefore = bestBefore,
                producedOn = producedOn,
                expiresOn = product.bestBeforeFor(bestBefore, producedOn)?.endDate,
                location = location?.trim()?.takeIf { it.isNotEmpty() },
                notes = notes?.trim()?.takeIf { it.isNotEmpty() },
                addedOn = addedOn,
                createdAt = now,
                updatedAt = now,
            )
        }
    }
}
