package com.bench.einthusan.ui.player

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bench.einthusan.ui.BenchIcons
import com.bench.einthusan.ui.formatMs
import com.bench.einthusan.ui.pressScale
import com.bench.einthusan.ui.theme.Glass
import com.bench.einthusan.ui.theme.GlassLineStrong
import com.bench.einthusan.ui.theme.TextHi
import com.bench.einthusan.ui.theme.TextMid
import kotlinx.coroutines.delay

/* Only thing this screen adds to the shared glass: a panel dense enough to
   read over a bright frame. */
internal val GlassDeep = Color(0xE60B0C0F)

private sealed interface Hint {
    data class Bright(val value: Float) : Hint
    data class Volume(val value: Float) : Hint
    data class Seek(val forward: Boolean) : Hint
    data class Zoom(val scale: Float) : Hint
}

@Composable
internal fun PlayerChrome(
    title: String,
    subtitle: String,
    playing: Boolean,
    wantPlay: Boolean,
    buffering: Boolean,
    playbackError: String?,
    position: Long,
    buffered: Long,
    duration: Long,
    resizeMode: Int,
    speed: Float,
    qualityOptions: List<Pair<Boolean, String>>,
    ultraHd: Boolean,
    qualityDetail: String,
    onQuality: (Boolean) -> Unit,
    onResize: (Int) -> Unit,
    onSpeed: (Float) -> Unit,
    zoomScale: Float,
    onTransform: (Float, Offset, Offset, IntSize) -> Float,
    onTogglePlay: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSkip: (Boolean) -> Unit,
) {
    val currentZoom by rememberUpdatedState(zoomScale)
    val context = LocalContext.current
    val activity = context as? Activity
    val audio = remember { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }

    var visible by remember { mutableStateOf(true) }
    var locked by remember { mutableStateOf(false) }
    var sheet by remember { mutableStateOf(false) }
    var scrubbing by remember { mutableStateOf(false) }
    var scrubAt by remember { mutableFloatStateOf(0f) }
    var hint by remember { mutableStateOf<Hint?>(null) }
    var brightness by remember { mutableFloatStateOf(0.5f) }

    LaunchedEffect(visible, wantPlay, sheet, locked, scrubbing) {
        if (visible && wantPlay && !sheet && !locked && !scrubbing) {
            delay(3600)
            visible = false
        }
    }

    LaunchedEffect(Unit) {
        val w = activity?.window?.attributes?.screenBrightness ?: -1f
        brightness = if (w < 0f) 0.6f else w
    }

    LaunchedEffect(hint) {
        if (hint != null) {
            delay(950)
            hint = null
        }
    }

    fun applyBrightness(v: Float) {
        val clamped = v.coerceIn(0.02f, 1f)
        brightness = clamped
        activity?.let {
            val attrs = it.window.attributes
            attrs.screenBrightness = clamped
            it.window.attributes = attrs
        }
        hint = Hint.Bright(clamped)
    }

    fun applyVolume(delta: Float) {
        val am = audio ?: return
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val now = am.getStreamVolume(AudioManager.STREAM_MUSIC)
        val next = (now + delta * max).toInt().coerceIn(0, max)
        runCatching { am.setStreamVolume(AudioManager.STREAM_MUSIC, next, 0) }
        hint = Hint.Volume(next.toFloat() / max)
    }

    val shownPosition = if (scrubbing) (scrubAt * duration).toLong() else position
    val fraction = if (duration > 0L) shownPosition.toFloat() / duration else 0f

    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(locked, sheet) {
                if (locked || sheet) return@pointerInput
                detectVideoGestures(
                    onTap = { visible = !visible },
                    onDoubleTap = { forward ->
                        onSkip(forward)
                        hint = Hint.Seek(forward)
                    },
                    onVertical = { left, step ->
                        if (left) applyBrightness(brightness + step * 1.4f)
                        else applyVolume(step * 1.6f)
                    },
                    canPan = { currentZoom > 1f },
                    onTransform = { factor, pan, centroid, size ->
                        hint = Hint.Zoom(onTransform(factor, pan, centroid, size))
                    },
                )
            }
    ) {
        if (locked) {
            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) { detectTapGestures { } }
            )
            GlassIcon(
                icon = BenchIcons.Lock,
                label = "Unlock",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(16.dp),
            ) { locked = false; visible = true }
        }

        AnimatedVisibility(visible = visible && !locked, enter = fadeIn(tween(180)), exit = fadeOut(tween(180))) {
            Box(Modifier.fillMaxSize()) {

                // Nothing is boxed: a soft scrim for legibility, and the controls float on it.
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0f to Color(0x99000000),
                                0.26f to Color(0x00000000),
                                0.58f to Color(0x00000000),
                                1f to Color(0xB3000000),
                            )
                        )
                )

                Column(
                    Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(horizontal = 22.dp, vertical = 14.dp)
                ) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextHi,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.width(300.dp),
                    )
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMid,
                    )
                }

                Row(
                    Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GlassIcon(BenchIcons.Lock, "Lock") { locked = true; sheet = false }
                    Spacer(Modifier.width(8.dp))
                    GlassIcon(BenchIcons.Sliders, "Player settings") { sheet = !sheet }
                }

                Row(
                    Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(26.dp),
                ) {
                    SkipButton(forward = false) {
                        onSkip(false)
                        hint = Hint.Seek(false)
                    }

                    Box(
                        Modifier
                            .size(62.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .pressScale(onTogglePlay),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (buffering && wantPlay) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF0B0B0B),
                            )
                        } else {
                            PlayPauseGlyph(playing = wantPlay, tint = Color(0xFF0B0B0B))
                        }
                    }

                    SkipButton(forward = true) {
                        onSkip(true)
                        hint = Hint.Seek(true)
                    }
                }

                Column(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 12.dp)
                ) {
                    Timeline(
                        fraction = fraction,
                        bufferedFraction = if (duration > 0L) buffered.toFloat() / duration else 0f,
                        duration = duration,
                        scrubbing = scrubbing,
                        targetLabel = formatMs(shownPosition),
                        onScrubStart = {
                            scrubbing = true
                            scrubAt = fraction
                        },
                        onScrub = { scrubAt = it },
                        onScrubEnd = {
                            if (duration > 0L) onSeekTo((scrubAt * duration).toLong())
                            scrubbing = false
                        },
                    )
                    Row(
                        Modifier.fillMaxWidth().padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            formatMs(shownPosition),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextHi,
                        )
                        if (playbackError != null) {
                            Spacer(Modifier.width(10.dp))
                            Text(
                                playbackError,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextHi.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        if (speed != 1f) {
                            Text(
                                "${speed}x",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMid,
                                modifier = Modifier.padding(end = 10.dp),
                            )
                        }
                        Text(
                            if (duration > 0L) "-" + formatMs((duration - shownPosition).coerceAtLeast(0L))
                            else "--:--",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMid,
                        )
                    }
                }
            }
        }

        // HUD: lives outside the chrome so a swipe always answers, even when hidden.
        AnimatedVisibility(
            visible = hint is Hint.Bright,
            enter = fadeIn(tween(110)),
            exit = fadeOut(tween(180)),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 28.dp),
        ) {
            val h = hint
            if (h is Hint.Bright) LevelIndicator(volume = false, level = h.value)
        }

        AnimatedVisibility(
            visible = hint is Hint.Volume,
            enter = fadeIn(tween(110)),
            exit = fadeOut(tween(180)),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 28.dp),
        ) {
            val h = hint
            if (h is Hint.Volume) LevelIndicator(volume = true, level = h.value)
        }

        // The skip flash belongs under the button it came from, never mid-screen.
        AnimatedVisibility(
            visible = (hint as? Hint.Seek)?.forward == false,
            enter = fadeIn(tween(110)),
            exit = fadeOut(tween(220)),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 190.dp),
        ) {
            SeekChip(forward = false)
        }

        AnimatedVisibility(
            visible = (hint as? Hint.Seek)?.forward == true,
            enter = fadeIn(tween(110)),
            exit = fadeOut(tween(220)),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 190.dp),
        ) {
            SeekChip(forward = true)
        }

        AnimatedVisibility(
            visible = hint is Hint.Zoom,
            enter = fadeIn(tween(110)),
            exit = fadeOut(tween(220)),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 96.dp),
        ) {
            Text(
                text = String.format(java.util.Locale.US, "%.2f×", currentZoom),
                color = TextHi,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.clip(RoundedCornerShape(50)).background(GlassDeep)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }

        if (sheet && !locked) {
            // Tap anywhere off the panel and it goes away — no button needed.
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color(0x66000000))
                    .pointerInput(Unit) { detectTapGestures { sheet = false } }
            )
            PlayerSheet(
                resizeMode = resizeMode,
                speed = speed,
                qualityOptions = qualityOptions,
                ultraHd = ultraHd,
                qualityDetail = qualityDetail,
                onQuality = onQuality,
                onResize = onResize,
                onSpeed = onSpeed,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

@Composable
private fun GlassIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(Glass)
            .border(1.dp, GlassLineStrong, CircleShape)
            .pressScale(onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, label, tint = TextHi, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun SkipButton(forward: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(Color(0x14FFFFFF))
            .border(1.dp, GlassLineStrong, RoundedCornerShape(50))
            .pressScale(onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!forward) {
            ChevronGlyph(forward = false)
            Spacer(Modifier.width(6.dp))
        }
        Text(
            "10",
            style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 0.2.sp),
            color = TextHi,
        )
        if (forward) {
            Spacer(Modifier.width(6.dp))
            ChevronGlyph(forward = true)
        }
    }
}

