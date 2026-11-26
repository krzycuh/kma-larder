package pl.kmazurek.larder.infrastructure.catalog

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import pl.kmazurek.larder.domain.model.stock.ProductCategory

class OpenFoodFactsCategoryMapperTest {
    private fun category(vararg tags: String) = OpenFoodFactsCategoryMapper.categoryFor(tags.toList())

    @Test
    fun `maps typical pantry products`() {
        assertEquals(
            ProductCategory.CANNED,
            category("en:plant-based-foods", "en:canned-foods", "en:canned-vegetables", "en:canned-tomatoes"),
        )
        assertEquals(
            ProductCategory.PRESERVES,
            category("en:spreads", "en:sweet-spreads", "en:fruits-based-foods", "en:jams", "en:strawberry-jams"),
        )
        assertEquals(ProductCategory.HONEY, category("en:sweeteners", "en:bee-products", "en:honeys", "en:acacia-honeys"))
        assertEquals(
            ProductCategory.PASTA_RICE,
            category("en:cereals-and-potatoes", "en:cereals-and-their-products", "en:pastas", "en:durum-wheat-pastas"),
        )
        assertEquals(
            ProductCategory.PASTA_RICE,
            category("en:cereals-and-potatoes", "en:cereals-and-their-products", "en:rices", "en:basmati-rices"),
        )
        assertEquals(
            ProductCategory.FLOUR_GRAINS,
            category("en:cereals-and-potatoes", "en:cereals-and-their-products", "en:flours", "en:wheat-flours"),
        )
        assertEquals(ProductCategory.BEVERAGES, category("en:beverages", "en:waters", "en:spring-waters", "en:mineral-waters"))
        assertEquals(ProductCategory.OILS_SAUCES, category("en:fats", "en:vegetable-fats", "en:vegetable-oils", "en:olive-oils"))
        assertEquals(
            ProductCategory.SWEETS_SNACKS,
            category("en:snacks", "en:sweet-snacks", "en:cocoa-and-its-products", "en:chocolates", "en:milk-chocolates"),
        )
        assertEquals(ProductCategory.SPICES_BAKING, category("en:condiments", "en:spices", "en:peppercorns"))
        assertEquals(ProductCategory.SPICES_BAKING, category("en:condiments", "en:salts", "en:table-salts"))
        assertEquals(ProductCategory.DRIED, category("en:plant-based-foods", "en:nuts", "en:almonds"))
        assertEquals(ProductCategory.DRIED, category("en:plant-based-foods", "en:legumes-and-their-products", "en:legumes", "en:lentils"))
        assertEquals(ProductCategory.FROZEN, category("en:frozen-foods", "en:frozen-vegetables", "en:frozen-peas"))
    }

    @Test
    fun `the more specific kind wins when tags overlap`() {
        // canned beans: tinned, not a dried legume
        assertEquals(
            ProductCategory.CANNED,
            category(
                "en:legumes-and-their-products",
                "en:canned-foods",
                "en:legumes",
                "en:canned-legumes",
                "en:beans",
                "en:red-kidney-beans",
            ),
        )
        // pasta sauce: a sauce, not pasta
        assertEquals(ProductCategory.OILS_SAUCES, category("en:groceries", "en:sauces", "en:pasta-sauces", "en:tomato-sauces"))
        // rice drink: a drink, not rice
        assertEquals(ProductCategory.BEVERAGES, category("en:beverages", "en:plant-based-beverages", "en:rice-drinks"))
        // pickles in a jar: preserves, even though also canned vegetables
        assertEquals(ProductCategory.PRESERVES, category("en:vegetables-based-foods", "en:canned-vegetables", "en:pickles", "en:cucumbers"))
        // frozen pizza: frozen before anything else
        assertEquals(ProductCategory.FROZEN, category("en:meals", "en:pizzas-pies-and-quiches", "en:frozen-foods", "en:frozen-pizzas"))
        // cereal bar: a snack, not a grain
        assertEquals(ProductCategory.SWEETS_SNACKS, category("en:snacks", "en:sweet-snacks", "en:bars", "en:cereal-bars"))
    }

    @Test
    fun `local language prefixes and letter case do not matter`() {
        assertEquals(ProductCategory.PRESERVES, category("PL:Ogorki-Kiszone", "en:Pickles"))
        assertEquals(ProductCategory.HONEY, category("fr:miels", "en:honeys"))
    }

    @Test
    fun `unknown, empty or missing tags give no suggestion`() {
        assertNull(category("en:dairies", "en:cheeses", "en:gouda"))
        assertNull(category("en:breads", "en:wheat-breads"))
        assertNull(category())
        assertNull(OpenFoodFactsCategoryMapper.categoryFor(null))
        assertNull(category("", "  "))
    }
}
