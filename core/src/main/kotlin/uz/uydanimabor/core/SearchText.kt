package uz.uydanimabor.core

/**
 * F-03: qidiruv uchun matnni normallashtirish.
 * oʻ, o', o‘, o` kabi apostrof turlari bir xil hisoblanadi.
 */
object SearchText {
    private val apostrophes = charArrayOf('\'', '`', '´', '‘', '’', 'ʻ', 'ʼ', 'ʹ', '′')

    fun normalize(s: String): String {
        val sb = StringBuilder(s.length)
        for (c in s.trim().lowercase()) {
            when {
                c in apostrophes -> sb.append('\'')
                c == 'ё' -> sb.append('е')
                c.isWhitespace() -> if (sb.isNotEmpty() && sb.last() != ' ') sb.append(' ')
                else -> sb.append(c)
            }
        }
        return sb.toString().trimEnd()
    }

    /** Apostrofsiz shakl: "gosht" ham "goʻsht" ni topishi uchun. */
    fun loose(s: String): String = normalize(s).replace("'", "")

    /** Soʻz yoki butun matn soʻrov bilan boshlanadimi (birinchi harflardan boshlab). */
    fun matches(query: String, candidates: List<String>): Boolean {
        val q = normalize(query)
        if (q.isEmpty()) return true
        val ql = q.replace("'", "")
        return candidates.any { c ->
            val n = normalize(c)
            val nl = n.replace("'", "")
            n.startsWith(q) || nl.startsWith(ql) ||
                n.split(' ', '-', '(').any { it.startsWith(q) } ||
                nl.split(' ', '-', '(').any { it.startsWith(ql) }
        }
    }

    fun matches(query: String, product: Product): Boolean =
        matches(query, listOf(product.name) + product.synonyms)
}

/** Oʻzbek lotin yozuvi uchun toʻgʻri belgilar: o' → oʻ, g' → gʻ, tutuq belgisi → ʼ. */
object UzTypography {
    fun apply(s: String): String {
        if (s.none { it == '\'' || it == '‘' || it == '’' || it == '`' }) return s
        val sb = StringBuilder(s.length)
        for ((i, c) in s.withIndex()) {
            if (c == '\'' || c == '‘' || c == '’' || c == '`') {
                val prev = if (i > 0) s[i - 1] else ' '
                sb.append(if (prev == 'o' || prev == 'O' || prev == 'g' || prev == 'G') 'ʻ' else 'ʼ')
            } else {
                sb.append(c)
            }
        }
        return sb.toString()
    }
}
