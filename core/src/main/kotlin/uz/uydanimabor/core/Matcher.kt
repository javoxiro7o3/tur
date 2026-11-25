package uz.uydanimabor.core

import kotlin.math.roundToInt

/** Bitta taomning moslik natijasi. */
data class MatchResult(
    val recipe: Recipe,
    /** Bajarilmagan kerakli masalliqlar. */
    val missing: List<Ingredient>,
    /** Ishlatilgan "tez ishlatish kerak" mahsulotlar soni. */
    val urgentUsed: Int,
    /** Ishlatilgan foydalanuvchi mahsulotlari soni (asosiy masalliqlarsiz). */
    val userUsed: Int,
    /** Moslik foizi, butun songacha yaxlitlangan. */
    val percent: Int,
) {
    val missingCount: Int get() = missing.size
}

/**
 * "Moslik algoritmi" (texnik topshiriq).
 *
 * 1. Kerakli masalliq guruhidagi kamida bitta mahsulot mavjud toʻplamda boʻlsa, u bajarilgan.
 * 2. Yetishmaydi = bajarilmagan kerakli masalliqlar soni.
 * 3. Taom roʻyxatga kiradi, agar yetishmaydi ≤ 2 va foydalanuvchi belgilagan
 *    mahsulotlardan kamida bittasi ishlatilsa.
 * 4. Saralash: yetishmaydi ↑, tez ishlatish ↓, foydalanuvchi mahsulotlari ↓, vaqt ↑, nomi (alifbo).
 * 5. Moslik = bajarilgan / jami kerakli × 100%.
 */
object Matcher {
    const val MAX_MISSING = 2

    /**
     * @param marks foydalanuvchi belgilagan mahsulotlar (HAVE yoki URGENT).
     * @param staples yoqilgan asosiy masalliqlar (F-04).
     */
    fun evaluate(recipe: Recipe, marks: Map<String, MarkState>, staples: Set<String>): MatchResult {
        val missing = ArrayList<Ingredient>()
        var urgent = 0
        var user = 0
        for (ing in recipe.required) {
            val userHit = ing.productIds.filter { (marks[it] ?: MarkState.NONE) != MarkState.NONE }
            val satisfied = userHit.isNotEmpty() || ing.productIds.any { it in staples }
            if (!satisfied) {
                missing += ing
                continue
            }
            if (userHit.isNotEmpty()) user++
            if (userHit.any { marks[it] == MarkState.URGENT }) urgent++
        }
        val total = recipe.required.size
        val percent = if (total == 0) 100 else ((total - missing.size) * 100.0 / total).roundToInt()
        return MatchResult(recipe, missing, urgent, user, percent)
    }

    val ORDER: Comparator<MatchResult> = compareBy<MatchResult> { it.missingCount }
        .thenByDescending { it.urgentUsed }
        .thenByDescending { it.userUsed }
        .thenBy { it.recipe.minutes }
        .thenBy { SearchText.normalize(it.recipe.name) }

    fun match(
        recipes: List<Recipe>,
        marks: Map<String, MarkState>,
        staples: Set<String>,
    ): List<MatchResult> = recipes.asSequence()
        .map { evaluate(it, marks, staples) }
        .filter { it.missingCount <= MAX_MISSING && it.userUsed >= 1 }
        .sortedWith(ORDER)
        .toList()
}

/** F-07: filtrlar. Bir nechta filtr birga (VA) ishlaydi; taom turlari ichida — YOKI. */
data class RecipeFilter(
    val types: Set<RecipeType> = emptySet(),
    val quickOnly: Boolean = false,
    val difficulties: Set<Difficulty> = emptySet(),
    val meatlessOnly: Boolean = false,
    val excludeAllergens: Set<Allergen> = emptySet(),
) {
    val isEmpty: Boolean
        get() = types.isEmpty() && !quickOnly && difficulties.isEmpty() && !meatlessOnly && excludeAllergens.isEmpty()

    val activeCount: Int
        get() = types.size + (if (quickOnly) 1 else 0) + difficulties.size + (if (meatlessOnly) 1 else 0) +
            excludeAllergens.size

    fun accepts(recipe: Recipe, catalog: Catalog): Boolean {
        if (types.isNotEmpty() && recipe.type !in types) return false
        if (quickOnly && recipe.minutes > QUICK_MINUTES) return false
        if (difficulties.isNotEmpty() && recipe.difficulty !in difficulties) return false
        if (meatlessOnly && !recipe.meatless) return false
        if (excludeAllergens.isNotEmpty() && catalog.allergensOf(recipe).any { it in excludeAllergens }) return false
        return true
    }

    companion object {
        const val QUICK_MINUTES = 30
    }
}
