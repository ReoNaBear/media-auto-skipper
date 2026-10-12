package com.mediaautoskipper.monitor

import com.mediaautoskipper.filter.TextFilterEngine
import com.mediaautoskipper.settings.AppSettings

/** The player session a track change came from. Production wraps a MediaController; tests use a fake. */
interface SkipTarget {
    val packageName: String
    fun skipToNext()

    /** Sends KEYCODE_MEDIA_NEXT down and up to this session only. */
    fun sendNextKey()
}

data class TrackIdentity(val pkg: String, val title: String, val artist: String)

data class SkipEvent(
    val timeMillis: Long,
    val pkg: String,
    val title: String,
    val artist: String,
    val reason: String,
    val usedFallback: Boolean,
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
) {
    private class State {
        var last: TrackIdentity? = null
        val skipTimes = ArrayDeque<Long>()
        var paused = false
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

        val fallback = !trySkipToNext(target)
        if (fallback && !trySendKey(target)) {
            log("skip failed pkg=$pkg title=\"$t\"")
            return
        }
        val issuedAt = nowMs()
        val latency = issuedAt - receivedAtMs
        log("skipped pkg=$pkg reason=${match.reason} fallback=$fallback latencyMs=$latency")
        onSkipped(SkipEvent(issuedAt, pkg, t, a, match.reason.orEmpty(), fallback, latency))

        while (state.skipTimes.isNotEmpty() && issuedAt - state.skipTimes.first() >= LOOP_WINDOW_MS) {
            state.skipTimes.removeFirst()
        }
        state.skipTimes.addLast(issuedAt)
        if (state.skipTimes.size >= LOOP_MAX_SKIPS) {
            state.paused = true
            onLoopPaused(pkg)
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
        const val LOOP_MAX_SKIPS = 10
        const val LOOP_WINDOW_MS = 60_000L
    }
}