@Composable
private fun ChevronGlyph(forward: Boolean) {
    Canvas(Modifier.size(12.dp)) {
        val p = Path().apply {
            if (forward) {
                moveTo(size.width * 0.3f, size.height * 0.12f)
                lineTo(size.width * 0.76f, size.height * 0.5f)
                lineTo(size.width * 0.3f, size.height * 0.88f)
            } else {
                moveTo(size.width * 0.7f, size.height * 0.12f)
                lineTo(size.width * 0.24f, size.height * 0.5f)
                lineTo(size.width * 0.7f, size.height * 0.88f)
            }
            close()
        }
        drawPath(p, Color.White)
    }
}

@Composable
private fun SeekChip(forward: Boolean) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(GlassDeep)
            .border(1.dp, GlassLineStrong, RoundedCornerShape(50))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!forward) {
            ChevronGlyph(forward = false)
            Spacer(Modifier.width(7.dp))
        }
        Text(
            "10s",
            style = MaterialTheme.typography.labelLarge,
            color = TextHi,
        )
        if (forward) {
            Spacer(Modifier.width(7.dp))
            ChevronGlyph(forward = true)
        }
    }
}

@Composable
private fun LevelIndicator(volume: Boolean, level: Float) {
    val fill by animateFloatAsState(
        targetValue = level.coerceIn(0f, 1f),
        animationSpec = tween(180),
        label = "level",
    )

    Column(
        Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(GlassDeep)
            .border(1.dp, GlassLineStrong, RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Canvas(Modifier.size(20.dp)) {
            if (volume) {
                drawRoundRect(
                    Color.White,
                    topLeft = Offset(size.width * 0.08f, size.height * 0.34f),
                    size = Size(size.width * 0.26f, size.height * 0.32f),
                    cornerRadius = CornerRadius(1.5f, 1.5f),
                )
                val p = Path().apply {
                    moveTo(size.width * 0.34f, size.height * 0.34f)
                    lineTo(size.width * 0.58f, size.height * 0.12f)
                    lineTo(size.width * 0.58f, size.height * 0.88f)
                    lineTo(size.width * 0.34f, size.height * 0.66f)
                    close()
                }
                drawPath(p, Color.White)
            } else {
                val c = Offset(size.width / 2f, size.height / 2f)
                drawCircle(Color.White, radius = size.width * 0.24f, center = c)
                for (i in 0 until 8) {
                    val a = Math.toRadians((i * 45).toDouble())
                    val inner = size.width * 0.36f
                    val outer = size.width * 0.50f
                    drawLine(
                        Color.White,
                        start = Offset(c.x + (inner * kotlin.math.cos(a)).toFloat(), c.y + (inner * kotlin.math.sin(a)).toFloat()),
                        end = Offset(c.x + (outer * kotlin.math.cos(a)).toFloat(), c.y + (outer * kotlin.math.sin(a)).toFloat()),
                        strokeWidth = 1.8f,
                        cap = StrokeCap.Round,
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Box(
            Modifier
                .width(5.dp)
                .height(96.dp)
                .clip(RoundedCornerShape(50))
                .background(Color(0x33FFFFFF))
        ) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxSize(fill)
                    .background(Color.White)
            )
        }
    }
}

@Composable
private fun PlayPauseGlyph(playing: Boolean, tint: Color = Color.White) {
    Canvas(Modifier.size(26.dp)) {
        if (playing) {
            val bar = size.width * 0.19f
            val gap = size.width * 0.14f
            drawRoundRect(
                color = tint,
                topLeft = Offset(size.width * 0.22f, size.height * 0.12f),
                size = Size(bar, size.height * 0.76f),
                cornerRadius = CornerRadius(bar / 2f, bar / 2f),
            )
            drawRoundRect(
                color = tint,
                topLeft = Offset(size.width * 0.22f + bar + gap, size.height * 0.12f),
                size = Size(bar, size.height * 0.76f),
                cornerRadius = CornerRadius(bar / 2f, bar / 2f),
            )
        } else {
            val path = Path().apply {
                moveTo(size.width * 0.34f, size.height * 0.1f)
                lineTo(size.width * 0.86f, size.height * 0.5f)
                lineTo(size.width * 0.34f, size.height * 0.9f)
                close()
            }
            drawPath(path, tint)
        }
    }
}

