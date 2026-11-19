package com.bench.einthusan.ui.player

import android.view.LayoutInflater
import com.bench.einthusan.R
import androidx.compose.ui.draw.clipToBounds
import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.bench.einthusan.data.MediaCache
import com.bench.einthusan.data.Repo
import com.bench.einthusan.net.Cdn
import com.bench.einthusan.parse.Movie
import com.bench.einthusan.parse.WatchInfo
import com.bench.einthusan.ui.AppState
import com.bench.einthusan.ui.Nav
import com.bench.einthusan.ui.Pill
import com.bench.einthusan.ui.theme.TextHi
import com.bench.einthusan.ui.theme.TextMid
import com.bench.einthusan.ui.theme.langOf
import com.bench.einthusan.util.suspendResult
import kotlinx.coroutines.delay

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun PlayerScreen(app: AppState, nav: Nav, movie: Movie, local: String? = null) {
    val context = LocalContext.current
    val view = LocalView.current
    val activity = context as? Activity

    val langKey = remember(movie) {
        val probe = movie.lang.lowercase()
        if (probe.isNotBlank() && probe.all { it.isLetter() }) probe else app.library.lastLang
    }

    var info by remember { mutableStateOf<WatchInfo?>(null) }
    var playbackError by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var attempt by remember { mutableIntStateOf(0) }
    var retries by remember { mutableIntStateOf(0) }

    var requestedUhd by remember(movie.id) { mutableStateOf(app.premium && local == null) }
    var qualityMessage by remember { mutableStateOf<String?>(null) }
    var resolution by remember { mutableStateOf<String?>(null) }

    val startAt = remember { app.library.position(movie.id) }

    val player = remember {
        val sources = DefaultMediaSourceFactory(
            DefaultDataSource.Factory(context, MediaCache.source(context))
        )
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(15_000, 45_000, 600, 1_200)
            .build()
        ExoPlayer.Builder(context, sources).setLoadControl(loadControl).build().apply {
            setHandleAudioBecomingNoisy(true)
            repeatMode = Player.REPEAT_MODE_OFF
            addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    val cause = generateSequence(error.cause) { it.cause }.lastOrNull()
                    val message = cause?.message ?: error.message ?: "no detail"
                    android.util.Log.e("Einthusan", "player: " + error.errorCodeName + " " + message)
                    playbackError = error.errorCodeName + ": " + message

                    val failed = currentMediaItem?.localConfiguration?.uri?.toString()
                    if (retries < 2 && (message.contains("404") || message.contains("403"))) {
                        Cdn.markBad(Cdn.hostOf(failed))
                        retries++
                        attempt++
                    }
                }

                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_READY) playbackError = null
                }
            })
        }
    }

    // A film is always landscape here; whatever the phone was set to comes back on exit.
    DisposableEffect(player) {
        Pip.attach(player) {
            val at = player.currentPosition.coerceAtLeast(0L)
            app.library.setPosition(movie.id, at)
            if (player.duration > 0L) app.library.recordProgress(movie, at, player.duration)
        }
        val previous = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose {
            runCatching {
                val at = player.currentPosition
                app.library.setPosition(movie.id, at)
                if (player.duration > 0L) app.library.recordProgress(movie, at, player.duration)
            }
            Pip.detach(player)
            player.release()
            activity?.requestedOrientation =
                previous ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    DisposableEffect(Unit) {
        val window = activity?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        view.keepScreenOn = true

        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
            view.keepScreenOn = false
            window?.let {
                val attrs = it.attributes
                attrs.screenBrightness = android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                it.attributes = attrs
            }
        }
    }


    LaunchedEffect(movie.id, attempt, requestedUhd) {
        if (local != null) {
            info = WatchInfo(mp4 = local, hls = null)
            loading = false
            error = null
            return@LaunchedEffect
        }
        loading = true
        error = null
        qualityMessage = null
        val result = suspendResult {
            Repo.watch(movie.id, langKey, uhd = requestedUhd, fresh = attempt > 0)
        }.getOrNull()
        if (result == null || !result.playable) {
            if (info?.playable == true) {
                qualityMessage = "Could not change quality. Your current stream is still playing."
                loading = false
                return@LaunchedEffect
            }
        }
        if (requestedUhd && result?.playable == true && !result.premiumHd) {
            qualityMessage = "Premium HD is unavailable for this title or session. Try signing in again if your Premium is active."
            if (info?.playable == true) {
                loading = false
                return@LaunchedEffect
            }
        }
        info = result
        if (result == null) {
            error = "Could not reach the stream host."
        } else if (!result.playable) {
            error = "This title has no playable stream."
        }
        loading = false
    }

    val url = info?.mp4 ?: info?.hls

    LaunchedEffect(url) {
        if (url != null) {
            val resumeAt = if (player.currentMediaItem != null) player.currentPosition else startAt
            val resumePlaying = if (player.currentMediaItem != null) player.playWhenReady else true
            resolution = null
            player.setMediaItem(MediaItem.Builder().setUri(url)
                .setMediaMetadata(MediaMetadata.Builder().setTitle(movie.title).build()).build(),
                resumeAt.coerceAtLeast(0L))
            player.prepare()
            player.playWhenReady = resumePlaying && Pip.canPlay
        }
    }

    var playing by remember { mutableStateOf(false) }
    var wantPlay by remember { mutableStateOf(true) }
    var buffering by remember { mutableStateOf(false) }
    var position by remember { mutableLongStateOf(0L) }
    var buffered by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }

    var resizeMode by remember { mutableIntStateOf(app.settings.resizeMode) }
    var speed by remember { mutableFloatStateOf(app.settings.speed) }
    var transform by remember { mutableStateOf(VideoTransform()) }

    LaunchedEffect(speed) { player.setPlaybackSpeed(speed) }

    LaunchedEffect(player) {
        var tick = 0
        var stalledAt = 0L
        while (true) {
            val videoSize = player.videoSize
            videoResolution(videoSize.width, videoSize.height)?.let { resolution = it }
            playing = player.isPlaying
            wantPlay = player.playWhenReady
            buffering = player.playbackState == Player.STATE_BUFFERING
            position = player.currentPosition.coerceAtLeast(0L)
            buffered = player.bufferedPosition.coerceAtLeast(0L)
            val d = player.duration
            if (d > 0L) duration = d

            if (buffering && wantPlay && !playing) {
                if (stalledAt == 0L) stalledAt = System.currentTimeMillis()
                if (retries < 2 && System.currentTimeMillis() - stalledAt > 18_000L) {
                    val failed = player.currentMediaItem?.localConfiguration?.uri?.toString()
                    Cdn.markBad(Cdn.hostOf(failed))
                    retries++
                    stalledAt = 0L
                    attempt++
                }
            } else {
                stalledAt = 0L
            }
            if (++tick % 20 == 0) app.library.setPosition(movie.id, position)
            if (tick % 40 == 0 && duration > 0L) {
                app.library.recordProgress(movie, position, duration)
            }
            delay(250)
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black).clipToBounds()) {

        if (url != null) {
            AndroidView(
                factory = { ctx ->
                    (LayoutInflater.from(ctx).inflate(R.layout.player_view, null) as PlayerView).apply {
                        useController = false
                        setShutterBackgroundColor(android.graphics.Color.BLACK)
                        setKeepContentOnPlayerReset(true)
                        this.player = player
                        addOnLayoutChangeListener { video, _, _, _, _, _, _, _, _ ->
                            Pip.updateSourceRect(video)
                        }
                    }
                },
                update = { v ->
                    v.player = player
                    v.resizeMode = resizeMode
                },
                modifier = Modifier.fillMaxSize().graphicsLayer {
                    scaleX = if (Pip.inPip) 1f else transform.scale
                    scaleY = if (Pip.inPip) 1f else transform.scale
                    translationX = if (Pip.inPip) 0f else transform.offsetX
                    translationY = if (Pip.inPip) 0f else transform.offsetY
                },
            )
        }

        if (loading) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.align(Alignment.Center).size(36.dp),
            )
        }

        if (url == null && !loading) {
            Column(
                Modifier.align(Alignment.Center).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "Playback blocked",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextHi,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    error ?: "Unknown error.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMid,
                )
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Pill("Retry", selected = true) { attempt++ }
                    Pill("Back") { nav.pop() }
                }
            }
        }

        if (url != null && !Pip.inPip) {
            PlayerChrome(
                title = movie.title,
                subtitle = langOf(langKey).label,
                playing = playing,
                wantPlay = wantPlay,
                buffering = buffering,
                playbackError = playbackError,
                position = position,
                buffered = buffered,
                duration = duration,
                resizeMode = resizeMode,
                speed = speed,
                qualityOptions = if (local == null && app.email != null && app.premium) {
                    listOf(false to "Standard", true to "Premium HD")
                } else emptyList(),
                ultraHd = info?.premiumHd == true,
                qualityDetail = qualityMessage ?: if (local != null) {
                    "Downloaded video" + (resolution?.let { " · $it" } ?: "")
                } else if (loading) "Loading quality…" else {
                    (resolution ?: "Reading resolution…") +
                        if (!app.premium) " · Premium required for HD" else if (info?.premiumHd == true) " · Premium HD" else " · Standard"
                },
                onQuality = { uhd ->
                    if (!loading && (uhd != info?.premiumHd || uhd != requestedUhd)) {
                        requestedUhd = uhd
                        retries = 0
                        attempt++
                    }
                },
                zoomScale = transform.scale,
                onTransform = { factor, pan, centroid, size ->
                    transform = transform.change(factor, pan.x, pan.y, centroid.x, centroid.y,
                        size.width.toFloat(), size.height.toFloat())
                    transform.scale
                },
                onResize = {
                    transform = VideoTransform()
                    resizeMode = it
                    app.settings.resizeMode = it
                },
                onSpeed = {
                    speed = it
                    app.settings.speed = it
                },
                onTogglePlay = {
                    if (player.isPlaying) player.pause() else player.play()
                },
                onSeekTo = { player.seekTo(it) },
                onSkip = { forward ->
                    val base = player.currentPosition.coerceAtLeast(0L)
                    val end = player.duration.takeIf { it > 0L } ?: Long.MAX_VALUE
                    val target = (base + if (forward) 10_000L else -10_000L)
                        .coerceIn(0L, end)
                    player.seekTo(target)
                },
            )
        }
    }
}

