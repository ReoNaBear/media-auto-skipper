package com.mediaautoskipper.filter

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private data class KeywordCase(val keyword: String, val text: String, val expected: Boolean)

class TextFilterEngineTest {

    // --- Simplified Chinese detection (spec Example: detection cases) ---

    @Test
    fun simplifiedDetectionCases() {
        val cases = listOf(
            "爱你" to true, // 爱 is Simplified-only
            "愛你" to false, // Traditional
            "的" to false, // shared character
            "恋するフォーチュン国" to false, // contains kana
            "국경의 밤 爱" to false, // contains Hangul
            "国境" to false, // 国 is shared with Japanese, not in the set
            "爱" to true, // Japanese uses 愛
        )
        for ((text, expected) in cases) {
            assertEquals(expected, TextFilterEngine.isSimplified(text), "isSimplified($text)")
        }
    }

    @Test
    fun simplifiedRuleOffDoesNotMatch() {
        val config = FilterConfig(simplifiedEnabled = false, keywords = emptyList())
        assertFalse(TextFilterEngine.evaluate("爱你", "", config).matched)
    }

    @Test
    fun simplifiedRuleOnMatchesWithReason() {
        val config = FilterConfig(keywords = emptyList())
        assertEquals(MatchResult(true, "simplified-title"), TextFilterEngine.evaluate("爱你", "", config))
    }

    // --- Keyword blacklist (spec Example: keyword cases) ---

    @Test
    fun keywordCases() {
        val cases = listOf(
            KeywordCase("Mix", "Summer Mix 2024", true),
            KeywordCase("mix", "SUMMER MIX", true),
            KeywordCase("Mix", "Remix", false),
            KeywordCase("Mix", "Mixtape", false),
            KeywordCase("Full Album", "Artist - Full Album", true),
            KeywordCase("精选", "华语精选", true),
            KeywordCase("  ", "anything", false),
            KeywordCase("", "anything", false),
            KeywordCase("ＭＩＸ", "Summer Mix", true), // full-width normalized
            KeywordCase("合辑", "经典合輯", false), // literal, no Simplified-Traditional conversion
            KeywordCase("合輯", "經典合輯", true),
            KeywordCase("베스트", "2024 베스트 모음", true),
            KeywordCase("ベスト", "ベスト盤", true),
            KeywordCase("国", "国境", true), // the user chose this keyword explicitly
        )
        for (c in cases) {
            assertEquals(c.expected, TextFilterEngine.matchesKeyword(c.text, c.keyword), "'${c.keyword}' in '${c.text}'")
        }
    }

    @Test
    fun keywordMatchesWhenPrecedingOccurrenceIsInsideAWord() {
        // The first "mix" is inside "Remix"; the second is a whole word.
        assertTrue(TextFilterEngine.matchesKeyword("Remix and Mix", "Mix"))
    }

    @Test
    fun decomposedHangulMatchesComposedKeyword() {
        val decomposed = java.text.Normalizer.normalize("베스트", java.text.Normalizer.Form.NFD)
        assertTrue(TextFilterEngine.matchesKeyword(decomposed, "베스트"))
    }

    // --- Keyword language independence ---

    @Test
    fun simplifiedKeywordDoesNotHitJapaneseTitle() {
        val config = FilterConfig(simplifiedEnabled = false, keywords = listOf("爱"))
        assertFalse(TextFilterEngine.evaluate("愛のうた", "", config).matched)
    }

    @Test
    fun resultIndependentOfJvmDefaultLocale() {
        val before = java.util.Locale.getDefault()
        try {
            val config = FilterConfig(keywords = listOf("MIX", "İ"))
            java.util.Locale.setDefault(java.util.Locale.forLanguageTag("tr-TR"))
            val tr = TextFilterEngine.evaluate("Summer Mix", "", config)
            java.util.Locale.setDefault(java.util.Locale.ENGLISH)
            val en = TextFilterEngine.evaluate("Summer Mix", "", config)
            assertEquals(en, tr)
            assertTrue(en.matched)
        } finally {
            java.util.Locale.setDefault(before)
        }
    }

    // --- Regular expressions ---

    @Test
    fun regexMatchesCaseInsensitively() {
        val config = FilterConfig(simplifiedEnabled = false, keywords = emptyList(), regexes = listOf("^\\[.*live.*\\]"))
        assertEquals(MatchResult(true, "regex:^\\[.*live.*\\]"), TextFilterEngine.evaluate("[Live] Song", "", config))
    }

    @Test
    fun invalidRegexIsRejectedAndDoesNotBreakOtherRules() {
        assertNotNull(TextFilterEngine.validateRegex("(["))
        assertNull(TextFilterEngine.validateRegex("^\\[.*Live.*\\]"))
        val config = FilterConfig(simplifiedEnabled = false, keywords = listOf("Nightcore"), regexes = listOf("(["))
        assertTrue(TextFilterEngine.evaluate("Nightcore Song", "", config).matched)
        assertFalse(TextFilterEngine.evaluate("Other", "", config).matched)
    }

    @Test
    fun runawayRegexCountsAsNoMatchInsteadOfHanging() {
        val evil = "(a+)+$"
        val text = "a".repeat(40) + "!"
        val started = System.nanoTime()
        assertFalse(TextFilterEngine.matchesRegex(text, evil))
        val elapsedMs = (System.nanoTime() - started) / 1_000_000
        assertTrue(elapsedMs < 2000, "took ${elapsedMs}ms")
    }

    // --- Default blacklist ---

    @Test
    fun defaultBlacklistHasExactlySixEntries() {
        assertEquals(listOf("合輯", "合辑", "精選", "精选", "Full Album", "Mix"), FilterConfig().keywords)
    }

    // --- Title and artist ---

    @Test
    fun artistMatchReportsArtistReason() {
        assertEquals(MatchResult(true, "simplified-artist"), TextFilterEngine.evaluate("Song", "华语群星", FilterConfig()))
    }

    @Test
    fun keywordMatchReportsKeywordReason() {
        assertEquals(MatchResult(true, "keyword:合輯"), TextFilterEngine.evaluate("經典合輯 Vol.2", "", FilterConfig()))
    }

    @Test
    fun nothingMatchesPlainTitle() {
        assertEquals(MatchResult.NONE, TextFilterEngine.evaluate("愛你", "周杰倫", FilterConfig()))
    }
}
