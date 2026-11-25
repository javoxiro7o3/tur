package uz.uydanimabor.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import uz.uydanimabor.MainViewModel
import uz.uydanimabor.ui.components.ConfirmDialog
import uz.uydanimabor.ui.components.EmptyState
import uz.uydanimabor.ui.components.MainTopBar
import uz.uydanimabor.ui.components.showUndo

/** F-09: xarid roʻyxati. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ShoppingScreen(vm: MainViewModel, snackbar: SnackbarHostState, onSettings: () -> Unit) {
    val items by vm.shopping.collectAsStateWithLifecycle()
    var text by rememberSaveable { mutableStateOf("") }
    var confirmClear by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    fun add() {
        vm.addShoppingItem(text)
        text = ""
    }

    Scaffold(
        topBar = {
            MainTopBar("Xarid roʻyxati", onSettings) {
                if (items.isNotEmpty()) {
                    IconButton(onClick = {
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, vm.shoppingShareText())
                        }
                        context.startActivity(Intent.createChooser(send, "Roʻyxatni ulashish"))
                    }) { Icon(Icons.Filled.Share, contentDescription = "Ulashish") }
                    IconButton(onClick = { confirmClear = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Hammasini tozalash")
                    }
                }
            }
        },
        contentWindowInsets = WindowInsets(0),
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("Mahsulot qoʻshish…") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { add() }),
                    modifier = Modifier.weight(1f),
                )
                FilledIconButton(
                    onClick = { add() },
                    enabled = text.isNotBlank(),
                    modifier = Modifier.heightIn(min = 48.dp),
                ) { Icon(Icons.Filled.Add, contentDescription = "Qoʻshish") }
            }
            if (items.isEmpty()) {
                EmptyState(
                    "🛒",
                    "Roʻyxat boʻsh",
                    "Retsept ekranidagi tugma bilan yetishmagan masalliqlarni qoʻshing yoki shu yerda qoʻlda yozing.",
                )
                return@Column
            }
            val left = items.count { !it.checked }
            Text(
                "Olinishi kerak: $left ta · Olingan: ${items.size - left} ta",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
                items(items, key = { it.id }) { item ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .toggleable(
                                value = item.checked,
                                role = Role.Checkbox,
                                onValueChange = { vm.setShoppingChecked(item.id, it) },
                            )
                            .padding(start = 8.dp, end = 4.dp)
                            .animateItem(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = item.checked, onCheckedChange = null, modifier = Modifier.padding(8.dp))
                        Text(
                            item.text,
                            style = MaterialTheme.typography.bodyLarge,
                            textDecoration = if (item.checked) TextDecoration.LineThrough else null,
                            color = if (item.checked) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { vm.deleteShoppingItem(item.id) }) {
                            Icon(Icons.Filled.Close, contentDescription = "Oʻchirish: ${item.text}")
                        }
                    }
                }
            }
        }
    }

    if (confirmClear) {
        ConfirmDialog(
            title = "Roʻyxatni tozalash?",
            text = "Xarid roʻyxatidagi barcha elementlar oʻchiriladi.",
            confirm = "Tozalash",
            onConfirm = {
                scope.launch {
                    val snapshot = vm.clearShopping()
                    showUndo(snackbar, "Xarid roʻyxati tozalandi") { vm.restoreShopping(snapshot) }
                }
            },
            onDismiss = { confirmClear = false },
        )
    }
}
