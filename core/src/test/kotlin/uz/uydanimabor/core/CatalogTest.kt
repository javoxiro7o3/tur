package uz.uydanimabor.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Ilova ichidagi haqiqiy bazani texnik topshiriq talablariga tekshiradi. */
class CatalogTest {
    private val catalog = Fixtures.realCatalog()

    @Test
    fun products_atLeast60_inAll7Categories() {
        assertTrue(catalog.products.size >= 60)
        assertEquals(Category.entries.toSet(), catalog.products.map { it.category }.toSet())
        assertEquals(catalog.products.size, catalog.products.map { it.id }.toSet().size)
        assertTrue(catalog.products.all { it.name.isNotBlank() })
        assertTrue(catalog.products.all { it.id.matches(Regex("[a-z_]+")) })
    }

    @Test
    fun staples_matchSpec() {
        assertEquals(
            setOf("tuz", "qora_murch", "osimlik_yogi", "suv", "shakar", "choy_sodasi"),
            catalog.stapleIds,
        )
    }

    @Test
    fun recipes_atLeast50_30National_4PerType() {
        assertTrue(catalog.recipes.size >= 50)
        assertTrue(catalog.recipes.count { it.national } >= 30)
        RecipeType.entries.forEach { t ->
            assertTrue("$t", catalog.recipes.count { it.type == t } >= 4)
        }
        assertEquals(catalog.recipes.size, catalog.recipes.map { it.id }.toSet().size)
    }

    @Test
    fun recipes_referenceExistingProducts() {
        catalog.recipes.forEach { r ->
            (r.required + r.optional).flatMap { it.productIds }.forEach { id ->
                assertTrue("${r.id}: $id", catalog.product(id) != null)
            }
            assertTrue(r.id, r.required.isNotEmpty())
            assertTrue(r.id, r.steps.isNotEmpty())
            assertTrue(r.id, r.servings in 1..10)
            assertTrue(r.id, r.minutes > 0)
        }
    }

    @Test
    fun meatAndEggRecipes_haveFullyCookedInstruction() {
        val risky = setOf("mol_goshti", "qoy_goshti", "tovuq", "qiyma", "jigar", "tuxum", "baliq")
        catalog.recipes.forEach { r ->
            val requiredRisky = r.required.any { g -> g.productIds.all { it in risky } }
            if (requiredRisky && r.type != RecipeType.SHIRINLIK && r.type != RecipeType.NONUSHTA.takeIf { false }) {
                val text = r.steps.joinToString(" ") { SearchText.normalize(it.text) }
                assertTrue(
                    "${r.id}: 'toʻliq pishguncha' yoʻq",
                    text.contains("to'liq pish") || text.contains("to'liq qot") || text.contains("ichi to'liq") ||
                        text.contains("to'liq pishgan"),
                )
            }
        }
    }

    @Test
    fun meatlessFlag_isConsistent() {
        val meat = catalog.products.filter { it.category == Category.GOSHT_BALIQ }.map { it.id }.toSet()
        catalog.recipes.forEach { r ->
            val hasMeat = r.required.any { g -> g.productIds.all { it in meat } }
            assertEquals(r.id, !hasMeat, r.meatless)
        }
    }

    @Test
    fun ingredientNames_areShortened() {
        val fruits = catalog.recipeById["mevali_salat"]!!.required.first()
        assertEquals("Olma yoki Banan va boshqalar", catalog.ingredientName(fruits))
        val meat = catalog.recipeById["palov"]!!.required[1]
        assertEquals("Mol goʻshti yoki Qoʻy goʻshti", catalog.ingredientName(meat))
    }

    @Test
    fun typographyApplied() {
        assertEquals("Moshxoʻrda", catalog.recipeById["moshxorda"]!!.name)
        assertEquals("Lagʻmon", catalog.recipeById["lagmon"]!!.name)
        assertEquals("Oʻsimlik yogʻi", catalog.product("osimlik_yogi")!!.name)
    }

    @Test
    fun allergens_computedFromProducts() {
        val manti = catalog.recipeById["manti"]!!
        assertTrue(Allergen.GLUTEN in catalog.allergensOf(manti))
        val achchiq = catalog.recipeById["achchiq_chuchuk"]!!
        assertTrue(catalog.allergensOf(achchiq).isEmpty())
        val quymoq = catalog.recipeById["quymoq"]!!
        assertEquals(setOf(Allergen.SUT, Allergen.TUXUM, Allergen.GLUTEN), catalog.allergensOf(quymoq))
        val kichiri = catalog.recipeById["mosh_kichiri"]!!
        assertTrue(catalog.allergensOf(kichiri).isEmpty())
        assertEquals(setOf(Allergen.SUT), catalog.optionalAllergensOf(kichiri))
    }

    /** Texnik topshiriqdagi misol haqiqiy bazada ham aynan shu tartibda chiqishi kerak. */
    @Test
    fun specExample_onRealDatabase() {
        val result = Matcher.match(catalog.recipes, Fixtures.exampleMarks, catalog.stapleIds)
        val six = listOf("mastava", "achchiq_chuchuk", "shorva", "palov", "moshxorda", "dimlama")
        val order = result.map { it.recipe.id }.filter { it in six }
        assertEquals(six, order)
        val byId = result.associateBy { it.recipe.id }
        val expected = mapOf(
            "mastava" to listOf(0, 1, 6, 60, 100),
            "achchiq_chuchuk" to listOf(0, 1, 2, 10, 100),
            "shorva" to listOf(0, 0, 4, 75, 100),
            "palov" to listOf(0, 0, 4, 90, 100),
            "moshxorda" to listOf(1, 0, 4, 60, 80),
            "dimlama" to listOf(1, 0, 4, 90, 80),
        )
        expected.forEach { (id, e) ->
            val r = byId.getValue(id)
            assertEquals(id, e, listOf(r.missingCount, r.urgentUsed, r.userUsed, r.recipe.minutes, r.percent))
        }
    }
}
