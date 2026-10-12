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
) : SkipTarget {
    var skips = 0
    var keys = 0

    override fun skipToNext() {
        if (skipThrows) throw IllegalStateException("boom")
        skips++
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
        repeat(10) { i ->
            clock += 1000
            feed(t, "华语精选 $i")
        }
        assertEquals(10, t.skips)
        assertEquals(listOf(DefaultPlayers.SPOTIFY), paused)
        clock += 1000
        feed(t, "华语精选 11")
        assertEquals(10, t.skips)
    }

    @Test
    fun nonMatchingTrackResumesAfterPause() {
        val t = FakeTarget()
        repeat(10) { i ->
            clock += 1000
            feed(t, "华语精选 $i")
        }
        feed(t, "Yesterday")
        feed(t, "华语精选 again")
        assertEquals(11, t.skips)
    }

    @Test
    fun resumesAfterSixtySecondsWithoutSkip() {
        val t = FakeTarget()
        repeat(10) { i ->
            clock += 1000
            feed(t, "华语精选 $i")
        }
        clock += SkipCoordinator.LOOP_WINDOW_MS
        feed(t, "华语精选 later")
        assertEquals(11, t.skips)
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
}
