package uz.uydanimabor.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchAndQuantityTest {
    private val catalog = Fixtures.realCatalog()
    private fun search(q: String) = catalog.products.filter { SearchText.matches(q, it) }.map { it.id }

    @Test
    fun apostropheVariants_areEqual() {
        listOf("go'sht", "goʻsht", "go‘sht", "go`sht", "go’sht", "gosht").forEach { q ->
            assertTrue(q, "mol_goshti" in search(q) || "qoy_goshti" in search(q))
        }
        assertTrue("osimlik_yogi" in search("oʻsimlik"))
        assertTrue("osimlik_yogi" in search("o'simlik"))
    }

    @Test
    fun prefixSearch() {
        assertTrue("kartoshka" in search("kar"))
        assertTrue("karam" in search("kar"))
        assertFalse("sabzi" in search("kar"))
        assertEquals(catalog.products.size, search("   ").size)
    }

    @Test
    fun russianNames() {
        assertTrue("kartoshka" in search("kartofel"))
        assertTrue("sabzi" in search("morkov"))
        assertTrue("kartoshka" in search("Картофель"))
        assertTrue("sabzi" in search("морковь"))
        assertTrue("piyoz" in search("luk"))
    }

    @Test
    fun caseAndSpacesIgnored() {
        assertTrue("pomidor" in search("  POMI "))
        assertEquals("a b", SearchText.normalize("  A   B "))
        assertEquals("kartoshka", SearchText.loose("KARTOSHKA"))
    }

    @Test
    fun typography() {
        assertEquals("Oʻzbekiston", UzTypography.apply("O'zbekiston"))
        assertEquals("gʻisht", UzTypography.apply("g'isht"))
        assertEquals("maʼlumot", UzTypography.apply("ma'lumot"))
        assertEquals("oddiy", UzTypography.apply("oddiy"))
    }

    @Test
    fun scaling() {
        assertEquals(1000.0, Quantity.scale(500.0, 4, 8), 1e-9)
        assertEquals("1000 g", Quantity.format(500.0 * 2, "g"))
        assertEquals("125 g", Quantity.format(Quantity.scale(500.0, 4, 1), "g"))
        assertEquals("0,5 dona", Quantity.format(Quantity.scale(2.0, 4, 1), "dona"))
        assertEquals("1,5 dona", Quantity.format(Quantity.scale(2.0, 4, 3), "dona"))
        assertEquals("0,6 l", Quantity.format(Quantity.scale(2.5, 4, 1), "l"))
        assertEquals("5 g", Quantity.format(1.0, "g"))
        val ing = Ingredient(listOf("guruch"), 500.0, "g")
        assertEquals("1250 g", Quantity.format(ing, 4, 10))
    }

    @Test
    fun enumsParse() {
        assertEquals(Difficulty.ORTACHA, Difficulty.of("o'rtacha"))
        assertEquals(Difficulty.ORTACHA, Difficulty.of("oʻrtacha"))
        assertEquals(RecipeType.BOTQA, RecipeType.of("botqa"))
        assertEquals(Category.MEVA, Category.of("meva"))
        assertEquals(Allergen.YONGOQ, Allergen.of("yongoq"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun unknownCategory_fails() {
        Category.of("nomalum")
    }
}
