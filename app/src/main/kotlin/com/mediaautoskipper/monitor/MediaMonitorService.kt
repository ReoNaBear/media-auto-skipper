package com.mediaautoskipper.monitor

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.media.MediaMetadata
import android.media.Rating
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.util.Log
import android.view.KeyEvent
import androidx.core.app.NotificationCompat
import com.mediaautoskipper.MasApp
import com.mediaautoskipper.R
import com.mediaautoskipper.history.SkipRecord
import com.mediaautoskipper.settings.AppSettings
import com.mediaautoskipper.settings.EnableState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

private const val TAG = "MediaAutoSkipper"
private const val CHANNEL_ID = "skip_paused"
private const val PROBE_ACTION = "com.mediaautoskipper.PROBE"

/**
 * Listens to active media sessions (never plays media) and hands each metadata change to
 * [SkipCoordinator]. Does nothing while the master switch is off or after a reboot.
 */
class MediaMonitorService : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val callbacks = mutableMapOf<MediaController, MediaController.Callback>()

    @Volatile private var settings = AppSettings()
    @Volatile private var enableState = EnableState(enabled = false, bootCount = -1)

    private var probeReceiver: BroadcastReceiver? = null
    private lateinit var sessionManager: MediaSessionManager
    private lateinit var coordinator: SkipCoordinator

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        attach(controllers.orEmpty())
    }

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        val app = application as MasApp
        coordinator = SkipCoordinator(
            nowMs = { SystemClock.elapsedRealtime() },
            onSkipped = { event ->
                scope.launch(Dispatchers.IO) {
                    app.historyDb.dao().record(
                        SkipRecord(
                            timeMillis = System.currentTimeMillis(),
                            pkg = event.pkg,
                            title = event.title,
                            artist = event.artist,
                            reason = event.reason,
                            disliked = event.disliked,
                        ),
                    )
                }
            },
            onLoopPaused = ::notifyPaused,
            log = { Log.i(TAG, it) },
            schedule = { delayMs, task -> handler.postDelayed(task, delayMs) },
        )
        app.settingsStore.settings.onEach { settings = it }.launchIn(scope)
        app.settingsStore.enableState.onEach { enableState = it }.launchIn(scope)
    }

    override fun onListenerConnected() {
        sessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        val component = ComponentName(this, MediaMonitorService::class.java)
        sessionManager.addOnActiveSessionsChangedListener(sessionsListener, component)
        attach(sessionManager.getActiveSessions(component))
        registerProbeReceiver(component)
        Log.i(TAG, "listener connected")
    }

    /** Debug builds only: `adb shell am broadcast -a com.mediaautoskipper.PROBE` dumps every active session (see `--es send`). */
    private fun registerProbeReceiver(component: ComponentName) {
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE == 0 || probeReceiver != null) return
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val sessions = sessionManager.getActiveSessions(component)
                // `--es send <action id>` sends that custom action to every active session, for timing experiments.
                intent.getStringExtra("send")?.let { id ->
                    sessions.forEach {
                        it.transportControls.sendCustomAction(id, null)
                        Log.i(TAG, "probe sent custom action $id to ${it.packageName}")
                    }
                }
                QueueProbe.dumpAll(sessions)
            }
        }
        registerReceiver(receiver, IntentFilter(PROBE_ACTION), Context.RECEIVER_EXPORTED)
        probeReceiver = receiver
    }

    override fun onListenerDisconnected() {
        detachAll()
        Log.i(TAG, "listener disconnected")
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        detachAll()
        scope.cancel()
        super.onDestroy()
    }

    private fun detachAll() {
        probeReceiver?.let { unregisterReceiver(it) }
        probeReceiver = null
        if (::sessionManager.isInitialized) sessionManager.removeOnActiveSessionsChangedListener(sessionsListener)
        callbacks.forEach { (controller, callback) -> controller.unregisterCallback(callback) }
        callbacks.clear()
    }

    /** Registers a callback on each controller not yet watched; drops callbacks of ended sessions. */
    private fun attach(controllers: List<MediaController>) {
        val live = controllers.map { it.sessionToken }.toSet()
        callbacks.keys.filter { it.sessionToken !in live }.forEach { gone ->
            callbacks.remove(gone)?.let { gone.unregisterCallback(it) }
        }
        for (controller in controllers) {
            if (callbacks.keys.any { it.sessionToken == controller.sessionToken }) continue
            val callback = object : MediaController.Callback() {
                override fun onMetadataChanged(metadata: MediaMetadata?) = handle(controller, metadata)
            }
            controller.registerCallback(callback)
            callbacks[controller] = callback
            handle(controller, controller.metadata)
        }
    }

    private fun handle(controller: MediaController, metadata: MediaMetadata?) {
        val receivedAt = SystemClock.elapsedRealtime()
        val bootCount = Settings.Global.getInt(contentResolver, Settings.Global.BOOT_COUNT, 0)
        coordinator.onMetadata(
            target = ControllerTarget(controller),
            title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE),
            artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST),
            receivedAtMs = receivedAt,
            active = enableState.isActive(bootCount),
            settings = settings,
        )
    }

    private fun notifyPaused(pkg: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, getString(R.string.notif_channel_paused), NotificationManager.IMPORTANCE_DEFAULT),
        )
        val n = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_pause)
            .setContentTitle(getString(R.string.notif_paused_title))
            .setContentText(getString(R.string.notif_paused_text, pkg))
            .setAutoCancel(true)
            .build()
        nm.notify(pkg.hashCode(), n)
    }
}

/** Skips through one specific session, so other players are never affected. */
private class ControllerTarget(private val controller: MediaController) : SkipTarget {
    override val packageName: String get() = controller.packageName

    override fun skipToNext() = controller.transportControls.skipToNext()

    override fun dislikeActionId(): String? = DislikeSupport.findActionId(
        controller.playbackState?.customActions.orEmpty().map { CustomActionInfo(it.action, it.name.toString()) },
    )

    override fun ratingState(): RatingState {
        val rating = controller.metadata?.getRating(MediaMetadata.METADATA_KEY_USER_RATING)
        val thumbs = rating != null && rating.ratingStyle == Rating.RATING_THUMB_UP_DOWN
        return RatingState.of(published = thumbs, rated = rating?.isRated == true, thumbUp = rating?.isThumbUp == true)
    }

    override fun sendCustomAction(id: String) = controller.transportControls.sendCustomAction(id, null)

    override fun sendNextKey() {
        controller.dispatchMediaButtonEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_NEXT))
        controller.dispatchMediaButtonEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_NEXT))
    }
}
