package uz.uydanimabor

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uz.uydanimabor.core.Catalog
import uz.uydanimabor.core.Ingredient
import uz.uydanimabor.core.MarkState
import uz.uydanimabor.core.MatchResult
import uz.uydanimabor.core.Matcher
import uz.uydanimabor.core.Quantity
import uz.uydanimabor.core.Recipe
import uz.uydanimabor.core.RecipeFilter
import uz.uydanimabor.data.FavoriteEntity
import uz.uydanimabor.data.MarkEntity
import uz.uydanimabor.data.ShoppingEntity
import uz.uydanimabor.data.ThemeMode
import uz.uydanimabor.data.UserSettings

/** Taomlar ekrani uchun guruhlangan natija (F-05). */
data class RecipeGroups(
    val ready: List<MatchResult> = emptyList(),
    val missingOne: List<MatchResult> = emptyList(),
    val missingTwo: List<MatchResult> = emptyList(),
) {
    val total: Int get() = ready.size + missingOne.size + missingTwo.size
}

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val container = app as UydaApp
    private val db = container.database
    private val settingsRepo = container.settings
    val catalog: Catalog get() = container.catalog

    val settings: StateFlow<UserSettings?> = settingsRepo.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val marks: StateFlow<Map<String, MarkState>> = db.marks().observe()
        .map { list ->
            list.associate { it.productId to if (it.state == 2) MarkState.URGENT else MarkState.HAVE }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    val favorites: StateFlow<List<String>> = db.favorites().observe()
        .map { list -> list.map { it.recipeId } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val shopping: StateFlow<List<ShoppingEntity>> = db.shopping().observe()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Yoqilgan asosiy masalliqlar (F-04). */
    val staples: StateFlow<Set<String>> = settingsRepo.settings
        .map { s -> catalog.stapleIds - s.disabledStaples }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    private val _filter = MutableStateFlow(RecipeFilter())
    val filter: StateFlow<RecipeFilter> = _filter.asStateFlow()

    /** Barcha mos taomlar (filtrsiz) — "Taomlarni koʻrish (N)" tugmasi uchun. */
    val allMatches: StateFlow<List<MatchResult>> = combine(marks, staples) { m, s ->
        Matcher.match(catalog.recipes, m, s)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** F-05: uch guruhdagi mos taomlar, filtrlar qoʻllangan. */
    val groups: StateFlow<RecipeGroups> = combine(allMatches, _filter) { list, f ->
        val filtered = if (f.isEmpty) list else list.filter { f.accepts(it.recipe, catalog) }
        RecipeGroups(
            ready = filtered.filter { it.missingCount == 0 },
            missingOne = filtered.filter { it.missingCount == 1 },
            missingTwo = filtered.filter { it.missingCount == 2 },
        )
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, RecipeGroups())

    fun evaluate(recipe: Recipe): MatchResult = Matcher.evaluate(recipe, marks.value, staples.value)

    // ---------------------------------------------------------------- mahsulotlar
    fun tapProduct(id: String) {
        val product = catalog.product(id) ?: return
        viewModelScope.launch {
            if (product.isStaple) {
                settingsRepo.setStapleEnabled(id, id !in staples.value)
                return@launch
            }
            when ((marks.value[id] ?: MarkState.NONE).next()) {
                MarkState.NONE -> db.marks().delete(id)
                MarkState.HAVE -> db.marks().upsert(MarkEntity(id, 1))
                MarkState.URGENT -> db.marks().upsert(MarkEntity(id, 2))
            }
        }
    }

    /** F-12: hammasini tozalash. Qaytarish uchun nusxani qaytaradi. */
    suspend fun clearMarks(): List<MarkEntity> {
        val snapshot = db.marks().all()
        db.marks().clear()
        return snapshot
    }

    fun restoreMarks(snapshot: List<MarkEntity>) {
        viewModelScope.launch { db.marks().insertAll(snapshot) }
    }

    // ---------------------------------------------------------------- filtrlar
    fun setFilter(f: RecipeFilter) {
        _filter.value = f
    }

    // ---------------------------------------------------------------- sevimlilar
    fun toggleFavorite(recipeId: String) {
        viewModelScope.launch {
            if (recipeId in favorites.value) db.favorites().delete(recipeId)
            else db.favorites().upsert(FavoriteEntity(recipeId, System.currentTimeMillis()))
        }
    }

    // ---------------------------------------------------------------- xarid roʻyxati
    /** F-09: yetishmayotgan masalliqlarni bitta tugma bilan qoʻshish. Qoʻshilganlar sonini qaytaradi. */
    suspend fun addMissingToShopping(recipe: Recipe, missing: List<Ingredient>, servings: Int): Int {
        val existing = db.shopping().all().filter { !it.checked }.mapNotNull { it.productId }.toSet()
        var added = 0
        for (ing in missing) {
            val pid = ing.productIds.first()
            if (pid in existing) continue
            val name = catalog.ingredientName(ing)
            val qty = Quantity.format(ing, recipe.servings, servings)
            db.shopping().insert(ShoppingEntity(text = "$name — $qty (${recipe.name})", productId = pid))
            added++
        }
        return added
    }

    fun addShoppingItem(text: String) {
        val t = text.trim()
        if (t.isEmpty()) return
        viewModelScope.launch { db.shopping().insert(ShoppingEntity(text = t)) }
    }

    fun setShoppingChecked(id: Long, checked: Boolean) {
        viewModelScope.launch { db.shopping().setChecked(id, checked) }
    }

    fun deleteShoppingItem(id: Long) {
        viewModelScope.launch { db.shopping().delete(id) }
    }

    suspend fun clearShopping(): List<ShoppingEntity> {
        val snapshot = db.shopping().all()
        db.shopping().clear()
        return snapshot
    }

    fun restoreShopping(snapshot: List<ShoppingEntity>) {
        viewModelScope.launch { db.shopping().replaceAll(snapshot) }
    }

    fun shoppingShareText(): String = buildString {
        append("🛒 Xarid roʻyxati\n")
        shopping.value.forEach { append(if (it.checked) "✅ " else "▫️ ").append(it.text).append('\n') }
        append("\n— «Uyda nima bor?» ilovasidan")
    }

    // ---------------------------------------------------------------- sozlamalar
    fun setStapleEnabled(id: String, enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setStapleEnabled(id, enabled) }
    }

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch { settingsRepo.setTheme(mode) }
    }

    fun finishOnboarding() {
        viewModelScope.launch { settingsRepo.setOnboardingDone() }
    }

    /** F-14: maʼlumotlarni oʻchirish. */
    fun deleteAllData() {
        viewModelScope.launch {
            db.marks().clear()
            db.favorites().clear()
            db.shopping().clear()
            settingsRepo.reset()
        }
    }
}
