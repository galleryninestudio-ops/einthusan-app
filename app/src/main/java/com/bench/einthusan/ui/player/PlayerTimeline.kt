package com.bench.einthusan.ui.player

import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bench.einthusan.ui.theme.Glass
import com.bench.einthusan.ui.theme.GlassLineStrong
import com.bench.einthusan.ui.theme.TextHi

/** Hairline that thickens under a finger, with a time bubble that follows it. */
@Composable
internal fun Timeline(
    fraction: Float,
    bufferedFraction: Float,
    duration: Long,
    scrubbing: Boolean,
    targetLabel: String,
    onScrubStart: () -> Unit,
    onScrub: (Float) -> Unit,
    onScrubEnd: () -> Unit,
) {
    val enabled = duration > 0L
    var pressed by remember { mutableStateOf(false) }
    var widthPx by remember { mutableIntStateOf(0) }
    val active = pressed || scrubbing

    val trackH by androidx.compose.animation.core.animateDpAsState(
        if (active) 11.dp else 7.dp, tween(150), label = "track"
    )
    val thumbR by androidx.compose.animation.core.animateDpAsState(
        if (active) 15.dp else 10.dp, tween(150), label = "thumb"
    )

    Box(Modifier.fillMaxWidth().height(64.dp)) {
        Canvas(
            Modifier
                .fillMaxSize()
                .onSizeChanged { widthPx = it.width }
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            pressed = true
                            onScrubStart()
                            onScrub((offset.x / size.width.toFloat()).coerceIn(0f, 1f))
                        },
                        onDragEnd = { pressed = false; onScrubEnd() },
                        onDragCancel = { pressed = false; onScrubEnd() },
                        onHorizontalDrag = { change, _ ->
                            change.consume()
                            onScrub((change.position.x / size.width.toFloat()).coerceIn(0f, 1f))
                        },
                    )
                }
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectTapGestures { offset ->
                        val f = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        onScrubStart()
                        onScrub(f)
                        onScrubEnd()
                    }
                }
        ) {
            val midY = size.height - 18f
            val stroke = trackH.toPx()

            drawLine(
                color = Color(0x4DFFFFFF),
                start = Offset(0f, midY),
                end = Offset(size.width, midY),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
            val bx = size.width * bufferedFraction.coerceIn(0f, 1f)
            if (bx > 0f) {
                drawLine(
                    color = Color(0x59FFFFFF),
                    start = Offset(0f, midY),
                    end = Offset(bx, midY),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
            }
            val x = size.width * fraction.coerceIn(0f, 1f)
            if (x > 0f) {
                drawLine(
                    color = Color.White,
                    start = Offset(0f, midY),
                    end = Offset(x, midY),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
            }
            // Glass thumb: a soft white halo, then the solid core.
            drawCircle(
                color = Color(0x40FFFFFF),
                radius = thumbR.toPx() * 1.9f,
                center = Offset(x, midY),
            )
            drawCircle(color = Color.White, radius = thumbR.toPx(), center = Offset(x, midY))
        }

        if (active && widthPx > 0) {
            val x = widthPx * fraction.coerceIn(0f, 1f)
            val half = with(androidx.compose.ui.platform.LocalDensity.current) { 34.dp.toPx() }
            Text(
                targetLabel,
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
                color = TextHi,
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (x - half).toInt().coerceIn(0, (widthPx - half * 2).toInt()),
                            0,
                        )
                    }
                    .clip(RoundedCornerShape(10.dp))
                    .background(GlassDeep)
                    .border(1.dp, GlassLineStrong, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}
