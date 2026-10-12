package com.mediaautoskipper.monitor

import com.mediaautoskipper.filter.MatchResult
import com.mediaautoskipper.filter.TextFilterEngine
import com.mediaautoskipper.settings.AppSettings
import com.mediaautoskipper.settings.DislikeRules

/** The player session a track change came from. Production wraps a MediaController; tests use a fake. */
interface SkipTarget {
    val packageName: String
    fun skipToNext()

    /** Sends KEYCODE_MEDIA_NEXT down and up to this session only. */
    fun sendNextKey()

    /** Id of the dislike custom action the player published, or null when it publishes none. */
    fun dislikeActionId(): String? = null

    /** The user's rating of the current track; [RatingState.UNKNOWN] when the player does not publish one. */
    fun ratingState(): RatingState = RatingState.UNKNOWN

    /** Sends the custom action [id], which must be an id the player itself published. */
    fun sendCustomAction(id: String) {}
}

data class TrackIdentity(val pkg: String, val title: String, val artist: String)

data class SkipEvent(
    val timeMillis: Long,
    val pkg: String,
    val title: String,
    val artist: String,
    val reason: String,
    val usedFallback: Boolean,
    /** True when the player's dislike action was sent just before the skip. */
    val disliked: Boolean = false,
    /** Milliseconds from metadata arrival to issuing the skip; the 0.5 s target is measured from this. */
    val latencyMs: Long,
)

/**
 * Decides, per metadata change, whether to skip. Holds the debounce and loop-protection state per
 * player package. Not thread-safe: call it from a single thread (the service main thread).
 */
