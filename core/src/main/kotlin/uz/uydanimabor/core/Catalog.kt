package uz.uydanimabor.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private class ProductFileDto(val versiya: String, val mahsulotlar: List<ProductDto>)

@Serializable
private class ProductDto(
    val id: String,
    val nomi: String,
    val sinonimlar: List<String> = emptyList(),
    val toifa: String,
    val asosiy: Boolean = false,
    val allergen: String? = null,
    val belgi: String = "•",
)

@Serializable
private class RecipeFileDto(val versiya: String, val retseptlar: List<RecipeDto>)

@Serializable
private class IngredientDto(val mahsulotlar: List<String>, val miqdor: Double, val birlik: String)

@Serializable
private class StepDto(val matn: String, @SerialName("taymer_daqiqa") val taymerDaqiqa: Int? = null)

@Serializable
private class RecipeDto(
    val id: String,
    val nomi: String,
    val turi: String,
    @SerialName("vaqt_daqiqa") val vaqtDaqiqa: Int,
    val qiyinlik: String,
    val porsiya: Int,
    val kerakli: List<IngredientDto>,
    val ixtiyoriy: List<IngredientDto> = emptyList(),
    val qadamlar: List<StepDto>,
    val ogohlantirish: String? = null,
    val goshtsiz: Boolean = false,
    val milliy: Boolean = false,
    val tekshirgan: String? = null,
    @SerialName("tekshirilgan_sana") val tekshirilganSana: String? = null,
)

/** Ilova ichidagi (oflayn) mahsulotlar va retseptlar bazasi. */
class Catalog(
    val products: List<Product>,
    val recipes: List<Recipe>,
    val version: String,
) {
    val productById: Map<String, Product> = products.associateBy { it.id }
    val recipeById: Map<String, Recipe> = recipes.associateBy { it.id }
    val stapleIds: Set<String> = products.filter { it.isStaple }.map { it.id }.toSet()

    fun product(id: String): Product? = productById[id]

    /**
     * Allergenlar retseptga qoʻlda yozilmaydi: masalliqlardagi mahsulotlardan avtomatik hisoblanadi.
     * Kerakli guruhdagi barcha muqobillarda boʻlgan allergen taomda albatta bor deb hisoblanadi.
     */
    fun allergensOf(recipe: Recipe): Set<Allergen> = recipe.required.flatMap { g ->
        val sets = g.productIds.map { id -> setOfNotNull(productById[id]?.allergen) }
        if (sets.isEmpty()) emptySet() else sets.reduce { a, b -> a intersect b }
    }.toSet()

    /** Ixtiyoriy masalliqlardagi allergenlar (faqat maʼlumot uchun). */
    fun optionalAllergensOf(recipe: Recipe): Set<Allergen> =
        recipe.optional.flatMap { it.productIds }.mapNotNull { productById[it]?.allergen }.toSet() -
            allergensOf(recipe)

    fun ingredientName(ing: Ingredient): String {
        val names = ing.productIds.mapNotNull { productById[it]?.name }
        return if (names.size <= 3) names.joinToString(" yoki ")
        else names.take(2).joinToString(" yoki ") + " va boshqalar"
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun parse(productsJson: String, recipesJson: String): Catalog {
            val pf = json.decodeFromString(ProductFileDto.serializer(), productsJson)
            val rf = json.decodeFromString(RecipeFileDto.serializer(), recipesJson)
            val products = pf.mahsulotlar.map { p ->
                Product(
                    id = p.id,
                    name = UzTypography.apply(p.nomi),
                    synonyms = p.sinonimlar,
                    category = Category.of(p.toifa),
                    isStaple = p.asosiy,
                    allergen = p.allergen?.let { Allergen.of(it) },
                    icon = p.belgi.ifBlank { "" },
                )
            }
            val recipes = rf.retseptlar.map { r ->
                Recipe(
                    id = r.id,
                    name = UzTypography.apply(r.nomi),
                    type = RecipeType.of(r.turi),
                    minutes = r.vaqtDaqiqa,
                    difficulty = Difficulty.of(r.qiyinlik),
                    servings = r.porsiya,
                    required = r.kerakli.map { it.toModel() },
                    optional = r.ixtiyoriy.map { it.toModel() },
                    steps = r.qadamlar.map { Step(UzTypography.apply(it.matn), it.taymerDaqiqa) },
                    warning = r.ogohlantirish?.let { UzTypography.apply(it) },
                    meatless = r.goshtsiz,
                    national = r.milliy,
                    reviewedBy = r.tekshirgan,
                    reviewedDate = r.tekshirilganSana,
                )
            }
            return Catalog(products, recipes, rf.versiya)
        }

        private fun IngredientDto.toModel() = Ingredient(mahsulotlar, miqdor, UzTypography.apply(birlik))
    }
}
