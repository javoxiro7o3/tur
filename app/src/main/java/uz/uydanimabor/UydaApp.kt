package uz.uydanimabor

import android.app.Application
import uz.uydanimabor.core.Catalog
import uz.uydanimabor.data.AppDatabase
import uz.uydanimabor.data.SettingsRepository

/** Oddiy qoʻlda bogʻliqliklar konteyneri. */
class UydaApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.create(this) }
    val settings: SettingsRepository by lazy { SettingsRepository(this) }

    /** Retseptlar bazasi APK ichida keladi va internetsiz ishlaydi. */
    val catalog: Catalog by lazy {
        Catalog.parse(
            assets.open("mahsulotlar.json").bufferedReader().use { it.readText() },
            assets.open("retseptlar.json").bufferedReader().use { it.readText() },
        )
    }
}
