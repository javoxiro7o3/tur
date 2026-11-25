package uz.uydanimabor.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.uydanimabor.MainViewModel
import uz.uydanimabor.core.Allergen
import uz.uydanimabor.core.Catalog
import uz.uydanimabor.core.Difficulty
import uz.uydanimabor.core.MarkState
import uz.uydanimabor.core.MatchResult
import uz.uydanimabor.core.Matcher
import uz.uydanimabor.core.RecipeFilter
import uz.uydanimabor.core.RecipeType
import uz.uydanimabor.ui.components.EmptyState
import uz.uydanimabor.ui.components.MainTopBar
import uz.uydanimabor.ui.components.RecipeCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipesScreen(
    vm: MainViewModel,
    onOpen: (String) -> Unit,
    onGoProducts: () -> Unit,
    onSettings: () -> Unit,
) {
    val groups by vm.groups.collectAsStateWithLifecycle()
    val filter by vm.filter.collectAsStateWithLifecycle()
    val marks by vm.marks.collectAsStateWithLifecycle()
    val staples by vm.staples.collectAsStateWithLifecycle()
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    var showAll by rememberSaveable { mutableStateOf(false) }
    var sheet by remember { mutableStateOf(false) }
    val catalog = vm.catalog

    Scaffold(
        topBar = { MainTopBar("Taomlar", onSettings) },
        contentWindowInsets = WindowInsets(0),
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = filter.activeCount > 0,
                    onClick = { sheet = true },
                    label = { Text(if (filter.activeCount > 0) "Filtrlar (${filter.activeCount})" else "Filtrlar") },
                    modifier = Modifier.heightIn(min = 48.dp),
                )
                FilterChip(
                    selected = filter.quickOnly,
                    onClick = { vm.setFilter(filter.copy(quickOnly = !filter.quickOnly)) },
                    label = { Text("Tez (30 daqiqagacha)") },
                    modifier = Modifier.heightIn(min = 48.dp),
                )
                FilterChip(
                    selected = filter.meatlessOnly,
                    onClick = { vm.setFilter(filter.copy(meatlessOnly = !filter.meatlessOnly)) },
                    label = { Text("Goʻshtsiz") },
                    modifier = Modifier.heightIn(min = 48.dp),
                )
                FilterChip(
                    selected = showAll,
                    onClick = { showAll = !showAll },
                    label = { Text("Barcha retseptlar") },
                    modifier = Modifier.heightIn(min = 48.dp),
                )
            }

            if (showAll) {
                val all = remember(marks, staples, filter) {
                    catalog.recipes
                        .filter { filter.accepts(it, catalog) }
                        .map { Matcher.evaluate(it, marks, staples) }
                        .sortedBy { it.recipe.name }
                }
                RecipeList(catalog, favorites, vm, onOpen) {
                    header("Barcha retseptlar (${all.size})")
                    cards(all, catalog, favorites, vm, onOpen)
                }
            } else if (marks.isEmpty()) {
                EmptyState(
                    "🧺",
                    "Avval mahsulotlarni belgilang",
                    "Uyingizda bor mahsulotlarni belgilang — ilova ulardan nima pishirish mumkinligini koʻrsatadi.",
                ) {
                    Button(onClick = onGoProducts, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text("Mahsulotlarni belgilash")
                    }
                }
            } else if (groups.total == 0) {
                EmptyState(
                    "🤔",
                    "Mos taom topilmadi",
                    if (filter.isEmpty) "Yana bir nechta mahsulot belgilab koʻring."
                    else "Filtrlarni yumshatib koʻring.",
                ) {
                    if (!filter.isEmpty) {
                        TextButton(onClick = { vm.setFilter(RecipeFilter()) }) { Text("Filtrlarni tozalash") }
                    }
                }
            } else {
                RecipeList(catalog, favorites, vm, onOpen) {
                    if (groups.ready.isNotEmpty()) {
                        header("Hozir pishirsa boʻladi (${groups.ready.size})")
                        cards(groups.ready, catalog, favorites, vm, onOpen)
                    }
                    if (groups.missingOne.isNotEmpty()) {
                        header("1 ta mahsulot yetishmaydi (${groups.missingOne.size})")
                        cards(groups.missingOne, catalog, favorites, vm, onOpen)
                    }
                    if (groups.missingTwo.isNotEmpty()) {
                        header("2 ta mahsulot yetishmaydi (${groups.missingTwo.size})")
                        cards(groups.missingTwo, catalog, favorites, vm, onOpen)
                    }
                }
            }
        }
    }

    if (sheet) {
        val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = { sheet = false }, sheetState = state) {
            FilterSheet(filter, onChange = vm::setFilter, onClose = { sheet = false })
        }
    }
}

