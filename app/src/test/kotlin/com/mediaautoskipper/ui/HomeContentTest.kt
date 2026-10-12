package com.mediaautoskipper.ui

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
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
class HomeContentTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun withoutAccessShowsExplanationAndDisabledSwitch() {
        rule.setContent { HomeContent(accessGranted = false, enabled = false, onOpenAccessSettings = {}, onToggle = {}) }
        rule.onNodeWithText("Notification Access is required").assertExists()
        rule.onNodeWithText("Open Notification Access settings").assertExists()
        rule.onNodeWithTag(Tags.ENABLE_SWITCH).assertIsNotEnabled()
    }

    @Test
    fun withAccessSwitchWorksAndShowsBackgroundReminderWhenOn() {
        val toggles = mutableListOf<Boolean>()
        rule.setContent {
            val on = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
            HomeContent(
                accessGranted = true,
                enabled = on.value,
                onOpenAccessSettings = {},
                onToggle = { toggles += it; on.value = it },
            )
        }
        rule.onNodeWithTag(Tags.ENABLE_SWITCH).assertIsEnabled().performClick()
        assertEquals(listOf(true), toggles)
        rule.onNodeWithText("Skipping keeps running", substring = true).assertExists()
    }
}
