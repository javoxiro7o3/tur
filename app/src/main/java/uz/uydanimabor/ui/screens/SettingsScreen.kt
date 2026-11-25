package uz.uydanimabor.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import uz.uydanimabor.BuildConfig
import uz.uydanimabor.MainViewModel
import uz.uydanimabor.R
import uz.uydanimabor.data.ThemeMode
import uz.uydanimabor.data.UserSettings
import uz.uydanimabor.ui.components.ConfirmDialog
import uz.uydanimabor.ui.components.SectionHeader

/** F-14: sozlamalar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: MainViewModel, settings: UserSettings, snackbar: SnackbarHostState, onBack: () -> Unit) {
    val staples by vm.staples.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val catalog = vm.catalog

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sozlamalar", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Orqaga")
                    }
                },
            )
        },
        contentWindowInsets = WindowInsets(0),
    ) { inner ->
        Column(
            Modifier
                .padding(inner)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding(),
        ) {
            SectionHeader("Asosiy masalliqlar")
            Text(
                "Bular sukut boʻyicha «bor» hisoblanadi. Uyda yoʻq boʻlsa, oʻchirib qoʻying.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            catalog.products.filter { it.isStaple }.forEach { p ->
                val on = p.id in staples
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .toggleable(value = on, role = Role.Switch, onValueChange = { vm.setStapleEnabled(p.id, it) })
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(p.icon, modifier = Modifier.padding(end = 12.dp).clearAndSetSemantics { })
                    Text(p.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Switch(checked = on, onCheckedChange = null)
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            SectionHeader("Mavzu")
            Column(Modifier.selectableGroup()) {
                ThemeMode.entries.forEach { mode ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .selectable(
                                selected = settings.theme == mode,
                                role = Role.RadioButton,
                                onClick = { vm.setTheme(mode) },
                            )
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = settings.theme == mode, onClick = null)
                        Text(mode.title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 12.dp))
                    }
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            SectionHeader("Maʼlumotlar")
            Text(
                "Barcha maʼlumotlar faqat shu telefonda saqlanadi. Ilova internetdan foydalanmaydi va hech qanday maʼlumot yigʻmaydi.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            OutlinedButton(
                onClick = { confirmDelete = true },
                modifier = Modifier
                    .padding(16.dp)
                    .heightIn(min = 48.dp),
            ) { Text("Maʼlumotlarni oʻchirish", color = MaterialTheme.colorScheme.error) }
            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            SectionHeader("Ilova haqida")
            Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                Text("Uyda nima bor?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("Ilova versiyasi: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Retseptlar bazasi versiyasi: ${catalog.version} · ${catalog.recipes.size} ta retsept · ${catalog.products.size} ta mahsulot",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    stringResource(R.string.contact_info),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(
                    "Ilova kaloriya, parhez yoki tibbiy maslahat bermaydi.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
                )
            }
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "Maʼlumotlarni oʻchirish?",
            text = "Belgilangan mahsulotlar, sevimlilar, xarid roʻyxati va sozlamalar oʻchiriladi. Buni qaytarib boʻlmaydi.",
            confirm = "Oʻchirish",
            onConfirm = {
                vm.deleteAllData()
                scope.launch { snackbar.showSnackbar("Maʼlumotlar oʻchirildi") }
            },
            onDismiss = { confirmDelete = false },
        )
    }
}
