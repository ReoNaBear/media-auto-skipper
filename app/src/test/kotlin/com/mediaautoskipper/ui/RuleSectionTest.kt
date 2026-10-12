package com.mediaautoskipper.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
}
