package com.mediaautoskipper.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import com.mediaautoskipper.R
import kotlin.test.Test
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private fun note(): String =
    ApplicationProvider.getApplicationContext<android.content.Context>().getString(R.string.dislike_note)

/** The Rules screen warning must name the three consequences of the dislike option, in every language. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DislikeWarningCardTest {
    @get:Rule val rule = createComposeRule()

    @Test fun warningCardNamesAccountEffectNoUndoAndSlowerSkipping() {
        rule.setContent { DislikeWarningCard() }
        rule.onNodeWithText("About the dislike option").assertExists()
        rule.onNodeWithText("changes your recommendations", substring = true).assertExists()
        rule.onNodeWithText("cannot undo it", substring = true).assertExists()
        rule.onNodeWithText("slower and less reliable", substring = true).assertExists()
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "ko")
class DislikeWarningKoreanTest {
    @Test fun mentionsSlowerSkipping() = assertTrue(note().contains("느려지고"))
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "ja")
class DislikeWarningJapaneseTest {
    @Test fun mentionsSlowerSkipping() = assertTrue(note().contains("遅く"))
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "zh-rCN")
class DislikeWarningSimplifiedTest {
    @Test fun mentionsSlowerSkipping() = assertTrue(note().contains("变慢"))
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "zh-rTW")
class DislikeWarningTraditionalTest {
    @Test fun mentionsSlowerSkipping() = assertTrue(note().contains("變慢"))
}
