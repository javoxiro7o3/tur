package uz.uydanimabor.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import uz.uydanimabor.MainViewModel
import uz.uydanimabor.core.Category
import uz.uydanimabor.core.MarkState
import uz.uydanimabor.core.Product
import uz.uydanimabor.core.SearchText
import uz.uydanimabor.ui.components.ConfirmDialog
import uz.uydanimabor.ui.components.EmptyState
import uz.uydanimabor.ui.components.MainTopBar
import uz.uydanimabor.ui.components.ProductIcon
import uz.uydanimabor.ui.components.SectionHeader
import uz.uydanimabor.ui.components.showUndo
import uz.uydanimabor.ui.theme.LocalExtraColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(
    vm: MainViewModel,
    snackbar: SnackbarHostState,
    onShowRecipes: () -> Unit,
    onSettings: () -> Unit,
) {
    val marks by vm.marks.collectAsStateWithLifecycle()
    val staples by vm.staples.collectAsStateWithLifecycle()
    val matches by vm.allMatches.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var confirmClear by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val catalog = vm.catalog

    val grouped = remember(query) {
        catalog.products.filter { SearchText.matches(query, it) }.groupBy { it.category }
    }
    val markedCount = marks.size

    Scaffold(
        topBar = {
            MainTopBar("Uyda nima bor?", onSettings) {
                if (markedCount > 0) {
                    IconButton(onClick = { confirmClear = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Hammasini tozalash")
                    }
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onShowRecipes,
                text = { Text("Taomlarni koʻrish (${matches.size})", fontWeight = FontWeight.SemiBold) },
                icon = { Text("🍲", modifier = Modifier.clearAndSetSemantics { }) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        },
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            // F-02: ekran tepasida belgilanganlar soni
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (markedCount == 0) "Uyingizdagi mahsulotlarni belgilang"
                    else "Belgilangan: $markedCount ta mahsulot",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                "1 bosish — bor · 2 — tez ishlatish kerak · 3 — olib tashlash",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Mahsulot qidirish…") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Qidiruvni tozalash")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
            )
            if (grouped.isEmpty()) {
                EmptyState("🔍", "Hech narsa topilmadi", "«$query» boʻyicha mahsulot yoʻq. Boshqa nom bilan qidirib koʻring.")
                return@Column
            }
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 108.dp),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 96.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                Category.entries.forEach { cat ->
                    val items = grouped[cat] ?: return@forEach
                    item(key = "h_${cat.key}", span = { GridItemSpan(maxLineSpan) }) {
                        SectionHeader(cat.title, Modifier.padding(top = 4.dp))
                    }
                    items(items, key = { it.id }) { p ->
                        val state = when {
                            p.isStaple -> if (p.id in staples) MarkState.HAVE else MarkState.NONE
                            else -> marks[p.id] ?: MarkState.NONE
                        }
                        ProductTile(p, state, onClick = { vm.tapProduct(p.id) })
                    }
                }
            }
        }
    }

    if (confirmClear) {
        ConfirmDialog(
            title = "Hammasini tozalash?",
            text = "Belgilangan barcha mahsulotlar olib tashlanadi.",
            confirm = "Tozalash",
            onConfirm = {
                scope.launch {
                    val snapshot = vm.clearMarks()
                    showUndo(snackbar, "Belgilar tozalandi") { vm.restoreMarks(snapshot) }
                }
            },
            onDismiss = { confirmClear = false },
        )
    }
}

@Composable
private fun ProductTile(p: Product, state: MarkState, onClick: () -> Unit) {
    val extra = LocalExtraColors.current
    val scheme = MaterialTheme.colorScheme
    // Harakat: 150 ms lik rang oʻzgarishi (tizimda animatsiya oʻchirilsa, u ham oʻchadi)
    val bg by animateColorAsState(
        when (state) {
            MarkState.NONE -> scheme.surfaceContainerHigh
            MarkState.HAVE -> extra.have
            MarkState.URGENT -> extra.urgent
        },
        animationSpec = tween(150),
        label = "tileBg",
    )
    val fg = when (state) {
        MarkState.NONE -> scheme.onSurface
        MarkState.HAVE -> extra.onHave
        MarkState.URGENT -> extra.onUrgent
    }
    val stateText = when {
        p.isStaple && state == MarkState.HAVE -> "bor, asosiy masalliq"
        p.isStaple -> "yoʻq, asosiy masalliq oʻchirilgan"
        state == MarkState.HAVE -> "bor"
        state == MarkState.URGENT -> "tez ishlatish kerak"
        else -> "belgilanmagan"
    }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = bg,
        contentColor = fg,
        border = if (state == MarkState.HAVE) BorderStroke(2.dp, scheme.primary) else null,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 96.dp)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = p.name
                stateDescription = stateText
            },
    ) {
        Box {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                ProductIcon(p.icon, p.name)
                Text(
                    p.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (state != MarkState.NONE) FontWeight.SemiBold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.clearAndSetSemantics { },
                )
                if (state == MarkState.URGENT) {
                    Text(
                        "tez ishlatish",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clearAndSetSemantics { },
                    )
                } else if (p.isStaple) {
                    Text(
                        "asosiy",
                        style = MaterialTheme.typography.labelSmall,
                        color = fg.copy(alpha = 0.8f),
                        modifier = Modifier.clearAndSetSemantics { },
                    )
                }
            }
            if (state != MarkState.NONE) {
                Text(
                    if (state == MarkState.URGENT) "⚡" else "✓",
                    color = if (state == MarkState.URGENT) Color(0xFF111827) else scheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .clearAndSetSemantics { },
                )
            }
        }
    }
}
