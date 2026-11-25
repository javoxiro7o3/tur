package uz.uydanimabor.core

/** F-01 dagi 7 ta mahsulot toifasi. */
enum class Category(val key: String, val title: String) {
    GOSHT_BALIQ("gosht_baliq", "Goʻsht va baliq"),
    SUT_TUXUM("sut_tuxum", "Sut mahsulotlari va tuxum"),
    SABZAVOT("sabzavot", "Sabzavotlar"),
    DON_UN("don_un", "Don, un va dukkaklilar"),
    NON_XAMIR("non_xamir", "Non va xamir mahsulotlari"),
    MEVA("meva", "Mevalar"),
    BOSHQA("boshqa", "Boshqalar");

    companion object {
        fun of(key: String): Category = entries.firstOrNull { it.key == key }
            ?: throw IllegalArgumentException("Nomaʼlum toifa: $key")
    }
}

/** Mahsulotdagi allergenlar. */
enum class Allergen(val key: String, val title: String) {
    SUT("sut", "sut"),
    TUXUM("tuxum", "tuxum"),
    GLUTEN("gluten", "gluten"),
    BALIQ("baliq", "baliq"),
    YONGOQ("yongoq", "yongʻoq");

    companion object {
        fun of(key: String): Allergen = entries.firstOrNull { it.key == key }
            ?: throw IllegalArgumentException("Nomaʼlum allergen: $key")
    }
}

data class Product(
    val id: String,
    val name: String,
    val synonyms: List<String>,
    val category: Category,
    /** F-04: sukut boʻyicha "bor" hisoblanadigan asosiy masalliq. */
    val isStaple: Boolean,
    val allergen: Allergen?,
    val icon: String,
)

/** F-07 dagi taom turlari. */
enum class RecipeType(val key: String, val title: String) {
    SHORVA("shorva", "Shoʻrva"),
    ASOSIY("asosiy", "Asosiy taom"),
    SALAT("salat", "Salat"),
    NONUSHTA("nonushta", "Nonushta"),
    BOTQA("botqa", "Boʻtqa"),
    XAMIR("xamir", "Xamir taomlari"),
    SHIRINLIK("shirinlik", "Shirinlik");

    companion object {
        fun of(key: String): RecipeType = entries.firstOrNull { it.key == key }
            ?: throw IllegalArgumentException("Nomaʼlum taom turi: $key")
    }
}

enum class Difficulty(val key: String, val title: String) {
    OSON("oson", "Oson"),
    ORTACHA("o'rtacha", "Oʻrtacha"),
    MURAKKAB("murakkab", "Murakkab");

    companion object {
        fun of(key: String): Difficulty {
            val k = SearchText.normalize(key)
            return entries.firstOrNull { SearchText.normalize(it.key) == k }
                ?: throw IllegalArgumentException("Nomaʼlum qiyinlik: $key")
        }
    }
}

/** Masalliq: muqobil mahsulotlar guruhi + miqdor. */
data class Ingredient(
    val productIds: List<String>,
    val amount: Double,
    val unit: String,
)

data class Step(val text: String, val timerMinutes: Int?)

data class Recipe(
    val id: String,
    val name: String,
    val type: RecipeType,
    val minutes: Int,
    val difficulty: Difficulty,
    val servings: Int,
    val required: List<Ingredient>,
    val optional: List<Ingredient>,
    val steps: List<Step>,
    val warning: String?,
    val meatless: Boolean,
    val national: Boolean,
    val reviewedBy: String?,
    val reviewedDate: String?,
)

/** F-02: mahsulot belgisi holati. */
enum class MarkState { NONE, HAVE, URGENT;

    /** Bir bosish: bor → tez ishlatish kerak → belgi olib tashlanadi. */
    fun next(): MarkState = when (this) {
        NONE -> HAVE
        HAVE -> URGENT
        URGENT -> NONE
    }
}
