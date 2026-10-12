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

    /** True when the player advertises setting a thumbs up/down rating (not stars or hearts). */
    fun supportsRating(): Boolean = false

    /** Sets a thumbs rating through the media controller. Setting it again is harmless; it does not toggle. */
    fun sendRating(thumbUp: Boolean) {}
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
    /** Whether the master switch is effective right now; checked again when a delayed skip runs. */
    private val isActive: () -> Boolean = { true },
    /** The settings in force right now, or null to keep using the ones passed to [onMetadata]. */
    private val currentSettings: () -> AppSettings? = { null },
    /** Wait before a rating dislike; a parameter so tests can exercise the delayed path. */
    private val ratingSettleMs: Long = RATING_SETTLE_MS,
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
        val method = if (wantsDislike && t.isNotEmpty()) {
            DislikeSupport.chooseMethod(target.dislikeActionId(), target.supportsRating())
        } else {
            DislikeMethod.NONE
        }
        when {
            // Wait only when a custom-action dislike can actually be sent: the rating is not already a dislike.
            // Players ignore a dislike sent while the track is still being set up, so wait a moment first.
            method == DislikeMethod.CUSTOM_ACTION && target.ratingState().allowsDislike ->
                afterSettle(DISLIKE_SETTLE_MS, target, state, t, receivedAtMs, settings) { artist, still, now ->
                    skipNow(target, state, t, artist, still, receivedAtMs, now)
                }

            // A rating is set, not toggled, so it needs no rating check and no look at whether the player moved on.
            method == DislikeMethod.RATING ->
                afterSettle(ratingSettleMs, target, state, t, receivedAtMs, settings) { artist, still, now ->
                    // The user may have unticked the dislike option during the wait: then only skip.
                    val key = still.reason?.let(DislikeRules::keyForReason)
                    if (key != null && key in now.dislikeRules) {
                        rateAndSkip(target, state, t, artist, still, receivedAtMs)
                    } else {
                        skipNow(target, state, t, artist, still, receivedAtMs, now)
                    }
                }

            // No dislike method, an unmarked rule, or a track that is already disliked: a plain, immediate skip.
            else -> skipNow(target, state, t, a, match, receivedAtMs, settings)
        }
    }

    /**
     * Runs [act] after [delayMs], unless the track changed in the meantime. It first checks again that the
     * master switch is on, that the player is still monitored and that the rule still matches, using the
     * settings in force by then, because the user may have changed them during the wait.
     */
    private fun afterSettle(
        delayMs: Long,
        target: SkipTarget,
        state: State,
        t: String,
        receivedAtMs: Long,
        settings: AppSettings,
        act: (artist: String, match: MatchResult, settings: AppSettings) -> Unit,
    ) {
        if (state.pendingTitle == t) return
        state.pendingTitle = t
        schedule(delayMs) {
            if (state.pendingTitle != t) return@schedule
            state.pendingTitle = null
            val current = state.last
            if (current == null || current.title != t) return@schedule
            if (!isActive()) return@schedule
            val now = currentSettings() ?: settings
            if (target.packageName !in now.monitoredPackages) return@schedule
            val still = TextFilterEngine.evaluate(t, current.artist, now.filterConfig)
            if (!still.matched) return@schedule
            act(current.artist, still, now)
        }
    }

    /** Sets a thumbs-down rating once per track, then skips right away. */
    private fun rateAndSkip(
        target: SkipTarget,
        state: State,
        t: String,
        a: String,
        match: MatchResult,
        receivedAtMs: Long,
    ) {
        if (t == state.dislikedTitle) {
            // The same track again (its artist arrived, or the first skip was ignored): do not rate twice,
            // but try the skip again so the track is not left playing.
            log("rating already sent for this track, retrying the skip pkg=${target.packageName}")
            trySkipToNext(target)
            return
        }
        val disliked = try {
            target.sendRating(thumbUp = false)
            log("dislike sent via rating pkg=${target.packageName}")
            state.dislikedTitle = t
            true
        } catch (e: Exception) {
            log("sendRating threw ${e.javaClass.simpleName}: ${e.message}")
            false
        }
        finishSkip(target, state, t, a, match, receivedAtMs, disliked = disliked, skipNeeded = true)
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

        /**
         * How long a rating dislike waits after the track's first metadata. On the YouTube video app (emulator,
         * one session) a rating sent at once was not applied and the skip was ignored; at 0.8 s the skip was
         * still ignored; at 1.5 s the rating was applied and the skip worked. Tuned on one emulator, so a
         * faster phone may allow less.
         */
        const val RATING_SETTLE_MS = 1500L

        /** How long to wait after a dislike to see whether the player moved to the next track by itself (it did so within 0.2 s on YouTube Music). */
        const val DISLIKE_ADVANCE_CHECK_MS = 600L
    }
}
