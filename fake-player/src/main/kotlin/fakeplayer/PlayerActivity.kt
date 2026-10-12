package fakeplayer

import android.app.Activity
import android.content.Intent
import android.media.MediaMetadata
import android.media.Rating
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Test-only player. Holds a MediaSession with a fixed playlist. skipToNext (or the "Next" button,
 * or `--ez advance true` on the launch intent) moves to the next track and publishes its metadata.
 * It also publishes a "thumbs_down_action" custom action and a thumbs up/down user rating, like
 * YouTube Music does; the action toggles the rating, so a second press undoes the dislike.
 * With `--ez ratingOnly true` it publishes no custom action and only accepts `setRating`, like the YouTube
 * video app: a thumbs-down rating sets the dislike, a thumbs-up or unrated rating clears it.
 */
class PlayerActivity : Activity() {
    private val playlist = listOf(
        "Yesterday" to "The Beatles",
        "华语精选" to "Various",
        "Hello" to "Adele",
        "爱你" to "Someone",
        "合輯 Vol.1" to "Various",
        "Summer Mix 2024" to "DJ",
        "Good Song" to "Fine Artist",
    )
    private var index = -1
    private val disliked = mutableSetOf<Int>()
    private var ratingOnly = false
    private lateinit var session: MediaSession
    private lateinit var label: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        label = TextView(this).apply { textSize = 18f }
        val next = Button(this).apply {
            text = "Next (natural track change)"
            setOnClickListener { advance("button") }
        }
        setContentView(
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(32, 64, 32, 32)
                addView(label)
                addView(next)
            },
        )

        session = MediaSession(this, "FakePlayer").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onSkipToNext() = advance("skipToNext from controller")

                override fun onSetRating(rating: Rating) {
                    if (rating.ratingStyle != Rating.RATING_THUMB_UP_DOWN) return
                    val down = rating.isRated && !rating.isThumbUp
                    if (down) disliked.add(index) else disliked.remove(index)
                    Log.i("FakePlayer", "setRating: track $index disliked=$down")
                    publish()
                }

                override fun onCustomAction(action: String, extras: Bundle?) {
                    if (action != DISLIKE_ACTION) return
                    if (!disliked.add(index)) disliked.remove(index)
                    Log.i("FakePlayer", "dislike action: track $index disliked=${index in disliked}")
                    publish()
                }
            })
            setRatingType(Rating.RATING_THUMB_UP_DOWN)
            isActive = true
        }
        advance("start")
        handle(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    private fun handle(intent: Intent) {
        if (intent.hasExtra("ratingOnly")) {
            ratingOnly = intent.getBooleanExtra("ratingOnly", false)
            Log.i("FakePlayer", "ratingOnly=$ratingOnly")
            publish()
        }
        if (intent.getBooleanExtra("advance", false)) advance("intent")
    }

    private fun advance(cause: String) {
        index = (index + 1) % playlist.size
        val (title, artist) = playlist[index]
        Log.i("FakePlayer", "now playing \"$title\" ($cause)")
        publish()
    }

    private fun publish() {
        val (title, artist) = playlist[index]
        val rating = if (index in disliked) Rating.newThumbRating(false) else Rating.newUnratedRating(Rating.RATING_THUMB_UP_DOWN)
        session.setMetadata(
            MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, title)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, artist)
                .putRating(MediaMetadata.METADATA_KEY_USER_RATING, rating)
                .build(),
        )
        session.setPlaybackState(
            PlaybackState.Builder()
                .apply {
                    if (!ratingOnly) addCustomAction(DISLIKE_ACTION, "Dislike", android.R.drawable.ic_delete)
                    setActions(
                        PlaybackState.ACTION_PLAY_PAUSE or PlaybackState.ACTION_SKIP_TO_NEXT or PlaybackState.ACTION_SET_RATING,
                    )
                }
                .setState(PlaybackState.STATE_PLAYING, 0, 1f)
                .build(),
        )
        label.text = "Now playing: $title — $artist" + if (index in disliked) " (disliked)" else ""
    }

    override fun onDestroy() {
        session.release()
        super.onDestroy()
    }

    private companion object {
        const val DISLIKE_ACTION = "thumbs_down_action"
    }
}
