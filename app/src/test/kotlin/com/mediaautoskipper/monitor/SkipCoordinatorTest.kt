package com.mediaautoskipper.monitor

import com.mediaautoskipper.settings.AppSettings
import com.mediaautoskipper.settings.DefaultPlayers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private class FakeTarget(
    override val packageName: String = DefaultPlayers.SPOTIFY,
    var skipThrows: Boolean = false,
    var dislikeId: String? = null,
    var rating: RatingState = RatingState.UNRATED,
    var dislikeThrows: Boolean = false,
    var ratingSupported: Boolean = false,
    var ratingThrows: Boolean = false,
) : SkipTarget {
    var skips = 0
    var keys = 0

    /** Every dislike and skip call in the order it happened, e.g. `dislike:thumbs_down_action`, `skip`. */
    val calls = mutableListOf<String>()

    override fun skipToNext() {
        if (skipThrows) throw IllegalStateException("boom")
        skips++
        calls += "skip"
    }

    override fun dislikeActionId(): String? = dislikeId

    override fun ratingState(): RatingState = rating

    override fun supportsRating(): Boolean = ratingSupported

    override fun sendRating(thumbUp: Boolean) {
        if (ratingThrows) throw IllegalStateException("boom")
        calls += if (thumbUp) "rate:up" else "rate:down"
    }

    override fun sendCustomAction(id: String) {
        if (dislikeThrows) throw IllegalStateException("boom")
        calls += "dislike:$id"
    }

    override fun sendNextKey() {
        keys++
    }
}

class SkipCoordinatorTest {
    private var clock = 1_000L
    private val events = mutableListOf<SkipEvent>()
    private val paused = mutableListOf<String>()
    private val coordinator = SkipCoordinator({ clock }, { events += it }, { paused += it })
    private val settings = AppSettings()

    private fun feed(target: SkipTarget, title: String, artist: String = "", active: Boolean = true) =
        coordinator.onMetadata(target, title, artist, clock, active, settings)

    @Test
    fun matchingTrackIsSkippedOnce() {
        val t = FakeTarget()
        feed(t, "华语精选")
        assertEquals(1, t.skips)
        assertEquals(1, events.size)
        assertEquals("华语精选", events[0].title)
        assertFalse(events[0].usedFallback)
    }

    @Test
    fun nonMatchingTrackSendsNothing() {
        val t = FakeTarget()
        feed(t, "Yesterday", "The Beatles")
        assertEquals(0, t.skips + t.keys)
        assertTrue(events.isEmpty())
    }

    @Test
    fun fallsBackToKeyEventWhenSkipToNextThrows() {
        val t = FakeTarget(skipThrows = true)
        feed(t, "华语精选")
        assertEquals(0, t.skips)
        assertEquals(1, t.keys)
        assertTrue(events.single().usedFallback)
    }

    @Test
    fun unmonitoredPlayerIgnored() {
        val t = FakeTarget(packageName = "com.other.player")
        feed(t, "华语精选")
        assertEquals(0, t.skips)
    }

    @Test
    fun switchOffIgnoresEverything() {
        val t = FakeTarget()
        feed(t, "华语精选", active = false)
        assertEquals(0, t.skips)
    }

    @Test
    fun latencyIsMeasuredFromMetadataArrival() {
        val t = FakeTarget()
        val arrived = clock
        clock += 40
        coordinator.onMetadata(t, "华语精选", "", arrived, true, settings)
        assertEquals(40L, events.single().latencyMs)
    }

    @Test
    fun repeatedMetadataSkipsOnlyOnce() {
        val t = FakeTarget()
        repeat(3) { feed(t, "华语精选", "A") }
        assertEquals(1, t.skips)
    }

    @Test
    fun sameTrackSkippedAgainAfterADifferentTrackWasSeen() {
        val t = FakeTarget()
        feed(t, "华语精选")
        feed(t, "Yesterday")
        feed(t, "华语精选")
        assertEquals(2, t.skips)
    }

