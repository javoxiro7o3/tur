package uz.uydanimabor.core

import java.io.File

object Fixtures {
    val GOSHT = listOf("mol_goshti", "qoy_goshti")
    val STAPLES = setOf("tuz", "qora_murch", "osimlik_yogi", "suv", "shakar", "choy_sodasi")

    fun ing(vararg ids: String, amount: Double = 1.0, unit: String = "dona") = Ingredient(ids.toList(), amount, unit)

    fun recipe(
        id: String,
        minutes: Int,
        vararg required: Ingredient,
        name: String = id,
        type: RecipeType = RecipeType.ASOSIY,
        optional: List<Ingredient> = emptyList(),
        meatless: Boolean = false,
        difficulty: Difficulty = Difficulty.OSON,
    ) = Recipe(
        id = id, name = name, type = type, minutes = minutes, difficulty = difficulty, servings = 4,
        required = required.toList(), optional = optional, steps = listOf(Step("Pishiring", null)),
        warning = null, meatless = meatless, national = true, reviewedBy = null, reviewedDate = null,
    )

    /** Texnik topshiriqdagi misol: oltita taom. */
    val exampleRecipes: List<Recipe> = listOf(
        // ataylab aralash tartibda
        recipe("dimlama", 90, ing(*GOSHT.toTypedArray()), ing("kartoshka"), ing("sabzi"), ing("piyoz"), ing("karam"), name = "Dimlama"),
        recipe("palov", 90, ing("guruch"), ing(*GOSHT.toTypedArray()), ing("sabzi"), ing("piyoz"), name = "Palov",
            optional = listOf(ing("zira"), ing("noxat"))),
        recipe("achchiq_chuchuk", 10, ing("pomidor"), ing("piyoz"), name = "Achchiq-chuchuk salat", type = RecipeType.SALAT),
        recipe("moshxorda", 60, ing("mosh"), ing("guruch"), ing(*GOSHT.toTypedArray()), ing("piyoz"), ing("sabzi"),
            name = "Moshxoʻrda", type = RecipeType.SHORVA),
        recipe("mastava", 60, ing("guruch"), ing(*GOSHT.toTypedArray()), ing("kartoshka"), ing("sabzi"), ing("piyoz"),
            ing("pomidor", "tomat_pastasi"), name = "Mastava", type = RecipeType.SHORVA),
        recipe("shorva", 75, ing(*GOSHT.toTypedArray()), ing("kartoshka"), ing("sabzi"), ing("piyoz"),
            name = "Shoʻrva", type = RecipeType.SHORVA),
    )

    /** Foydalanuvchi: kartoshka, piyoz, sabzi, mol goʻshti, guruch va pomidor (tez ishlatish kerak). */
    val exampleMarks: Map<String, MarkState> = mapOf(
        "kartoshka" to MarkState.HAVE,
        "piyoz" to MarkState.HAVE,
        "sabzi" to MarkState.HAVE,
        "mol_goshti" to MarkState.HAVE,
        "guruch" to MarkState.HAVE,
        "pomidor" to MarkState.URGENT,
    )

    /** Ilova ichidagi haqiqiy bazani oʻqiydi. */
    fun realCatalog(): Catalog {
        val dir = File(System.getProperty("assetsDir") ?: "../app/src/main/assets")
        return Catalog.parse(
            File(dir, "mahsulotlar.json").readText(),
            File(dir, "retseptlar.json").readText(),
        )
    }
}
