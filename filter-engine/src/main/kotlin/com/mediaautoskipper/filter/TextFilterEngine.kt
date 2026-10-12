package com.mediaautoskipper.filter

import java.text.Normalizer
import java.util.Locale
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

/** Rules the engine evaluates. Keywords and regexes are user supplied and may be in any language. */
data class FilterConfig(
    val simplifiedEnabled: Boolean = true,
    val keywords: List<String> = DefaultBlacklist.keywords,
    val regexes: List<String> = emptyList(),
)

/** [reason] is null when nothing matched, otherwise e.g. `simplified-title`, `keyword:合輯`, `regex:^\[Live\]`. */
data class MatchResult(val matched: Boolean, val reason: String? = null) {
    companion object {
        val NONE = MatchResult(false)
    }
}

object DefaultBlacklist {
    val keywords: List<String> = listOf("合輯", "合辑", "精選", "精选", "Full Album", "Mix")
}

object TextFilterEngine {

    /** Longest time a single user regex may run on one text before it is treated as not matching. */
    private const val REGEX_BUDGET_NANOS = 50_000_000L

    /** Evaluates the title, then the artist; within each: Simplified Chinese, keywords, regexes. */
    fun evaluate(title: String, artist: String, config: FilterConfig): MatchResult {
        evaluateField("title", title, config)?.let { return it }
        evaluateField("artist", artist, config)?.let { return it }
        return MatchResult.NONE
    }

    private fun evaluateField(field: String, text: String, config: FilterConfig): MatchResult? {
        if (text.isBlank()) return null
        if (config.simplifiedEnabled && isSimplified(text)) {
            return MatchResult(true, "simplified-$field")
        }
        for (keyword in config.keywords) {
            if (matchesKeyword(text, keyword)) return MatchResult(true, "keyword:$keyword")
        }
        for (expr in config.regexes) {
            if (matchesRegex(text, expr)) return MatchResult(true, "regex:$expr")
        }
        return null
    }

    /**
     * True when [text] holds a Simplified-Chinese-exclusive character and no Japanese kana
     * or Korean Hangul (which marks the text as not Chinese).
     */
    fun isSimplified(text: String): Boolean {
        val normalized = normalize(text)
        var found = false
        for (ch in normalized) {
            if (isKanaOrHangul(ch)) return false
            if (ch in SimplifiedChars.set) found = true
        }
        return found
    }

    /**
     * Literal, case-insensitive match after NFKC normalization. A keyword made only of
     * ASCII letters, digits and spaces must match whole words ("Mix" does not match "Remix");
     * any other keyword matches as a substring. Blank keywords never match.
     */
    fun matchesKeyword(text: String, keyword: String): Boolean {
        val k = normalize(keyword).trim()
        if (k.isEmpty()) return false
        val t = normalize(text)
        if (!k.all { it.code < 128 && (it.isLetterOrDigit() || it == ' ') }) return t.contains(k)
        var from = 0
        while (true) {
            val i = t.indexOf(k, from)
            if (i < 0) return false
            val before = if (i == 0) null else t[i - 1]
            val after = t.getOrNull(i + k.length)
            if ((before == null || !before.isLetterOrDigit()) && (after == null || !after.isLetterOrDigit())) {
                return true
            }
            from = i + 1
        }
    }

    /** Case-insensitive search on the original text. Invalid expressions and runaway matches count as no match. */
    fun matchesRegex(text: String, expr: String): Boolean {
        val pattern = compileOrNull(expr) ?: return false
        return try {
            pattern.matcher(BudgetedCharSequence(text, System.nanoTime() + REGEX_BUDGET_NANOS)).find()
        } catch (_: RegexBudgetExceeded) {
            false
        }
    }

    /** Null when [expr] compiles; otherwise the parser's message, for showing to the user. */
    fun validateRegex(expr: String): String? {
        if (expr.isBlank()) return "empty"
        return try {
            Pattern.compile(expr, FLAGS)
            null
        } catch (e: PatternSyntaxException) {
            e.description ?: "invalid"
        }
    }

    private const val FLAGS = Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE

    private fun compileOrNull(expr: String): Pattern? =
        if (expr.isBlank()) null else try {
            Pattern.compile(expr, FLAGS)
        } catch (_: PatternSyntaxException) {
            null
        }

    private fun normalize(s: String): String =
        Normalizer.normalize(s, Normalizer.Form.NFKC).lowercase(Locale.ROOT)

    private fun isKanaOrHangul(ch: Char): Boolean {
        val c = ch.code
        return c in 0x3040..0x30FF || c in 0x31F0..0x31FF || // Hiragana, Katakana, Katakana extensions
            c in 0x1100..0x11FF || c in 0x3130..0x318F || c in 0xAC00..0xD7AF || // Hangul
            c in 0xFF66..0xFF9F // half-width Katakana (also covered by NFKC)
    }
}

private class RegexBudgetExceeded : RuntimeException()

/** Aborts a regex run that exceeds its time budget, so a user's pathological pattern cannot stall the service. */
private class BudgetedCharSequence(
    private val inner: CharSequence,
    private val deadline: Long,
) : CharSequence {
    override val length: Int get() = inner.length

    override fun get(index: Int): Char {
        if (System.nanoTime() > deadline) throw RegexBudgetExceeded()
        return inner[index]
    }

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence =
        BudgetedCharSequence(inner.subSequence(startIndex, endIndex), deadline)

    override fun toString(): String = inner.toString()
}
