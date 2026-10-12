package com.mediaautoskipper.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mediaautoskipper.history.SkipRecord
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HistoryContentTest {
    @get:Rule val rule = createComposeRule()

    private val ytm = "com.google.android.apps.youtube.music"
    private val spotify = "com.spotify.music"
    private val players = listOf(PlayerInfo(ytm, "YouTube Music"), PlayerInfo(spotify, "Spotify"))
    private fun r(id: Long, time: Long, pkg: String, title: String) =
        SkipRecord(id, time, pkg, title, "artist", "simplified-title")

    private fun show(records: List<SkipRecord>, onClear: () -> Unit = {}) = rule.setContent {
        var list by remember { mutableStateOf(records) }
        HistoryContent(list, players, { if (it == spotify) "Spotify" else "YouTube Music" }, onClear = {
            list = emptyList()
            onClear()
        })
    }

    @Test
    fun newerRecordIsAboveOlderOne() {
        show(listOf(r(1, 1000, ytm, "Older"), r(2, 2000, ytm, "Newer")))
        val older = rule.onNodeWithText("Older").fetchSemanticsNode().positionInRoot.y
        val newer = rule.onNodeWithText("Newer").fetchSemanticsNode().positionInRoot.y
        assert(newer < older) { "newer=$newer older=$older" }
    }

    @Test
    fun selectingSpotifyShowsOnlySpotifyRecords() {
        show(listOf(r(1, 1000, ytm, "FromYtm"), r(2, 2000, spotify, "FromSpotify")))
        rule.onNodeWithTag(HistoryTags.chip(spotify)).performClick()
        rule.onNodeWithText("FromSpotify").assertExists()
        rule.onNodeWithText("FromYtm").assertDoesNotExist()
    }

    @Test
    fun cancelKeepsRecordsAndConfirmShowsEmptyState() {
        var cleared = 0
        show(listOf(r(1, 1000, ytm, "Keep me")), onClear = { cleared++ })

        rule.onNodeWithTag(HistoryTags.CLEAR).performClick()
        rule.onNodeWithTag(HistoryTags.CANCEL).performClick()
        rule.onNodeWithText("Keep me").assertExists()
        assertEquals(0, cleared)

        rule.onNodeWithTag(HistoryTags.CLEAR).performClick()
        rule.onNodeWithTag(HistoryTags.CONFIRM).performClick()
        assertEquals(1, cleared)
        rule.onNodeWithTag(HistoryTags.EMPTY).assertExists()
    }

    @Test
    fun emptyHistoryShowsEmptyState() {
        show(emptyList())
        rule.onNodeWithTag(HistoryTags.EMPTY).assertExists()
    }

    @Test
    fun dislikedRecordShowsTheMarkerAndPlainSkipDoesNot() {
        show(
            listOf(
                r(1, 1000, ytm, "Plain skip"),
                r(2, 2000, ytm, "Skipped and disliked").copy(disliked = true),
            ),
        )
        rule.onNodeWithTag(HistoryTags.disliked(2)).assertExists()
        rule.onNodeWithTag(HistoryTags.disliked(1)).assertDoesNotExist()
    }
}
