package com.mediaautoskipper.monitor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DislikeSupportTest {

    @Test
    fun actionIdContainingThumbsDownIsTheDislikeAction() {
        val actions = listOf(
            CustomActionInfo("thumbs_up_action", "Like"),
            CustomActionInfo("shuffle_action", "Shuffle off"),
            CustomActionInfo("loop_mode_action", "Repeat off"),
            CustomActionInfo("thumbs_down_action", "Dislike"),
        )
        assertEquals("thumbs_down_action", DislikeSupport.findActionId(actions))
    }

    @Test
    fun idContainingDislikeMatchesCaseInsensitively() {
        assertEquals("DISLIKE_TRACK", DislikeSupport.findActionId(listOf(CustomActionInfo("DISLIKE_TRACK", "x"))))
    }

    @Test
    fun nameAloneMatchesWhenNoIdDoes() {
        val actions = listOf(CustomActionInfo("action_6", "Like"), CustomActionInfo("action_7", "dislike"))
        assertEquals("action_7", DislikeSupport.findActionId(actions))
    }

    @Test
    fun idMatchWinsOverAnEarlierNameMatch() {
        val actions = listOf(CustomActionInfo("a1", "Dislike"), CustomActionInfo("thumbs_down", "x"))
        assertEquals("thumbs_down", DislikeSupport.findActionId(actions))
    }

    @Test
    fun noMatchingActionMeansNoDislikeSupport() {
        val actions = listOf(CustomActionInfo("thumbs_up_action", "Like"), CustomActionInfo("shuffle_action", "Shuffle"))
        assertNull(DislikeSupport.findActionId(actions))
        assertNull(DislikeSupport.findActionId(emptyList()))
    }

    @Test
    fun ratingStatesFollowTheMetadataFlags() {
        assertEquals(RatingState.UNKNOWN, RatingState.of(published = false, rated = false, thumbUp = false))
        assertEquals(RatingState.UNRATED, RatingState.of(published = true, rated = false, thumbUp = false))
        assertEquals(RatingState.LIKED, RatingState.of(published = true, rated = true, thumbUp = true))
        assertEquals(RatingState.DISLIKED, RatingState.of(published = true, rated = true, thumbUp = false))
    }

    @Test
    fun onlyUnratedAndLikedAllowADislike() {
        assertTrue(RatingState.UNRATED.allowsDislike)
        assertTrue(RatingState.LIKED.allowsDislike)
        assertFalse(RatingState.DISLIKED.allowsDislike)
        assertFalse(RatingState.UNKNOWN.allowsDislike)
    }
}