    @Test
    fun tenSkipsInWindowPauseAndNotify() {
        val t = FakeTarget()
        repeat(SkipCoordinator.LOOP_MAX_SKIPS) { i ->
            clock += 1000
            feed(t, "华语精选 $i")
        }
        assertEquals(SkipCoordinator.LOOP_MAX_SKIPS, t.skips)
        assertEquals(listOf(DefaultPlayers.SPOTIFY), paused)
        clock += 1000
        feed(t, "华语精选 11")
        assertEquals(SkipCoordinator.LOOP_MAX_SKIPS, t.skips)
    }

    @Test
    fun nonMatchingTrackResumesAfterPause() {
        val t = FakeTarget()
        repeat(SkipCoordinator.LOOP_MAX_SKIPS) { i ->
            clock += 1000
            feed(t, "华语精选 $i")
        }
        feed(t, "Yesterday")
        feed(t, "华语精选 again")
        assertEquals(SkipCoordinator.LOOP_MAX_SKIPS + 1, t.skips)
    }

    @Test
    fun resumesAfterSixtySecondsWithoutSkip() {
        val t = FakeTarget()
        repeat(SkipCoordinator.LOOP_MAX_SKIPS) { i ->
            clock += 1000
            feed(t, "华语精选 $i")
        }
        clock += SkipCoordinator.LOOP_WINDOW_MS
        feed(t, "华语精选 later")
        assertEquals(SkipCoordinator.LOOP_MAX_SKIPS + 1, t.skips)
    }

    @Test
    fun slowSkipsNeverTriggerProtection() {
        val t = FakeTarget()
        repeat(12) { i ->
            clock += 10_000
            feed(t, "华语精选 $i")
        }
        assertEquals(12, t.skips)
        assertTrue(paused.isEmpty())
    }

    @Test
    fun disablingSimplifiedRuleStopsSkippingThatTrack() {
        val t = FakeTarget()
        coordinator.onMetadata(t, "爱你", "", clock, true, AppSettings(simplifiedEnabled = false))
        assertEquals(0, t.skips)
        coordinator.onMetadata(t, "Summer Mix", "", clock, true, AppSettings(simplifiedEnabled = false))
        assertEquals(1, t.skips)
    }

    // --- dislike before skip ---

    private val dislikeId = "thumbs_down_action"
    private val dislikeMix = AppSettings(dislikeRules = setOf("keyword:Mix"))

    private fun feedWith(t: FakeTarget, s: AppSettings, title: String, active: Boolean = true) =
        coordinator.onMetadata(t, title, "", clock, active, s)

    @Test
    fun markedKeywordSendsDislikeBeforeTheSkip() {
        val t = FakeTarget(dislikeId = dislikeId)
        feedWith(t, dislikeMix, "Summer Mix 2024")
        assertEquals(listOf("dislike:$dislikeId", "skip"), t.calls)
        assertTrue(events.single().disliked)
    }

    @Test
    fun unmarkedRuleOnlySkips() {
        val t = FakeTarget(dislikeId = dislikeId)
        feedWith(t, dislikeMix, "合輯 Vol.1")
        assertEquals(listOf("skip"), t.calls)
        assertFalse(events.single().disliked)
    }

    @Test
    fun markedSimplifiedRuleSendsDislikeForTitleAndArtist() {
        val s = AppSettings(dislikeRules = setOf("simplified"))
        val t = FakeTarget(dislikeId = dislikeId)
        feedWith(t, s, "爱你")
        coordinator.onMetadata(t, "Hello", "刘德华", clock, true, s)
        assertEquals(listOf("dislike:$dislikeId", "skip", "dislike:$dislikeId", "skip"), t.calls)
    }

    @Test
    fun markedRegexSendsDislike() {
        val s = AppSettings(simplifiedEnabled = false, keywords = emptyList(), regexes = listOf("""^\[Live\]"""), dislikeRules = setOf("""regex:^\[Live\]"""))
        val t = FakeTarget(dislikeId = dislikeId)
        feedWith(t, s, "[Live] Song")
        assertEquals(listOf("dislike:$dislikeId", "skip"), t.calls)
    }

    @Test
    fun playerWithoutADislikeActionStillSkipsWithoutError() {
        val t = FakeTarget(dislikeId = null)
        feedWith(t, dislikeMix, "Summer Mix 2024")
        assertEquals(listOf("skip"), t.calls)
        assertFalse(events.single().disliked)
    }

