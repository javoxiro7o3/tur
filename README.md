# «Uyda nima bor?» — Android ilovasi

Foydalanuvchi uyidagi mahsulotlarni belgilaydi, ilova esa ulardan qaysi taomlarni pishirish
mumkinligini koʻrsatadi. Ilova toʻliq oflayn ishlaydi, hech qanday ruxsat soʻramaydi va maʼlumot yigʻmaydi.

## Texnologiyalar

| Parametr | Qiymat |
|---|---|
| Til / UI | Kotlin, Jetpack Compose, Material 3 |
| Arxitektura | MVVM, bitta Activity, Compose Navigation |
| Minimal / Target API | 24 (Android 7.0) / 36 (Android 16) |
| Foydalanuvchi maʼlumotlari | Room (belgilar, sevimlilar, xarid roʻyxati) |
| Sozlamalar | DataStore |
| Retseptlar bazasi | `app/src/main/assets/*.json` — APK ichida keladi |
| Ruxsatlar | Yoʻq (INTERNET ham yoʻq) |

## Modullar

- `core/` — sof Kotlin modul: maʼlumot modellari, JSON parser, **moslik algoritmi** (`Matcher.kt`),
  qidiruv (`SearchText`), porsiya hisoblash (`Quantity`) va filtrlar. Unit testlar shu yerda.
- `app/` — Android ilova: ekranlar (`ui/screens`), Room va DataStore (`data/`), `MainViewModel`.

## Qurish

Talablar: JDK 17+ va Android SDK (platform 36, build-tools 36). `local.properties` faylida `sdk.dir` koʻrsatiladi.

```bash
gradle :core:check            # unit testlar + moslik algoritmi qamrovi (≥80%)
gradle :app:assembleDebug      # debug APK
gradle :app:assembleRelease    # imzolangan release APK
gradle :app:bundleRelease      # Google Play uchun AAB
```

Natijalar: `app/build/outputs/apk/release/app-release.apk` va `app/build/outputs/bundle/release/app-release.aab`.

### Imzolash kaliti

Release build loyiha ildizidagi `keystore.properties` faylini oʻqiydi (gitga **qoʻshilmaydi**):

```properties
storeFile=uydanimabor-release.jks
storePassword=...
keyAlias=uydanimabor
keyPassword=...
```

APK Signature Scheme v2 va v3 bilan imzolanadi. **Kalit va parolni xavfsiz joyda, zaxira nusxasi bilan
saqlang: kalit yoʻqolsa, ilovani hech qachon yangilab boʻlmaydi.**

## Retseptlar bazasini tahrirlash

Baza ikkita JSON fayldan iborat:

- `app/src/main/assets/mahsulotlar.json` — mahsulotlar: `id` (lotin harflarida, oʻzgarmas), `nomi`,
  `sinonimlar` (qidiruv uchun, ruscha nomlar ham), `toifa` (7 tadan biri), `asosiy`, `allergen`, `belgi` (emoji).
- `app/src/main/assets/retseptlar.json` — retseptlar, texnik topshiriqdagi maydonlar boʻyicha
  (`kerakli`, `ixtiyoriy`, `qadamlar`, `ogohlantirish`, `goshtsiz`, `tekshirgan`, `tekshirilgan_sana`) va
  qoʻshimcha `milliy` (oʻzbek milliy taomi) belgisi.

Qoidalar:

1. Apostrofni oddiy `'` bilan yozing (`Sho'rva`). Ilova uni avtomatik `oʻ`, `gʻ`, `ʼ` ga aylantiradi.
2. `kerakli` elementidagi `mahsulotlar` — muqobillar roʻyxati (`["mol_goshti", "qoy_goshti"]`).
3. Asosiy masalliqlar (tuz, yogʻ, suv…) odatda `ixtiyoriy` ga yoziladi va ekranda «asosiy» deb chiqadi.
   Ularsiz taom chiqmasa (masalan, shirinlikda shakar) — `kerakli` ga yozing.
4. Allergenlar retseptga yozilmaydi: ular mahsulotlardan avtomatik hisoblanadi.
5. Goʻsht, tovuq, qiyma, baliq va tuxumli taomlarda qadamda «toʻliq pishguncha» koʻrsatmasi boʻlishi shart.
6. Oʻzgartirgandan keyin `versiya` ni oshiring va `gradle :core:test` ni ishga tushiring — testlar
   barcha mahsulot havolalarini, retseptlar sonini va xavfsizlik qoidalarini tekshiradi.

## Moslik algoritmi

`core/src/main/kotlin/uz/uydanimabor/core/Matcher.kt`. Taom roʻyxatga kiradi, agar unga koʻpi bilan
2 ta kerakli masalliq yetishmasa va foydalanuvchi belgilagan mahsulotlardan kamida bittasi ishlatilsa.
Saralash: yetishmaydiganlar ↑, «tez ishlatish kerak» ↓, foydalanuvchi mahsulotlari ↓, vaqt ↑, nomi.
Texnik topshiriqdagi misol (`MatcherTest.specExample_orderAndValues` va
`CatalogTest.specExample_onRealDatabase`) testlarda tekshiriladi.

## Funksional talablar xaritasi

| ID | Qayerda |
|---|---|
| F-01, F-02, F-03, F-12 | `ProductsScreen.kt`, `SearchText` |
| F-04, F-14 | `SettingsScreen.kt`, `data/Settings.kt` |
| F-05, F-06, F-07 | `RecipesScreen.kt`, `Matcher.kt` |
| F-08 | `RecipeScreen.kt` (porsiya, taymer, ekran oʻchmaydi, ogohlantirish) |
| F-09 | `ShoppingScreen.kt` (Telegram va boshqalarga ulashish) |
| F-10 | `FavoritesScreen.kt` |
| F-11 | Room + DataStore |
| F-13 | `OnboardingScreen.kt` |

## Hali qilinishi kerak (buyurtmachi tomonidan)

- Retseptlar tajribali oshpaz tomonidan tekshirilishi va amalda pishirib koʻrilishi kerak
  (`tekshirgan` / `tekshirilgan_sana` maydonlari hozircha `null`).
- Matnlarni muharrir tekshiruvi.
- `app/src/main/res/values/strings.xml` dagi `contact_info` ga aloqa maʼlumotini yozish.
- Paket nomi `uz.uydanimabor` ni Android Developer Console'da roʻyxatdan oʻtkazish (developer verification).