@Composable
private fun RecipeList(
    catalog: Catalog,
    favorites: List<String>,
    vm: MainViewModel,
    onOpen: (String) -> Unit,
    content: LazyListScope.() -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        content = content,
    )
}

private fun LazyListScope.header(text: String) {
    item(key = "h_$text") {
        Text(
            text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
                .semantics { heading() },
        )
    }
}

fun LazyListScope.cards(
    list: List<MatchResult>,
    catalog: Catalog,
    favorites: List<String>,
    vm: MainViewModel,
    onOpen: (String) -> Unit,
) {
    items(list, key = { it.recipe.id }) { m ->
        val missing = m.missing.map { catalog.ingredientName(it) }
        val marks = vm.marks.value
        val have = m.recipe.required.mapNotNull { ing ->
            ing.productIds.firstOrNull { (marks[it] ?: MarkState.NONE) != MarkState.NONE }
                ?.let { catalog.product(it)?.name }
        }
        RecipeCard(
            match = m,
            missingNames = missing,
            haveNames = have,
            isFavorite = m.recipe.id in favorites,
            onToggleFavorite = { vm.toggleFavorite(m.recipe.id) },
            onClick = { onOpen(m.recipe.id) },
            modifier = Modifier.animateItem(),
        )
    }
}

/** F-07: filtrlar. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun FilterSheet(filter: RecipeFilter, onChange: (RecipeFilter) -> Unit, onClose: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text("Filtrlar", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.size(12.dp))
        Group("Taom turi") {
            RecipeType.entries.forEach { t ->
                Chip(t.title, t in filter.types) {
                    onChange(filter.copy(types = filter.types.toggle(t)))
                }
            }
        }
        Group("Vaqt va tarkib") {
            Chip("Tez (30 daqiqagacha)", filter.quickOnly) { onChange(filter.copy(quickOnly = !filter.quickOnly)) }
            Chip("Goʻshtsiz", filter.meatlessOnly) { onChange(filter.copy(meatlessOnly = !filter.meatlessOnly)) }
        }
        Group("Qiyinlik") {
            Difficulty.entries.forEach { d ->
                Chip(d.title, d in filter.difficulties) {
                    onChange(filter.copy(difficulties = filter.difficulties.toggle(d)))
                }
            }
        }
        Group("Allergensiz") {
            listOf(Allergen.SUT, Allergen.TUXUM, Allergen.GLUTEN).forEach { a ->
                Chip("${a.title}siz", a in filter.excludeAllergens) {
                    onChange(filter.copy(excludeAllergens = filter.excludeAllergens.toggle(a)))
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(
                onClick = { onChange(RecipeFilter()) },
                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
            ) { Text("Tozalash") }
            Button(onClick = onClose, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("Tayyor") }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun Group(title: String, content: @Composable () -> Unit) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp).semantics { heading() },
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { content() }
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        modifier = Modifier.heightIn(min = 48.dp),
    )
}

private fun <T> Set<T>.toggle(v: T): Set<T> = if (v in this) this - v else this + v

