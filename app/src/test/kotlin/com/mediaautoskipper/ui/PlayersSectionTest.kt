package com.mediaautoskipper.ui

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PlayersSectionTest {
    @get:Rule val rule = createComposeRule()

    private val ytm = PlayerInfo("com.google.android.apps.youtube.music", "YouTube Music")
    private val other = PlayerInfo("org.other.player", "Other Player")

    @Test
    fun simplifiedSwitchReportsChange() {
        val changes = mutableListOf<Boolean>()
        rule.setContent {
            RuleTogglesAndPlayers(true, { changes += it }, listOf(ytm), emptyList(), {}, {})
        }
        rule.onNodeWithTag(PlayerTags.SIMPLIFIED_SWITCH).performClick()
        assertEquals(listOf(false), changes)
    }

    @Test
    fun installedAppCanBeAddedAndMonitoredOneRemoved() {
        val added = mutableListOf<String>()
        val removed = mutableListOf<String>()
        rule.setContent {
            RuleTogglesAndPlayers(true, {}, listOf(ytm), listOf(ytm, other), { removed += it }, { added += it })
        }
        rule.onNodeWithText("Other Player").assertExists()
        rule.onNodeWithText("Add").performClick()
        rule.onNodeWithText("Remove").performClick()
        assertEquals(listOf("org.other.player"), added)
        assertEquals(listOf(ytm.pkg), removed)
    }

    @Test
    fun simplifiedDislikeOptionIsAvailableAndShowsTheAccountNote() {
        val changes = mutableListOf<Boolean>()
        rule.setContent {
            RuleTogglesAndPlayers(
                true, {}, listOf(ytm), emptyList(), {}, {},
                simplifiedDislike = false,
                onSimplifiedDislikeChange = { changes += it },
            )
        }
        rule.onNodeWithTag(PlayerTags.SIMPLIFIED_DISLIKE).assertIsEnabled().assertIsOff().performClick()
        assertEquals(listOf(true), changes)
        rule.onNodeWithText("cannot undo it", substring = true).assertExists()
    }

    @Test
    fun simplifiedDislikeOptionIsDisabledWhileTheRuleIsOff() {
        rule.setContent {
            RuleTogglesAndPlayers(
                false, {}, listOf(ytm), emptyList(), {}, {},
                simplifiedDislike = true,
                onSimplifiedDislikeChange = {},
            )
        }
        rule.onNodeWithTag(PlayerTags.SIMPLIFIED_DISLIKE).assertIsNotEnabled()
    }
}
