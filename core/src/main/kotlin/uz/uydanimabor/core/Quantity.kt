package uz.uydanimabor.core

import kotlin.math.abs
import kotlin.math.roundToInt

/** F-08: porsiya oʻzgarganda miqdorlarni qayta hisoblash va chiroyli koʻrsatish. */
object Quantity {
    const val MIN_SERVINGS = 1
    const val MAX_SERVINGS = 10

    fun scale(amount: Double, baseServings: Int, servings: Int): Double =
        amount * servings / baseServings.coerceAtLeast(1)

    fun format(amount: Double, unit: String): String {
        val u = unit.trim()
        val value = when (u) {
            "g", "ml" -> formatNumber(roundTo(amount, if (amount >= 250) 10.0 else 5.0).coerceAtLeast(5.0))
            "kg", "l" -> formatNumber(roundTo(amount, 0.1))
            else -> formatNumber(roundTo(amount, 0.5).coerceAtLeast(0.5))
        }
        return "$value $u"
    }

    fun format(ing: Ingredient, baseServings: Int, servings: Int): String =
        format(scale(ing.amount, baseServings, servings), ing.unit)

    private fun roundTo(x: Double, step: Double): Double = (x / step).roundToInt() * step

    private fun formatNumber(x: Double): String {
        val r = (x * 10).roundToInt() / 10.0
        return if (abs(r - r.roundToInt()) < 1e-9) r.roundToInt().toString()
        else r.toString().replace('.', ',')
    }
}
