package fakeplayer

import android.app.Activity
import android.content.Intent
import android.media.MediaMetadata
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
            })
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
        if (intent.getBooleanExtra("advance", false)) advance("intent")
    }

    private fun advance(cause: String) {
        index = (index + 1) % playlist.size
        val (title, artist) = playlist[index]
        Log.i("FakePlayer", "now playing \"$title\" ($cause)")
        session.setMetadata(
            MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, title)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, artist)
                .build(),
        )
        session.setPlaybackState(
            PlaybackState.Builder()
                .setActions(PlaybackState.ACTION_PLAY_PAUSE or PlaybackState.ACTION_SKIP_TO_NEXT)
                .setState(PlaybackState.STATE_PLAYING, 0, 1f)
                .build(),
        )
        label.text = "Now playing: $title — $artist"
    }

    override fun onDestroy() {
        session.release()
        super.onDestroy()
    }
}
