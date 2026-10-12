package com.mediaautoskipper.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.mediaautoskipper.settings.RuleKind
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RuleSectionTest {
    @get:Rule val rule = createComposeRule()
    private var current = listOf("Mix")

    private fun show(kind: RuleKind) {
        current = listOf("Mix")
        rule.setContent {
            var items by remember { mutableStateOf(current) }
            RuleSection(kind, items, onChange = { items = it; current = it })
        }
    }

    @Test
    fun blankInputIsRejectedAndListUnchanged() {
        show(RuleKind.KEYWORD)
        rule.onNodeWithTag(RuleTags.input(RuleKind.KEYWORD)).performTextInput("   ")
        rule.onNodeWithTag(RuleTags.add(RuleKind.KEYWORD)).performClick()
        assertEquals(listOf("Mix"), current)
        rule.onNodeWithText("Enter something", substring = true).assertExists()
    }

    @Test
    fun nightcoreAppearsInTheList() {
        show(RuleKind.KEYWORD)
        rule.onNodeWithTag(RuleTags.input(RuleKind.KEYWORD)).performTextInput("Nightcore")
        rule.onNodeWithTag(RuleTags.add(RuleKind.KEYWORD)).performClick()
        assertEquals(listOf("Mix", "Nightcore"), current)
        rule.onNodeWithText("Nightcore").assertExists()
    }

    @Test
    fun invalidRegexShowsErrorAndIsNotSaved() {
        show(RuleKind.REGEX)
        rule.onNodeWithTag(RuleTags.input(RuleKind.REGEX)).performTextInput("([")
        rule.onNodeWithTag(RuleTags.add(RuleKind.REGEX)).performClick()
        assertEquals(listOf("Mix"), current)
        rule.onNodeWithText("Invalid regular expression", substring = true).assertExists()
    }

    @Test
    fun deleteRemovesTheEntry() {
        show(RuleKind.KEYWORD)
        rule.onNodeWithText("Delete").performClick()
        assertEquals(emptyList(), current)
    }

    @Test
    fun dislikeOptionIsHiddenUnlessWired() {
        show(RuleKind.KEYWORD)
        rule.onNodeWithTag(RuleTags.dislike(RuleKind.KEYWORD, "Mix")).assertDoesNotExist()
    }

    @Test
    fun dislikeOptionDefaultsOffAndTogglesTheRuleKey() {
        val changes = mutableListOf<Pair<String, Boolean>>()
        rule.setContent {
            RuleSection(
                RuleKind.KEYWORD,
                listOf("Mix"),
                onChange = {},
                dislikeKeys = emptySet(),
                onDislikeChange = { key, on -> changes += key to on },
            )
        }
        rule.onNodeWithTag(RuleTags.dislike(RuleKind.KEYWORD, "Mix")).assertIsOff()
        rule.onNodeWithTag(RuleTags.dislike(RuleKind.KEYWORD, "Mix")).performClick()
        assertEquals(listOf("keyword:Mix" to true), changes)
    }

    @Test
    fun dislikeOptionShowsOnWhenTheKeyIsSet() {
        rule.setContent {
            RuleSection(
                RuleKind.REGEX,
                listOf("^a"),
                onChange = {},
                dislikeKeys = setOf("regex:^a"),
                onDislikeChange = { _, _ -> },
            )
        }
        rule.onNodeWithTag(RuleTags.dislike(RuleKind.REGEX, "^a")).assertIsOn()
    }
}