    @Test
    fun throwingDislikeStillSkips() {
        val t = FakeTarget(dislikeId = dislikeId, dislikeThrows = true)
        feedWith(t, dislikeMix, "Summer Mix 2024")
        assertEquals(listOf("skip"), t.calls)
        assertFalse(events.single().disliked)
    }

    @Test
    fun alreadyDislikedTrackIsNotDislikedAgain() {
        val t = FakeTarget(dislikeId = dislikeId, rating = RatingState.DISLIKED)
        feedWith(t, dislikeMix, "Summer Mix 2024")
        assertEquals(listOf("skip"), t.calls)
        assertFalse(events.single().disliked)
    }

    @Test
    fun unpublishedRatingMeansNoDislike() {
        val t = FakeTarget(dislikeId = dislikeId, rating = RatingState.UNKNOWN)
        feedWith(t, dislikeMix, "Summer Mix 2024")
        assertEquals(listOf("skip"), t.calls)
    }

    @Test
    fun previouslyLikedTrackIsDisliked() {
        val t = FakeTarget(dislikeId = dislikeId, rating = RatingState.LIKED)
        feedWith(t, dislikeMix, "Summer Mix 2024")
        assertEquals(listOf("dislike:$dislikeId", "skip"), t.calls)
    }

    @Test
    fun masterSwitchOffSendsNothing() {
        val t = FakeTarget(dislikeId = dislikeId)
        feedWith(t, dislikeMix, "Summer Mix 2024", active = false)
        assertTrue(t.calls.isEmpty())
    }

    @Test
    fun unmonitoredPlayerSendsNothing() {
        val t = FakeTarget(packageName = "com.example.other", dislikeId = dislikeId)
        feedWith(t, dislikeMix, "Summer Mix 2024")
        assertTrue(t.calls.isEmpty())
    }

    @Test
    fun repeatedMetadataSendsOneDislike() {
        val t = FakeTarget(dislikeId = dislikeId)
        repeat(3) { feedWith(t, dislikeMix, "Summer Mix 2024") }
        assertEquals(listOf("dislike:$dislikeId", "skip"), t.calls)
    }

    @Test
    fun loopProtectionPauseSendsNoMoreDislikes() {
        val t = FakeTarget(dislikeId = dislikeId)
        repeat(SkipCoordinator.LOOP_MAX_SKIPS) { i ->
            clock += 1000
            feedWith(t, dislikeMix, "Mix $i")
        }
        val sent = t.calls.count { it.startsWith("dislike") }
        assertEquals(SkipCoordinator.LOOP_MAX_SKIPS, sent)
        clock += 1000
        feedWith(t, dislikeMix, "Mix after pause")
        assertEquals(sent, t.calls.count { it.startsWith("dislike") })
    }

    @Test
    fun artistArrivingLaterDoesNotSendASecondDislikeForTheSameTrack() {
        val t = FakeTarget(dislikeId = dislikeId)
        coordinator.onMetadata(t, "Summer Mix 2024", "", clock, true, dislikeMix)
        coordinator.onMetadata(t, "Summer Mix 2024", "DJ", clock, true, dislikeMix)
        assertEquals(1, t.calls.count { it.startsWith("dislike") })
    }

    @Test
    fun aDifferentTrackGetsItsOwnDislike() {
        val t = FakeTarget(dislikeId = dislikeId)
        feedWith(t, dislikeMix, "Summer Mix 2024")
        feedWith(t, dislikeMix, "Winter Mix 2024")
        assertEquals(2, t.calls.count { it.startsWith("dislike") })
    }

    @Test
    fun theSameTitleComingBackAfterAnotherTrackIsDislikedAgain() {
        val t = FakeTarget(dislikeId = dislikeId)
        feedWith(t, dislikeMix, "Summer Mix 2024")
        feedWith(t, dislikeMix, "Good Song")
        feedWith(t, dislikeMix, "Summer Mix 2024")
        assertEquals(2, t.calls.count { it.startsWith("dislike") })
    }

    // --- settle delay for dislike skips ---

    private class Delayed(val delayMs: Long, val task: () -> Unit)

    private val delayed = mutableListOf<Delayed>()
    private val delayedCoordinator = SkipCoordinator(
        { clock },
        { events += it },
        { paused += it },
        schedule = { ms, task -> delayed += Delayed(ms, task) },
    )

