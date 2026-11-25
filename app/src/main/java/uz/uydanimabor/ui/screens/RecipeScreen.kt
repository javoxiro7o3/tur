package uz.uydanimabor.ui.screens

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.SystemClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import uz.uydanimabor.MainViewModel
import uz.uydanimabor.core.Ingredient
import uz.uydanimabor.core.MarkState
import uz.uydanimabor.core.Matcher
import uz.uydanimabor.core.Quantity
import uz.uydanimabor.core.Step
import uz.uydanimabor.ui.components.EmptyState
import uz.uydanimabor.ui.components.PercentBadge
import uz.uydanimabor.ui.components.UrgentTag
import uz.uydanimabor.ui.theme.LocalExtraColors

/** F-08: retsept ekrani. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeScreen(
    vm: MainViewModel,
    recipeId: String,
    snackbar: SnackbarHostState,
    onBack: () -> Unit,
    onOpenShopping: () -> Unit,
) {
    val catalog = vm.catalog
    val recipe = catalog.recipeById[recipeId]
    val marks by vm.marks.collectAsStateWithLifecycle()
    val staples by vm.staples.collectAsStateWithLifecycle()
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    // Retsept ochiq turganda ekran oʻchmaydi
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    if (recipe == null) {
        EmptyState("😕", "Retsept topilmadi", "Bu retsept bazada yoʻq.")
        return
    }
    var servings by rememberSaveable(recipeId) { mutableStateOf(recipe.servings) }
    val match = remember(marks, staples) { Matcher.evaluate(recipe, marks, staples) }
    val isFav = recipe.id in favorites
    val allergens = remember { catalog.allergensOf(recipe) }
    val optionalAllergens = remember { catalog.optionalAllergensOf(recipe) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(recipe.name, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Orqaga")
                    }
                },
                actions = {
                    IconButton(onClick = { vm.toggleFavorite(recipe.id) }) {
                        Icon(
                            if (isFav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = if (isFav) "Sevimlilardan olib tashlash" else "Sevimlilarga qoʻshish",
                            tint = if (isFav) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
            )
        },
        contentWindowInsets = WindowInsets(0),
    ) { inner ->
        LazyColumn(
            Modifier
                .padding(inner)
                .fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            // Duxovka yoki oldindan ivitish kerak boʻlsa, ogohlantirish tepada turadi
            recipe.warning?.let { w ->
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Warning, contentDescription = "Ogohlantirish")
                            Text(
                                w,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(start = 12.dp),
                            )
                        }
                    }
                }
            }
            item {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "⏱ ${recipe.minutes} daqiqa · ${recipe.difficulty.title} · ${recipe.type.title}",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )
                        PercentBadge(match.percent)
                    }
                    if (allergens.isNotEmpty()) {
                        Text(
                            "Allergenlar: " + allergens.joinToString(", ") { it.title },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    if (optionalAllergens.isNotEmpty()) {
                        Text(
                            "Ixtiyoriy masalliqlarda: " + optionalAllergens.joinToString(", ") { it.title },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (match.urgentUsed > 0) {
                        Row(Modifier.padding(top = 8.dp)) { UrgentTag("⚡ Tez buziladigan mahsulot ishlatiladi") }
                    }
                }
            }
            item { ServingsStepper(servings) { servings = it } }
            item { Header("Masalliqlar") }
            val haveCheck: (Ingredient) -> Boolean = { ing ->
                ing.productIds.any { (marks[it] ?: MarkState.NONE) != MarkState.NONE || it in staples }
            }
            recipe.required.forEach { ing ->
                item { IngredientRow(catalog.ingredientName(ing), Quantity.format(ing, recipe.servings, servings), haveCheck(ing), null) }
            }
            val (stapleOpt, otherOpt) = recipe.optional.partition { ing ->
                ing.productIds.all { catalog.product(it)?.isStaple == true }
            }
            stapleOpt.forEach { ing ->
                item {
                    IngredientRow(catalog.ingredientName(ing), Quantity.format(ing, recipe.servings, servings), haveCheck(ing), "asosiy")
                }
            }
            otherOpt.forEach { ing ->
                item {
                    IngredientRow(catalog.ingredientName(ing), Quantity.format(ing, recipe.servings, servings), haveCheck(ing), "ixtiyoriy")
                }
            }
            if (match.missing.isNotEmpty()) {
                item {
                    Button(
                        onClick = {
                            scope.launch {
                                val n = vm.addMissingToShopping(recipe, match.missing, servings)
                                val msg = if (n == 0) "Bu mahsulotlar roʻyxatda allaqachon bor"
                                else "$n ta mahsulot xarid roʻyxatiga qoʻshildi"
                                val r = snackbar.showSnackbar(msg, actionLabel = "Koʻrish", duration = SnackbarDuration.Short)
                                if (r == SnackbarResult.ActionPerformed) onOpenShopping()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .heightIn(min = 52.dp),
                    ) {
                        Icon(Icons.Filled.ShoppingCart, contentDescription = null)
                        Text(
                            "Yetishmaganlarni xarid roʻyxatiga qoʻshish (${match.missing.size})",
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
            item { Header("Tayyorlash") }
            itemsIndexed(recipe.steps) { i, step -> StepRow(i + 1, step, recipeId) }
            item {
                HorizontalDivider(Modifier.padding(16.dp))
                Text(
                    if (recipe.reviewedBy != null) "Retseptni tekshirgan: ${recipe.reviewedBy}, ${recipe.reviewedDate ?: ""}"
                    else "Retsept oshpaz tekshiruvini kutmoqda",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun Header(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
            .semantics { heading() },
    )
}

@Composable
private fun ServingsStepper(value: Int, onChange: (Int) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Porsiya", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        FilledTonalIconButton(
            onClick = { onChange((value - 1).coerceAtLeast(Quantity.MIN_SERVINGS)) },
            enabled = value > Quantity.MIN_SERVINGS,
            modifier = Modifier
                .size(48.dp)
                .semantics { contentDescription = "Porsiyani kamaytirish" },
        ) { Text("−", style = MaterialTheme.typography.titleLarge, modifier = Modifier.clearAndSetSemantics { }) }
        Text(
            "$value",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .widthIn(min = 56.dp)
                .padding(horizontal = 8.dp)
                .semantics { contentDescription = "$value porsiya"; liveRegion = LiveRegionMode.Polite },
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        FilledTonalIconButton(
            onClick = { onChange((value + 1).coerceAtMost(Quantity.MAX_SERVINGS)) },
            enabled = value < Quantity.MAX_SERVINGS,
            modifier = Modifier
                .size(48.dp)
                .semantics { contentDescription = "Porsiyani koʻpaytirish" },
        ) { Icon(Icons.Filled.Add, contentDescription = null) }
    }
}

@Composable
private fun IngredientRow(name: String, qty: String, have: Boolean, tag: String?) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "$name, $qty, " + (if (have) "bor" else "yetishmaydi") + (tag?.let { ", $it" } ?: "")
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = CircleShape,
            color = if (have) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.errorContainer,
            contentColor = if (have) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.size(28.dp),
        ) {
            Text(
                if (have) "✓" else "✗",
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(
                name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (!have && tag == null) FontWeight.SemiBold else FontWeight.Normal,
                color = if (!have && tag == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
            if (tag != null) {
                Text(tag, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(qty, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
    }
}

/** Raqamlangan qadam va (boʻlsa) taymer. */
@Composable
private fun StepRow(n: Int, step: Step, recipeId: String) {
    Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(32.dp),
        ) {
            Text(
                "$n",
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(top = 5.dp).semantics { contentDescription = "$n-qadam" },
            )
        }
        Column(Modifier.padding(start = 12.dp).weight(1f)) {
            Text(step.text, style = MaterialTheme.typography.bodyLarge)
            step.timerMinutes?.takeIf { it > 0 }?.let { StepTimer(it, "$recipeId#$n") }
        }
    }
}

