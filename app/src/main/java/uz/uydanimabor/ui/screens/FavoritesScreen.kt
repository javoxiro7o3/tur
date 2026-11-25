package uz.uydanimabor.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.uydanimabor.MainViewModel
import uz.uydanimabor.core.Matcher
import uz.uydanimabor.ui.components.EmptyState
import uz.uydanimabor.ui.components.MainTopBar

/** F-10: sevimlilar. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(vm: MainViewModel, onOpen: (String) -> Unit, onSettings: () -> Unit) {
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    val marks by vm.marks.collectAsStateWithLifecycle()
    val staples by vm.staples.collectAsStateWithLifecycle()
    val catalog = vm.catalog
    val list = remember(favorites, marks, staples) {
        favorites.mapNotNull { catalog.recipeById[it] }.map { Matcher.evaluate(it, marks, staples) }
    }
    Scaffold(topBar = { MainTopBar("Sevimlilar", onSettings) }, contentWindowInsets = WindowInsets(0)) { inner ->
        if (list.isEmpty()) {
            androidx.compose.foundation.layout.Box(Modifier.padding(inner)) {
                EmptyState("❤️", "Hali sevimli taom yoʻq", "Taom kartochkasidagi yurakcha belgisini bosib saqlang.")
            }
        } else {
            LazyColumn(Modifier.padding(inner).fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
                cards(list, catalog, favorites, vm, onOpen)
            }
        }
    }
}