    private fun runDelayed() {
        while (delayed.isNotEmpty()) delayed.removeFirst().task()
    }

    private fun delayedFeed(t: FakeTarget, title: String, artist: String = "", s: AppSettings = dislikeMix) =
        delayedCoordinator.onMetadata(t, title, artist, clock, true, s)

    @Test
    fun dislikeSkipWaitsForTheSettleDelayBeforeActing() {
        val t = FakeTarget(dislikeId = dislikeId)
        delayedFeed(t, "Summer Mix 2024")
        assertTrue(t.calls.isEmpty())
        assertEquals(SkipCoordinator.DISLIKE_SETTLE_MS, delayed.single().delayMs)

        clock += SkipCoordinator.DISLIKE_SETTLE_MS
        runDelayed()
        assertEquals(listOf("dislike:$dislikeId", "skip"), t.calls)
        assertEquals(SkipCoordinator.DISLIKE_SETTLE_MS, events.single().latencyMs)
    }

    @Test
    fun playerThatMovesOnByItselfAfterTheDislikeIsNotSkippedAgain() {
        val t = FakeTarget(dislikeId = dislikeId)
        delayedFeed(t, "Summer Mix 2024")
        delayed.removeFirst().task() // settle: sends the dislike and schedules the check
        assertEquals(listOf("dislike:$dislikeId"), t.calls)
        assertEquals(SkipCoordinator.DISLIKE_ADVANCE_CHECK_MS, delayed.single().delayMs)

        delayedFeed(t, "Good Song") // the player moved on by itself
        delayed.removeFirst().task()
        assertEquals(listOf("dislike:$dislikeId"), t.calls)
        val e = events.single()
        assertTrue(e.disliked)
        assertEquals("Summer Mix 2024", e.title)
    }

    @Test
    fun playerThatStaysOnTheTrackAfterTheDislikeIsSkipped() {
        val t = FakeTarget(dislikeId = dislikeId)
        delayedFeed(t, "Summer Mix 2024")
        delayed.removeFirst().task()
        delayed.removeFirst().task()
        assertEquals(listOf("dislike:$dislikeId", "skip"), t.calls)
    }

    @Test
    fun markedRuleOnAPlayerWithoutADislikeActionSkipsAtOnce() {
        val t = FakeTarget(dislikeId = null)
        delayedFeed(t, "Summer Mix 2024")
        assertEquals(listOf("skip"), t.calls)
        assertTrue(delayed.isEmpty())
        assertFalse(events.single().disliked)
    }

    @Test
    fun skipOnlyRulesAreNotDelayed() {
        val t = FakeTarget(dislikeId = dislikeId)
        delayedFeed(t, "合輯 Vol.1")
        assertEquals(listOf("skip"), t.calls)
        assertTrue(delayed.isEmpty())
    }

    @Test
    fun artistArrivingDuringTheDelayDoesNotScheduleASecondSkip() {
        val t = FakeTarget(dislikeId = dislikeId)
        delayedFeed(t, "Summer Mix 2024")
        delayedFeed(t, "Summer Mix 2024", "DJ")
        assertEquals(1, delayed.size)
        runDelayed()
        assertEquals(listOf("dislike:$dislikeId", "skip"), t.calls)
        assertEquals("DJ", events.single().artist)
    }

    @Test
    fun aTrackChangeDuringTheDelayCancelsThePendingSkip() {
        val t = FakeTarget(dislikeId = dislikeId)
        delayedFeed(t, "Summer Mix 2024")
        delayedFeed(t, "Good Song")
        delayed.forEach { it.task() }
        assertTrue(t.calls.isEmpty())
    }

    // --- state changes during the settle delay ---

    private var stillActive = true
    private var latestSettings: AppSettings? = null
    private val guardedCoordinator = SkipCoordinator(
        { clock },
        { events += it },
        { paused += it },
        schedule = { ms, task -> delayed += Delayed(ms, task) },
        isActive = { stillActive },
        currentSettings = { latestSettings },
    )

    private fun guardedFeed(t: FakeTarget, title: String) =
        guardedCoordinator.onMetadata(t, title, "", clock, true, dislikeMix)