/**
 * Qadam taymeri. Tugash vaqti saqlanadi, shuning uchun ekran burilganda ham taymer davom etadi.
 * Fonda hech qanday jarayon ishlamaydi.
 */
@Composable
private fun StepTimer(minutes: Int, key: String) {
    val total = minutes * 60_000L
    var endAt by rememberSaveable(key) { mutableLongStateOf(0L) } // 0 = ishlamayapti
    var pausedLeft by rememberSaveable(key) { mutableLongStateOf(total) }
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    var finished by rememberSaveable(key) { mutableStateOf(false) }
    val running = endAt != 0L

    LaunchedEffect(running) {
        while (endAt != 0L) {
            now = SystemClock.elapsedRealtime()
            if (now >= endAt) {
                endAt = 0L
                pausedLeft = total
                finished = true
                beep()
                break
            }
            delay(250)
        }
    }
    val left = if (running) (endAt - now).coerceAtLeast(0) else pausedLeft
    val mm = left / 60_000
    val ss = (left % 60_000) / 1000
    val extra = LocalExtraColors.current

    Row(
        Modifier.padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        when {
            running -> {
                Text(
                    "⏱ %02d:%02d".format(mm, ss),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.semantics { contentDescription = "Qoldi $mm daqiqa $ss soniya" },
                )
                OutlinedButton(onClick = {
                    pausedLeft = (endAt - SystemClock.elapsedRealtime()).coerceAtLeast(0)
                    endAt = 0L
                }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Pauza") }
                OutlinedButton(onClick = { endAt = 0L; pausedLeft = total }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text("Bekor")
                }
            }
            else -> {
                Button(
                    onClick = {
                        finished = false
                        endAt = SystemClock.elapsedRealtime() + pausedLeft
                        now = SystemClock.elapsedRealtime()
                    },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(
                        if (pausedLeft < total) "▶ Davom ettirish (%02d:%02d)".format(mm, ss)
                        else "⏱ Taymer: $minutes daqiqa",
                    )
                }
                if (finished) {
                    Surface(shape = RoundedCornerShape(50), color = extra.urgent, contentColor = extra.onUrgent) {
                        Text(
                            "Vaqt tugadi!",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .semantics { liveRegion = LiveRegionMode.Assertive },
                        )
                    }
                }
            }
        }
    }
}

private fun beep() {
    runCatching {
        val tg = ToneGenerator(AudioManager.STREAM_ALARM, 90)
        tg.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 1500)
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ tg.release() }, 2000)
    }
}

