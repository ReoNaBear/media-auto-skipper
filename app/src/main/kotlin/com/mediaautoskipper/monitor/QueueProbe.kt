package com.mediaautoskipper.monitor

import android.media.session.MediaController
import android.util.Log

/**
 * Debug-only dump of what a player publishes: queue, metadata and playback state. Used to decide
 * whether looking ahead at the next track is feasible; nothing here changes playback.
 */
object QueueProbe {
    private const val TAG = "MasProbe"

    fun dump(controller: MediaController) {
        val pkg = controller.packageName
        Log.i(TAG, "=== $pkg ===")
        val md = controller.metadata
        Log.i(
            TAG,
            "metadata: title=${md?.getString("android.media.metadata.TITLE")} " +
                "artist=${md?.getString("android.media.metadata.ARTIST")} " +
                "duration=${md?.getLong("android.media.metadata.DURATION")} " +
                "mediaId=${md?.getString("android.media.metadata.MEDIA_ID")}",
        )
        val ps = controller.playbackState
        Log.i(
            TAG,
            "playbackState: state=${ps?.state} position=${ps?.position} speed=${ps?.playbackSpeed} " +
                "actions=${ps?.actions?.let { "0x" + it.toString(16) }} " +
                "activeQueueItemId=${ps?.activeQueueItemId} " +
                "skipToNext=${ps != null && (ps.actions and android.media.session.PlaybackState.ACTION_SKIP_TO_NEXT) != 0L} " +
                "skipToQueueItem=${ps != null && (ps.actions and android.media.session.PlaybackState.ACTION_SKIP_TO_QUEUE_ITEM) != 0L}",
        )
        Log.i(
            TAG,
            "rating: ratingType=${controller.ratingType} " +
                "setRating=${ps != null && (ps.actions and android.media.session.PlaybackState.ACTION_SET_RATING) != 0L} " +
                "userRating=${md?.getRating("android.media.metadata.USER_RATING")} " +
                "overallRating=${md?.getRating("android.media.metadata.RATING")}",
        )
        val custom = ps?.customActions.orEmpty()
        Log.i(TAG, "customActions: ${custom.size}")
        custom.forEach { Log.i(TAG, "  action=${it.action} name=${it.name}") }
        val queue = controller.queue
        if (queue == null) {
            Log.i(TAG, "queue: null (not published)")
        } else {
            Log.i(TAG, "queue: ${queue.size} items")
            queue.forEachIndexed { i, item ->
                Log.i(TAG, "  [$i] id=${item.queueId} title=${item.description.title} subtitle=${item.description.subtitle}")
            }
        }
    }

    fun dumpAll(controllers: Collection<MediaController>) {
        if (controllers.isEmpty()) Log.i(TAG, "no active sessions")
        controllers.forEach(::dump)
    }
}