    @Test
    fun turningTheMasterSwitchOffDuringTheDelayCancelsTheDislikeAndTheSkip() {
        val t = FakeTarget(dislikeId = dislikeId)
        guardedFeed(t, "Summer Mix 2024")
        stillActive = false
        runDelayed()
        assertTrue(t.calls.isEmpty())
        assertTrue(events.isEmpty())
    }

    @Test
    fun removingTheRuleDuringTheDelayCancelsTheDislikeAndTheSkip() {
        val t = FakeTarget(dislikeId = dislikeId)
        guardedFeed(t, "Summer Mix 2024")
        latestSettings = AppSettings(keywords = emptyList(), dislikeRules = setOf("keyword:Mix"))
        runDelayed()
        assertTrue(t.calls.isEmpty())
    }

    @Test
    fun uncheckingTheDislikeOptionDuringTheDelayStillSkipsButDoesNotDislike() {
        val t = FakeTarget(dislikeId = dislikeId)
        guardedFeed(t, "Summer Mix 2024")
        latestSettings = AppSettings(dislikeRules = emptySet())
        runDelayed()
        assertEquals(listOf("skip"), t.calls)
        assertFalse(events.single().disliked)
    }

    @Test
    fun removingThePlayerDuringTheDelayCancelsEverything() {
        val t = FakeTarget(dislikeId = dislikeId)
        guardedFeed(t, "Summer Mix 2024")
        latestSettings = AppSettings(monitoredPackages = emptyList(), dislikeRules = setOf("keyword:Mix"))
        runDelayed()
        assertTrue(t.calls.isEmpty())
    }

    @Test
    fun aTrackAlreadyDislikedOrWithoutPublishedRatingIsSkippedAtOnce() {
        for (rating in listOf(RatingState.DISLIKED, RatingState.UNKNOWN)) {
            delayed.clear()
            val t = FakeTarget(dislikeId = dislikeId, rating = rating)
            delayedFeed(t, "Summer Mix 2024 $rating")
            assertEquals(listOf("skip"), t.calls, "rating=$rating")
            assertTrue(delayed.isEmpty(), "rating=$rating")
        }
    }

    // --- dislike through the rating API ---

    @Test
    fun ratingOnlyPlayerGetsAThumbsDownThenASkip() {
        val t = FakeTarget(dislikeId = null, ratingSupported = true)
        feedWith(t, dislikeMix, "Summer Mix 2024")
        assertEquals(listOf("rate:down", "skip"), t.calls)
        assertTrue(events.single().disliked)
    }

    @Test
    fun ratingPathIsNotBlockedByAnUnpublishedRating() {
        val t = FakeTarget(dislikeId = null, ratingSupported = true, rating = RatingState.UNKNOWN)
        feedWith(t, dislikeMix, "Summer Mix 2024")
        assertEquals(listOf("rate:down", "skip"), t.calls)
    }

    @Test
    fun ratingPathSendsEvenWhenTheRatingLooksLikeADislike() {
        val t = FakeTarget(dislikeId = null, ratingSupported = true, rating = RatingState.DISLIKED)
        feedWith(t, dislikeMix, "Summer Mix 2024")
        assertEquals(listOf("rate:down", "skip"), t.calls)
    }

    @Test
    fun theCustomActionWinsOverRating() {
        val t = FakeTarget(dislikeId = dislikeId, ratingSupported = true)
        feedWith(t, dislikeMix, "Summer Mix 2024")
        assertEquals(listOf("dislike:$dislikeId", "skip"), t.calls)
    }

    @Test
    fun noActionAndNoRatingSupportOnlySkips() {
        val t = FakeTarget(dislikeId = null, ratingSupported = false)
        feedWith(t, dislikeMix, "Summer Mix 2024")
        assertEquals(listOf("skip"), t.calls)
        assertFalse(events.single().disliked)
    }

    @Test
    fun unmarkedRuleOnARatingPlayerOnlySkips() {
        val t = FakeTarget(dislikeId = null, ratingSupported = true)
        feedWith(t, dislikeMix, "合輯 Vol.1")
        assertEquals(listOf("skip"), t.calls)
    }

