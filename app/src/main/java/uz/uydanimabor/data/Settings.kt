package uz.uydanimabor.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode(val title: String) {
    SYSTEM("Tizim"),
    LIGHT("Yorugʻ"),
    DARK("Qorongʻi"),
}

data class UserSettings(
    /** F-04: foydalanuvchi oʻchirgan asosiy masalliqlar. */
    val disabledStaples: Set<String> = emptySet(),
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val onboardingDone: Boolean = false,
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** F-14: foydalanuvchi sozlamalari DataStore'da saqlanadi. */
class SettingsRepository(private val context: Context) {
    private object Keys {
        val disabledStaples = stringSetPreferencesKey("disabled_staples")
        val theme = stringPreferencesKey("theme")
        val onboardingDone = booleanPreferencesKey("onboarding_done")
    }

    val settings: Flow<UserSettings> = context.dataStore.data.map { p ->
        UserSettings(
            disabledStaples = p[Keys.disabledStaples] ?: emptySet(),
            theme = p[Keys.theme]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
            onboardingDone = p[Keys.onboardingDone] ?: false,
        )
    }

    suspend fun setStapleEnabled(id: String, enabled: Boolean) {
        context.dataStore.edit { p ->
            val cur = p[Keys.disabledStaples] ?: emptySet()
            p[Keys.disabledStaples] = if (enabled) cur - id else cur + id
        }
    }

    suspend fun setTheme(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.theme] = mode.name }
    }

    suspend fun setOnboardingDone() {
        context.dataStore.edit { it[Keys.onboardingDone] = true }
    }

    /** Maʼlumotlarni oʻchirish: tanishuv qayta koʻrsatilmaydi. */
    suspend fun reset() {
        context.dataStore.edit {
            it.remove(Keys.disabledStaples)
            it.remove(Keys.theme)
        }
    }
}
