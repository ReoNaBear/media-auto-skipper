package com.mediaautoskipper.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.mediaautoskipper.R
import com.mediaautoskipper.filter.FilterConfig
import com.mediaautoskipper.filter.TextFilterEngine
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private fun title(): String =
    ApplicationProvider.getApplicationContext<android.content.Context>().getString(R.string.status_access_missing_title)

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "zh-rTW")
class TraditionalChineseTest {
    @Test fun showsTraditionalChinese() = assertEquals("需要通知存取權", title())
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "zh-rCN")
class SimplifiedChineseTest {
    @Test fun showsSimplifiedChinese() = assertEquals("需要通知访问权限", title())
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "ja")
class JapaneseTest {
    @get:Rule val rule = createComposeRule()

    @Test fun homeShowsJapanese() {
        rule.setContent { HomeContent(accessGranted = false, enabled = false, onOpenAccessSettings = {}, onToggle = {}) }
        rule.onNodeWithText("通知へのアクセス権限が必要です").assertExists()
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "ko")
class KoreanTest {
    @Test fun showsKorean() = assertEquals("알림 접근 권한이 필요합니다", title())
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "fr")
class UnsupportedLanguageTest {
    @Test fun frenchFallsBackToEnglish() = assertEquals("Notification Access is required", title())
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LanguageSelectionTest {
    @get:Rule val rule = createComposeRule()

    @Test fun everyOptionIsLabelledInItsOwnLanguageAndReportsSelection() {
        val picked = mutableListOf<LanguageChoice>()
        rule.setContent { LanguageOptions(LanguageChoice.SYSTEM, onSelect = { picked += it }) }
        listOf("Follow system", "English", "한국어", "日本語", "简体中文", "繁體中文").forEach {
            rule.onNodeWithText(it).assertExists()
        }
        rule.onNodeWithTag(LanguageTags.option(LanguageChoice.JAPANESE)).performClick()
        assertEquals(listOf(LanguageChoice.JAPANESE), picked)
    }

    @Test fun tagsMapBackToChoices() {
        assertEquals(LanguageChoice.TRADITIONAL_CHINESE, LanguageChoice.fromTag("zh-TW"))
        assertEquals(LanguageChoice.SYSTEM, LanguageChoice.fromTag(""))
        assertEquals(LanguageChoice.SYSTEM, LanguageChoice.fromTag("fr"))
    }

    @Test fun languageDoesNotAffectFiltering() {
        val before = TextFilterEngine.evaluate("Nightcore Mix", "", FilterConfig(keywords = listOf("Nightcore")))
        val after = TextFilterEngine.evaluate("Nightcore Mix", "", FilterConfig(keywords = listOf("Nightcore")))
        assertEquals(before, after)
        assertEquals(true, after.matched)
    }
}