    @Test
    fun throwingRatingStillSkips() {
        val t = FakeTarget(dislikeId = null, ratingSupported = true, ratingThrows = true)
        feedWith(t, dislikeMix, "Summer Mix 2024")
        assertEquals(listOf("skip"), t.calls)
        assertFalse(events.single().disliked)
    }

    @Test
    fun ratingIsSentOncePerTrackButTheSkipIsRetriedWhenTheTrackShowsAgain() {
        val t = FakeTarget(dislikeId = null, ratingSupported = true)
        coordinator.onMetadata(t, "Summer Mix 2024", "", clock, true, dislikeMix)
        coordinator.onMetadata(t, "Summer Mix 2024", "DJ", clock, true, dislikeMix)
        assertEquals(listOf("rate:down", "skip", "skip"), t.calls)
        assertEquals(1, events.size)
    }

    @Test
    fun ratingPathWaitsOnlyItsOwnDelayAndHasNoCheckDelay() {
        val t = FakeTarget(dislikeId = null, ratingSupported = true)
        delayedFeed(t, "Summer Mix 2024")
        assertTrue(t.calls.isEmpty())
        assertEquals(SkipCoordinator.RATING_SETTLE_MS, delayed.single().delayMs)
        delayed.removeFirst().task()
        assertEquals(listOf("rate:down", "skip"), t.calls)
        assertTrue(delayed.isEmpty())
    }

    @Test
    fun ratingPathStopsAfterLoopProtectionPauses() {
        val t = FakeTarget(dislikeId = null, ratingSupported = true)
        repeat(SkipCoordinator.LOOP_MAX_SKIPS) { i ->
            clock += 1000
            feedWith(t, dislikeMix, "Mix $i")
        }
        val sent = t.calls.count { it.startsWith("rate") }
        assertEquals(SkipCoordinator.LOOP_MAX_SKIPS, sent)
        clock += 1000
        feedWith(t, dislikeMix, "Mix after pause")
        assertEquals(sent, t.calls.count { it.startsWith("rate") })
    }

    @Test
    fun masterSwitchOffSendsNoRating() {
        val t = FakeTarget(dislikeId = null, ratingSupported = true)
        feedWith(t, dislikeMix, "Summer Mix 2024", active = false)
        assertTrue(t.calls.isEmpty())
    }

    // --- rating dislike with a settle delay ---

    private var ratingActive = true
    private var ratingLatest: AppSettings? = null
    private val waitingCoordinator = SkipCoordinator(
        { clock },
        { events += it },
        { paused += it },
        schedule = { ms, task -> delayed += Delayed(ms, task) },
        isActive = { ratingActive },
        currentSettings = { ratingLatest },
        ratingSettleMs = 500,
    )

    private fun waitingFeed(t: FakeTarget, title: String) =
        waitingCoordinator.onMetadata(t, title, "", clock, true, dislikeMix)

    @Test
    fun ratingWaitsForItsSettleDelayThenRatesAndSkipsWithoutACheckDelay() {
        val t = FakeTarget(dislikeId = null, ratingSupported = true)
        waitingFeed(t, "Summer Mix 2024")
        assertTrue(t.calls.isEmpty())
        assertEquals(500L, delayed.single().delayMs)
        runDelayed()
        assertEquals(listOf("rate:down", "skip"), t.calls)
        assertTrue(delayed.isEmpty())
    }

    @Test
    fun switchOffRuleRemovalOrTrackChangeDuringTheRatingDelayCancelsEverything() {
        val cases = listOf<(FakeTarget) -> Unit>(
            { ratingActive = false },
            { ratingLatest = AppSettings(keywords = emptyList(), dislikeRules = setOf("keyword:Mix")) },
            { ratingLatest = AppSettings(monitoredPackages = emptyList(), dislikeRules = setOf("keyword:Mix")) },
            { t -> waitingFeed(t, "Good Song") },
        )
        for ((i, change) in cases.withIndex()) {
            delayed.clear()
            ratingActive = true
            ratingLatest = null
            val t = FakeTarget(dislikeId = null, ratingSupported = true)
            waitingFeed(t, "Summer Mix 2024 #$i")
            change(t)
            runDelayed()
            assertTrue(t.calls.isEmpty(), "case $i: ${t.calls}")
        }
    }
}
