package com.bench.einthusan.ui.player

import android.app.Activity
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Rect
import android.graphics.drawable.Icon
import android.os.Build
import android.util.Rational
import android.view.View
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.session.MediaSession
import java.lang.ref.WeakReference
import kotlin.math.abs

/** PiP transport, media-session integration and the visible-playback lifecycle. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
object Pip {
    private const val PLAY = "com.bench.einthusan.pip.PLAY"
    private const val PAUSE = "com.bench.einthusan.pip.PAUSE"
    private const val REWIND = "com.bench.einthusan.pip.REWIND"
    private const val FORWARD = "com.bench.einthusan.pip.FORWARD"
    private const val CLOSE = "com.bench.einthusan.pip.CLOSE"

    var inPip by mutableStateOf(false)
        private set

    private var activityRef = WeakReference<Activity>(null)
    private val activity: Activity? get() = activityRef.get()
    private var player: Player? = null
    private var mediaSession: MediaSession? = null
    private var saveProgress: (() -> Unit)? = null
    private var ratio = Rational(16, 9)
    private var sourceRect: Rect? = null
    private var policy = PipPlaybackPolicy()

    /** Used by late stream loading as well as system transport controls. */
    val canPlay: Boolean get() = policy.canPlay

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) = refresh()
        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) = refresh()
        override fun onPlaybackStateChanged(playbackState: Int) = refresh()
        override fun onVideoSizeChanged(videoSize: VideoSize) {
            ratio = ratioFor(videoSize.width, videoSize.height)
            refresh()
        }
    }

    fun bind(activity: Activity) {
        activityRef = WeakReference(activity)
        policy = PipPlaybackPolicy()
        inPip = activity.isInPictureInPictureMode
        refresh()
    }

    fun unbind(activity: Activity) {
        if (this.activity !== activity) return
        policy.close()
        player?.pause()
        mediaSession?.release()
        mediaSession = null
        player?.removeListener(listener)
        player = null
        saveProgress = null
        sourceRect = null
        activityRef.clear()
        inPip = false
    }

    fun attach(player: Player, saveProgress: () -> Unit) {
        if (this.player === player) return
        this.player?.pause()
        this.player?.removeListener(listener)
        mediaSession?.release()
        this.player = player
        this.saveProgress = saveProgress
        sourceRect = null
        ratio = ratioFor(player.videoSize.width, player.videoSize.height)
        player.addListener(listener)

        activity?.let { act ->
            // System PiP/media controls must target this player. Do not allow a
            // controller to restart a hidden activity after its PiP was dismissed.
            val visiblePlayer = object : ForwardingPlayer(player) {
                override fun play() {
                    if (Pip.canPlay) super.play()
                }
                override fun setPlayWhenReady(playWhenReady: Boolean) {
                    if (!playWhenReady || Pip.canPlay) super.setPlayWhenReady(playWhenReady)
                }
            }
            mediaSession = MediaSession.Builder(act.applicationContext, visiblePlayer)
                .setId("einthusan-player")
                .setSessionActivity(PendingIntent.getActivity(
                    act, 0, Intent(act, act.javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                ))
                .build()
        }
        refresh()
    }

    fun detach(player: Player) {
        if (this.player !== player) return
        mediaSession?.release()
        mediaSession = null
        player.removeListener(listener)
        this.player = null
        saveProgress = null
        sourceRect = null
        refresh()
    }

    fun onPipChanged(value: Boolean) {
        inPip = value
        refresh()
    }

    fun onActivityStarted() {
        policy.started()
        refresh()
    }

    fun onActivityStop() {
        // PiP is paused but visible. onStop means it is no longer visible, even
        // on devices that leave isInPictureInPictureMode true during dismissal.
        policy.stopped()
        saveProgress?.invoke()
        player?.pause() // Also clears playWhenReady while buffering.
        refresh()
    }

    fun onUserLeave(activity: Activity) {
        val p = player ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S || !supports(activity)) return
        if (policy.shouldAutoEnter(p.playWhenReady, p.playbackState)) {
            runCatching { activity.enterPictureInPictureMode(params()) }
        }
    }

    internal fun handleAction(action: String?) {
        val p = player ?: return
        when (action) {
            PLAY -> if (canPlay) {
                if (p.playbackState == Player.STATE_ENDED) p.seekTo(0L)
                if (p.playbackState == Player.STATE_IDLE) p.prepare()
                p.play()
            }
            PAUSE -> p.pause()
            REWIND -> p.seekTo((p.currentPosition - 10_000L).coerceAtLeast(0L))
            FORWARD -> {
                val end = p.duration.takeIf { it > 0L } ?: Long.MAX_VALUE
                p.seekTo((p.currentPosition + 10_000L).coerceIn(0L, end))
            }
            CLOSE -> {
                policy.close()
                saveProgress?.invoke()
                p.pause()
                p.stop()
                refresh()
                activity?.takeUnless { it.isFinishing || it.isDestroyed }?.finishAndRemoveTask()
            }
        }
        refresh()
    }

    fun updateSourceRect(view: View) {
        val next = Rect()
        if (view.getGlobalVisibleRect(next) && !next.isEmpty && next != sourceRect) {
            sourceRect = next
            refresh()
        }
    }

    private fun refresh() {
        val act = activity ?: return
        if (act.isDestroyed || !supports(act)) return
        runCatching { act.setPictureInPictureParams(params()) }
    }

    private fun params(): PictureInPictureParams {
        val act = activity
        val p = player
        val requested = p != null && policy.playbackRequested(p.playWhenReady, p.playbackState)
        val builder = PictureInPictureParams.Builder().setAspectRatio(ratio)
        sourceRect?.let(builder::setSourceRectHint)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(p != null && policy.shouldAutoEnter(p.playWhenReady, p.playbackState))
                .setSeamlessResizeEnabled(true)
        }
        if (act != null && p != null) {
            builder.setActions(listOf(
                action(act, REWIND, 1, android.R.drawable.ic_media_rew, "Back 10 seconds"),
                action(act, if (requested) PAUSE else PLAY, 2,
                    if (requested) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                    if (requested) "Pause" else "Play"),
                action(act, FORWARD, 3, android.R.drawable.ic_media_ff, "Forward 10 seconds"),
            ))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                builder.setCloseAction(action(act, CLOSE, 4, android.R.drawable.ic_menu_close_clear_cancel, "Close video"))
            }
        }
        return builder.build()
    }

    private fun action(act: Activity, command: String, requestCode: Int, icon: Int, label: String): RemoteAction {
        val intent = Intent(act, PipActionReceiver::class.java).setAction(command)
        val pending = PendingIntent.getBroadcast(act, requestCode, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return RemoteAction(Icon.createWithResource(act, icon), label, label, pending)
    }

    private fun supports(act: Activity): Boolean =
        act.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)

    private fun ratioFor(width: Int, height: Int): Rational {
        if (width <= 0 || height <= 0) return Rational(16, 9)
        val r = width.toFloat() / height
        return when {
            abs(r - 16f / 9f) < 0.02f -> Rational(16, 9)
            r < 0.42f -> Rational(42, 100)
            r > 2.39f -> Rational(239, 100)
            else -> Rational(width, height)
        }
    }
}
