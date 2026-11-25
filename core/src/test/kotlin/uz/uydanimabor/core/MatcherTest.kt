package uz.uydanimabor.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.uydanimabor.core.Fixtures.STAPLES
import uz.uydanimabor.core.Fixtures.exampleMarks
import uz.uydanimabor.core.Fixtures.exampleRecipes
import uz.uydanimabor.core.Fixtures.ing
import uz.uydanimabor.core.Fixtures.recipe

class MatcherTest {

    /** Texnik topshiriqdagi misol — aynan jadvaldagi tartib va qiymatlar. */
    @Test
    fun specExample_orderAndValues() {
        val result = Matcher.match(exampleRecipes, exampleMarks, STAPLES)
        assertEquals(
            listOf("mastava", "achchiq_chuchuk", "shorva", "palov", "moshxorda", "dimlama"),
            result.map { it.recipe.id },
        )
        // id -> (yetishmaydi, tez buziladigan, foydalanuvchi mahsulotlari, vaqt, moslik)
        val expected = listOf(
            listOf(0, 1, 6, 60, 100),
            listOf(0, 1, 2, 10, 100),
            listOf(0, 0, 4, 75, 100),
            listOf(0, 0, 4, 90, 100),
            listOf(1, 0, 4, 60, 80),
            listOf(1, 0, 4, 90, 80),
        )
        result.zip(expected).forEach { (r, e) ->
            assertEquals(r.recipe.id, e, listOf(r.missingCount, r.urgentUsed, r.userUsed, r.recipe.minutes, r.percent))
        }
        assertEquals(listOf("mosh"), result[4].missing.flatMap { it.productIds })
        assertEquals(listOf("karam"), result[5].missing.flatMap { it.productIds })
    }

    @Test
    fun moreThanTwoMissing_isExcluded() {
        val r = recipe("x", 10, ing("a"), ing("b"), ing("c"), ing("d"))
        assertTrue(Matcher.match(listOf(r), mapOf("a" to MarkState.HAVE), STAPLES).isEmpty())
        val r2 = recipe("y", 10, ing("a"), ing("b"), ing("c"))
        val res = Matcher.match(listOf(r2), mapOf("a" to MarkState.HAVE), STAPLES)
        assertEquals(1, res.size)
        assertEquals(2, res[0].missingCount)
        assertEquals(33, res[0].percent)
    }

    @Test
    fun recipeOfOnlyStaples_isNotShown() {
        val r = recipe("choy", 5, ing("suv"), ing("shakar"))
        assertTrue(Matcher.match(listOf(r), emptyMap(), STAPLES).isEmpty())
        assertTrue(Matcher.match(listOf(r), mapOf("pomidor" to MarkState.HAVE), STAPLES).isEmpty())
    }

    @Test
    fun noUserProducts_nothingShown() {
        assertTrue(Matcher.match(exampleRecipes, emptyMap(), STAPLES).isEmpty())
    }

    @Test
    fun staplesCountAsAvailable_butNotAsUserProducts() {
        val r = recipe("x", 10, ing("tuxum"), ing("osimlik_yogi"), ing("tuz"))
        val res = Matcher.evaluate(r, mapOf("tuxum" to MarkState.HAVE), STAPLES)
        assertEquals(0, res.missingCount)
        assertEquals(1, res.userUsed)
        assertEquals(100, res.percent)
    }

    @Test
    fun disabledStaple_becomesMissing() {
        val r = recipe("x", 10, ing("tuxum"), ing("shakar"))
        val res = Matcher.evaluate(r, mapOf("tuxum" to MarkState.HAVE), STAPLES - "shakar")
        assertEquals(1, res.missingCount)
        assertEquals(50, res.percent)
    }

    @Test
    fun alternativesGroup_satisfiedByAnyAndCountedOnce() {
        val r = recipe("x", 10, ing("mol_goshti", "qoy_goshti"), ing("piyoz"))
        val res = Matcher.evaluate(
            r,
            mapOf("mol_goshti" to MarkState.HAVE, "qoy_goshti" to MarkState.URGENT, "piyoz" to MarkState.HAVE),
            STAPLES,
        )
        assertEquals(0, res.missingCount)
        assertEquals(2, res.userUsed)
        assertEquals(1, res.urgentUsed)
    }

    @Test
    fun optionalIngredients_doNotAffectMatching() {
        val r = recipe("x", 10, ing("piyoz"), optional = listOf(ing("pomidor")))
        val res = Matcher.evaluate(r, mapOf("piyoz" to MarkState.HAVE, "pomidor" to MarkState.URGENT), STAPLES)
        assertEquals(0, res.urgentUsed)
        assertEquals(1, res.userUsed)
    }

    @Test
    fun ties_brokenByTimeThenName() {
        val a = recipe("a", 30, ing("piyoz"), name = "Bodom")
        val b = recipe("b", 30, ing("piyoz"), name = "Anor")
        val c = recipe("c", 20, ing("piyoz"), name = "Zira")
        val res = Matcher.match(listOf(a, b, c), mapOf("piyoz" to MarkState.HAVE), STAPLES)
        assertEquals(listOf("c", "b", "a"), res.map { it.recipe.id })
    }

    @Test
    fun percent_isRounded() {
        val r = recipe("x", 10, ing("a"), ing("b"), ing("c"))
        assertEquals(67, Matcher.evaluate(r, mapOf("a" to MarkState.HAVE, "b" to MarkState.HAVE), STAPLES).percent)
        val empty = recipe("e", 10)
        assertEquals(100, Matcher.evaluate(empty, emptyMap(), STAPLES).percent)
    }

    @Test
    fun markState_cyclesThroughThreeTaps() {
        assertEquals(MarkState.HAVE, MarkState.NONE.next())
        assertEquals(MarkState.URGENT, MarkState.HAVE.next())
        assertEquals(MarkState.NONE, MarkState.URGENT.next())
    }

    @Test
    fun filters_combine() {
        val catalog = Fixtures.realCatalog()
        val all = catalog.recipes
        val quickSalads = RecipeFilter(types = setOf(RecipeType.SALAT), quickOnly = true)
        val res = all.filter { quickSalads.accepts(it, catalog) }
        assertTrue(res.isNotEmpty())
        assertTrue(res.all { it.type == RecipeType.SALAT && it.minutes <= 30 })

        val noDairy = RecipeFilter(excludeAllergens = setOf(Allergen.SUT, Allergen.GLUTEN), meatlessOnly = true)
        val res2 = all.filter { noDairy.accepts(it, catalog) }
        assertTrue(res2.none { Allergen.SUT in catalog.allergensOf(it) || Allergen.GLUTEN in catalog.allergensOf(it) })
        assertTrue(res2.all { it.meatless })
        assertFalse(res2.any { it.id == "manti" })

        val hard = RecipeFilter(difficulties = setOf(Difficulty.MURAKKAB))
        assertTrue(all.filter { hard.accepts(it, catalog) }.all { it.difficulty == Difficulty.MURAKKAB })
        assertTrue(RecipeFilter().isEmpty)
        assertEquals(3, RecipeFilter(types = setOf(RecipeType.SALAT, RecipeType.SHORVA), quickOnly = true).activeCount)
    }
}
