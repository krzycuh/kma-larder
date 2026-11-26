package pl.kmazurek.larder.domain.model.stock

import pl.kmazurek.larder.domain.model.pantry.PantryId
import pl.kmazurek.larder.domain.model.pantry.ProductId
import pl.kmazurek.larder.domain.model.user.UserId
import java.text.Normalizer
import java.time.LocalDateTime

enum class ProductKind {
    /** Bought: has a best-before date on the packaging */
    STORE_BOUGHT,

    /** Home-made: has a production date; the best-before date follows from the product's shelf life */
    HOMEMADE,
}

enum class ProductCategory {
    PRESERVES,
    HONEY,
    FLOUR_GRAINS,
    PASTA_RICE,
    CANNED,
    BEVERAGES,
    OILS_SAUCES,
    SWEETS_SNACKS,
    SPICES_BAKING,
    DRIED,
    FROZEN,
    OTHER,
}

/**
 * Product Aggregate Root (Stock context)
 * What a household keeps in its pantry: "Ogórki kiszone", "Mąka pszenna 1 kg". Belongs to one pantry.
 * The units on the shelves are its batches; the product itself only describes them and says how many
 * there should always be (minQuantity) and, for home-made food, how long it keeps (defaultShelfLifeMonths).
 */
data class Product(
    val id: ProductId,
    val pantryId: PantryId,
    val name: ProductName,
    val kind: ProductKind,
    val category: ProductCategory,
    val barcode: Barcode? = null,
    val packageSize: String? = null,
    val defaultShelfLifeMonths: Int? = null,
    val minQuantity: Int = 0,
    val notes: String? = null,
    val imageUrl: String? = null,
    val archived: Boolean = false,
    val createdByUserId: UserId? = null,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    init {
        require(minQuantity >= 0) { "Minimum quantity cannot be negative" }
        defaultShelfLifeMonths?.let {
            require(
                it in 1..MAX_SHELF_LIFE_MONTHS,
            ) { "Shelf life must be between 1 and $MAX_SHELF_LIFE_MONTHS months" }
        }
        packageSize?.let { require(it.length <= 50) { "Package size cannot exceed 50 characters" } }
        notes?.let { require(it.length <= 2000) { "Notes cannot exceed 2000 characters" } }
        imageUrl?.let { require(isHttpUrl(it)) { "Image URL must be an http(s) link of at most $MAX_IMAGE_URL_LENGTH characters" } }
    }

    fun update(
        name: ProductName,
        kind: ProductKind,
        category: ProductCategory,
        barcode: Barcode?,
        packageSize: String?,
        defaultShelfLifeMonths: Int?,
        minQuantity: Int,
        notes: String?,
        imageUrl: String?,
        archived: Boolean,
    ): Product =
        copy(
            name = name,
            kind = kind,
            category = category,
            barcode = barcode,
            packageSize = packageSize?.trim()?.takeIf { it.isNotEmpty() },
            defaultShelfLifeMonths = defaultShelfLifeMonths,
            minQuantity = minQuantity,
            notes = notes?.trim()?.takeIf { it.isNotEmpty() },
            imageUrl = imageUrl?.trim()?.takeIf { it.isNotEmpty() },
            archived = archived,
            updatedAt = LocalDateTime.now(),
        )

    fun archive(): Product = copy(archived = true, updatedAt = LocalDateTime.now())

    fun unarchive(): Product = copy(archived = false, updatedAt = LocalDateTime.now())

    /**
     * When the batch should be used by: the date on the packaging wins; otherwise the production date
     * plus this product's shelf life. Null when neither is known.
     */
    fun bestBeforeFor(
        bestBefore: ApproximateDate?,
        producedOn: ApproximateDate?,
    ): ApproximateDate? =
        bestBefore
            ?: producedOn?.let { produced -> defaultShelfLifeMonths?.let { produced.plusMonths(it) } }

    fun belongsTo(pantryId: PantryId) = this.pantryId == pantryId

    companion object {
        const val MAX_SHELF_LIFE_MONTHS = 600
        const val MAX_IMAGE_URL_LENGTH = 500

        /** A link a browser can show as an <img>: http(s), no whitespace, bounded length. */
        fun isHttpUrl(url: String): Boolean =
            url.length <= MAX_IMAGE_URL_LENGTH &&
                (url.startsWith("https://") || url.startsWith("http://")) &&
                url.length > 8 &&
                url.none { it.isWhitespace() }

        fun create(
            pantryId: PantryId,
            name: ProductName,
            kind: ProductKind,
            category: ProductCategory,
            barcode: Barcode? = null,
            packageSize: String? = null,
            defaultShelfLifeMonths: Int? = null,
            minQuantity: Int = 0,
            notes: String? = null,
            imageUrl: String? = null,
            createdByUserId: UserId? = null,
        ): Product {
            val now = LocalDateTime.now()
            return Product(
                id = ProductId.generate(),
                pantryId = pantryId,
                name = name,
                kind = kind,
                category = category,
                barcode = barcode,
                packageSize = packageSize?.trim()?.takeIf { it.isNotEmpty() },
                defaultShelfLifeMonths = defaultShelfLifeMonths,
                minQuantity = minQuantity,
                notes = notes?.trim()?.takeIf { it.isNotEmpty() },
                imageUrl = imageUrl?.trim()?.takeIf { it.isNotEmpty() },
                archived = false,
                createdByUserId = createdByUserId,
                createdAt = now,
                updatedAt = now,
            )
        }
    }
}

/**
 * Value Object: product name, trimmed, with a normalized form for duplicate detection
 * ("Ogórki  Kiszone" and "ogorki kiszone" are the same product).
 */
@JvmInline
value class ProductName(val value: String) {
    init {
        require(value.isNotBlank()) { "Product name cannot be blank" }
        require(value.length <= 255) { "Product name cannot exceed 255 characters" }
    }

    val normalized: String
        get() = normalize(value)

    override fun toString() = value

    companion object {
        fun of(raw: String) = ProductName(raw.trim().replace(Regex("\\s+"), " "))

        fun normalize(raw: String): String =
            Normalizer.normalize(raw.trim().lowercase(), Normalizer.Form.NFD)
                .replace("ł", "l")
                .replace(Regex("\\p{M}+"), "")
                .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
                .trim()
    }
}

/**
 * Value Object: a retail barcode (GTIN): EAN-13, EAN-8, or UPC-A stored as EAN-13 with a leading zero.
 * Validates the check digit, so a misread scan is rejected instead of creating a product nobody can find again.
 */
@JvmInline
value class Barcode(val value: String) {
    init {
        require(value.length == 8 || value.length == 13) { "Barcode must have 8 or 13 digits" }
        require(value.all { it.isDigit() }) { "Barcode must contain only digits" }
        require(checksumValid(value)) { "Barcode check digit is invalid" }
    }

    override fun toString() = value

    companion object {
        /** Parses a typed or scanned code; null when it is not a valid GTIN. */
        fun parse(raw: String?): Barcode? {
            val digits = raw.orEmpty().filter { it.isDigit() }
            val code =
                when (digits.length) {
                    8, 13 -> digits
                    12 -> "0$digits"
                    else -> return null
                }
            return if (checksumValid(code)) Barcode(code) else null
        }

        private fun checksumValid(digits: String): Boolean {
            val body = digits.dropLast(1)
            val sum =
                body.reversed().withIndex().sumOf { (index, char) ->
                    char.digitToInt() * if (index % 2 == 0) 3 else 1
                }
            return (10 - sum % 10) % 10 == digits.last().digitToInt()
        }
    }
}
