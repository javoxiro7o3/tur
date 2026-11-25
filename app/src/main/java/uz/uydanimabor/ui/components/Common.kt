package uz.uydanimabor.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import uz.uydanimabor.core.MatchResult
import uz.uydanimabor.ui.theme.LocalExtraColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopBar(
    title: String,
    onSettings: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    actions: @Composable () -> Unit = {},
) {
    TopAppBar(
        title = { Text(title, fontWeight = FontWeight.SemiBold) },
        actions = {
            actions()
            IconButton(onClick = onSettings) {
                Icon(Icons.Filled.Settings, contentDescription = "Sozlamalar")
            }
        },
        scrollBehavior = scrollBehavior,
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirm: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = { onDismiss(); onConfirm() }) { Text(confirm) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Bekor qilish") } },
    )
}

/** F-12: 5 soniya davomida "Qaytarish" imkoni. */
fun CoroutineScope.showUndo(snackbar: SnackbarHostState, message: String, onUndo: () -> Unit) {
    launch {
        snackbar.currentSnackbarData?.dismiss()
        val timer = launch {
            delay(5_000)
            snackbar.currentSnackbarData?.dismiss()
        }
        val r = snackbar.showSnackbar(message, actionLabel = "Qaytarish", duration = SnackbarDuration.Indefinite)
        timer.cancel()
        if (r == SnackbarResult.ActionPerformed) onUndo()
    }
}

@Composable
fun EmptyState(emoji: String, title: String, text: String, action: (@Composable () -> Unit)? = null) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(emoji, style = MaterialTheme.typography.displayMedium, modifier = Modifier.clearAndSetSemantics { })
        Spacer(Modifier.size(12.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.size(8.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (action != null) {
            Spacer(Modifier.size(16.dp))
            action()
        }
    }
}

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .semantics { heading() },
    )
}

/** Moslik foizi uchun kichik belgi. */
@Composable
fun PercentBadge(percent: Int) {
    val ok = percent == 100
    Surface(
        shape = RoundedCornerShape(50),
        color = if (ok) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (ok) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Text(
            "$percent%",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
fun UrgentTag(text: String) {
    val extra = LocalExtraColors.current
    Surface(shape = RoundedCornerShape(50), color = extra.urgent, contentColor = extra.onUrgent) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

/** F-06: taom kartochkasi. */
@Composable
fun RecipeCard(
    match: MatchResult,
    missingNames: List<String>,
    haveNames: List<String>,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val r = match.recipe
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(start = 16.dp, top = 12.dp, bottom = 14.dp, end = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    r.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                PercentBadge(match.percent)
                IconButton(onClick = onToggleFavorite, modifier = Modifier.semantics {
                    contentDescription = if (isFavorite) "Sevimlilardan olib tashlash" else "Sevimlilarga qoʻshish"
                }) {
                    Icon(
                        if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = null,
                        tint = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                "⏱ ${r.minutes} daqiqa · ${r.difficulty.title} · ${r.servings} porsiya · ${r.type.title}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 12.dp),
            )
            Column(Modifier.padding(top = 8.dp, end = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (haveNames.isNotEmpty()) {
                    Text(
                        "✓ Bor: " + haveNames.joinToString(", "),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (missingNames.isNotEmpty()) {
                    Text(
                        "✗ Yetishmaydi: " + missingNames.joinToString(", "),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                if (match.urgentUsed > 0) {
                    Row(Modifier.padding(top = 2.dp)) {
                        UrgentTag("⚡ Tez buziladigan mahsulot ishlatiladi")
                    }
                }
            }
        }
    }
}