class SkipCoordinator(
    private val nowMs: () -> Long,
    private val onSkipped: (SkipEvent) -> Unit,
    private val onLoopPaused: (pkg: String) -> Unit,
    private val log: (String) -> Unit = {},
    /** Runs [task] after the given delay on the thread that calls [onMetadata]. The default runs it at once. */
    private val schedule: (delayMs: Long, task: () -> Unit) -> Unit = { _, task -> task() },
) {
    private class State {
        var last: TrackIdentity? = null
        val skipTimes = ArrayDeque<Long>()
        var paused = false

        /**
         * Title of the track a dislike was last sent for. Players publish a track's metadata in pieces
         * (title first, artist a moment later), so the same track can reach the coordinator under
         * more than one [TrackIdentity]; this keeps it to one dislike per track.
         */
        var dislikedTitle: String? = null

        /** Title of a track whose skip is waiting for the dislike settle delay. */
        var pendingTitle: String? = null
    }

    private val states = mutableMapOf<String, State>()

    /**
     * @param active whether the master switch is currently effective
     * @param receivedAtMs [nowMs] reading taken when the metadata callback arrived
     */
    fun onMetadata(
        target: SkipTarget,
        title: String?,
        artist: String?,
        receivedAtMs: Long,
        active: Boolean,
        settings: AppSettings,
    ) {
        if (!active) return
        val pkg = target.packageName
        if (pkg !in settings.monitoredPackages) return

        val t = title.orEmpty()
        val a = artist.orEmpty()
        log("metadata pkg=$pkg title=\"$t\" artist=\"$a\"")

        val state = states.getOrPut(pkg) { State() }
        val identity = TrackIdentity(pkg, t, a)
        if (identity == state.last) return
        state.last = identity
        if (t.isNotEmpty() && t != state.dislikedTitle) state.dislikedTitle = null
        if (t.isNotEmpty() && t != state.pendingTitle) state.pendingTitle = null

        val match = TextFilterEngine.evaluate(t, a, settings.filterConfig)
        if (!match.matched) {
            state.skipTimes.clear()
            state.paused = false
            return
        }

        val now = nowMs()
        state.skipTimes.lastOrNull()?.let {
            if (now - it >= LOOP_WINDOW_MS) {
                state.skipTimes.clear()
                state.paused = false
            }
        }
        if (state.paused) {
            log("skip paused by loop protection pkg=$pkg")
            return
        }

        val wantsDislike = match.reason?.let(DislikeRules::keyForReason)?.let { it in settings.dislikeRules } == true
        // A player that publishes no dislike action (the YouTube video app) gets a plain, immediate skip.
        if (wantsDislike && t.isNotEmpty() && target.dislikeActionId() != null) {
            // Players ignore a rating sent while the track is still being set up, so wait a moment first.
            if (state.pendingTitle == t) return
            state.pendingTitle = t
            schedule(DISLIKE_SETTLE_MS) {
                if (state.pendingTitle != t) return@schedule
                state.pendingTitle = null
                val current = state.last
                if (current == null || current.title != t) return@schedule
                skipNow(target, state, t, current.artist, match, receivedAtMs, settings)
            }
            return
        }
        skipNow(target, state, t, a, match, receivedAtMs, settings)
    }

    private fun skipNow(
        target: SkipTarget,
        state: State,
        t: String,
        a: String,
        match: MatchResult,
        receivedAtMs: Long,
        settings: AppSettings,
    ) {
        // Dislike goes first: once the skip lands, the session's current track is already the next one.
        val disliked = tryDislike(target, state, t, match.reason, settings)
        if (!disliked) {
            finishSkip(target, state, t, a, match, receivedAtMs, disliked = false, skipNeeded = true)
            return
        }
        // Some players (YouTube Music) move to the next track by themselves once a track is disliked, so
        // skipping on top of that would drop a second track. Give the player a moment, then skip only if
        // it is still on the same track.
        schedule(DISLIKE_ADVANCE_CHECK_MS) {
            val current = state.last
            val advanced = current == null || current.title != t
            if (advanced) log("player moved on by itself after the dislike pkg=${target.packageName}")
            finishSkip(target, state, t, current?.artist ?: a, match, receivedAtMs, disliked = true, skipNeeded = !advanced)
        }
    }

    private fun finishSkip(
        target: SkipTarget,
        state: State,
        t: String,
        a: String,
        match: MatchResult,
        receivedAtMs: Long,
        disliked: Boolean,
        skipNeeded: Boolean,
    ) {
        val pkg = target.packageName
        var fallback = false
        if (skipNeeded) {
            fallback = !trySkipToNext(target)
            if (fallback && !trySendKey(target)) {
                log("skip failed pkg=$pkg title=\"$t\"")
                return
            }
        }
        val issuedAt = nowMs()
        val latency = issuedAt - receivedAtMs
        log("skipped pkg=$pkg reason=${match.reason} fallback=$fallback disliked=$disliked latencyMs=$latency")
        onSkipped(SkipEvent(issuedAt, pkg, t, a, match.reason.orEmpty(), fallback, disliked, latency))

        while (state.skipTimes.isNotEmpty() && issuedAt - state.skipTimes.first() >= LOOP_WINDOW_MS) {
            state.skipTimes.removeFirst()
        }
        state.skipTimes.addLast(issuedAt)
        if (state.skipTimes.size >= LOOP_MAX_SKIPS) {
            state.paused = true
            onLoopPaused(pkg)
        }
    }

    /** Sends the player's dislike when the matched rule asks for it and doing so cannot undo an existing dislike. */
    private fun tryDislike(target: SkipTarget, state: State, title: String, reason: String?, settings: AppSettings): Boolean {
        val key = reason?.let(DislikeRules::keyForReason) ?: return false
        if (key !in settings.dislikeRules) return false
        if (title.isNotEmpty() && title == state.dislikedTitle) {
            log("dislike skipped: already sent for this track pkg=${target.packageName}")
            return false
        }
        return try {
            val actionId = target.dislikeActionId()
            if (actionId == null) {
                log("dislike skipped: no dislike action pkg=${target.packageName}")
                return false
            }
            val rating = target.ratingState()
            if (!rating.allowsDislike) {
                log("dislike skipped: rating=$rating pkg=${target.packageName}")
                return false
            }
            target.sendCustomAction(actionId)
            log("dislike sent action=$actionId pkg=${target.packageName}")
            state.dislikedTitle = title.ifEmpty { null }
            true
        } catch (e: Exception) {
            log("dislike threw ${e.javaClass.simpleName}: ${e.message}")
            false
        }
    }

    private fun trySkipToNext(target: SkipTarget): Boolean = try {
        target.skipToNext()
        true
    } catch (e: Exception) {
        log("skipToNext threw ${e.javaClass.simpleName}: ${e.message}")
        false
    }

    private fun trySendKey(target: SkipTarget): Boolean = try {
        target.sendNextKey()
        true
    } catch (e: Exception) {
        log("sendNextKey threw ${e.javaClass.simpleName}: ${e.message}")
        false
    }

    companion object {
        // Design parameters, not measurements.
        const val LOOP_MAX_SKIPS = 20
        const val LOOP_WINDOW_MS = 60_000L

        /**
         * How long a skip that carries a dislike waits after the track's first metadata. On YouTube Music a
         * dislike sent 0.7 s after the track started was not reliably applied and its rating metadata was
         * stale, which risks toggling an existing dislike off; 2 s worked. Tuned on one emulator session.
         */
        const val DISLIKE_SETTLE_MS = 2000L

        /** How long to wait after a dislike to see whether the player moved to the next track by itself (it did so within 0.2 s on YouTube Music). */
        const val DISLIKE_ADVANCE_CHECK_MS = 600L
    }
}
