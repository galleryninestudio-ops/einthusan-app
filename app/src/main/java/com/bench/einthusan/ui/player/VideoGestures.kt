package com.bench.einthusan.ui.player

import android.os.SystemClock
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.unit.IntSize
import kotlin.math.abs

/** One recognizer owns taps, single-finger swipes and deliberate two-finger transforms. */
internal suspend fun PointerInputScope.detectVideoGestures(
    onTap: () -> Unit,
    onDoubleTap: (forward: Boolean) -> Unit,
    onVertical: (left: Boolean, fraction: Float) -> Unit,
    canPan: () -> Boolean,
    onTransform: (factor: Float, pan: Offset, centroid: Offset, size: IntSize) -> Unit,
) {
    var lastTapAt = 0L
    var lastTapForward = false
    awaitEachGesture {
        val first = awaitFirstDown()
        var previous = first.position
        var travel = Offset.Zero
        var dragging = false
        var moved = false
        var multiTouch = false
        var blocked = false
        var cancelled = false
        var multiStartedAt = 0L
        var pinch: PinchIntent? = null
        var transforming = false
        do {
            val event = awaitPointerEvent()
            if (event.changes.any { it.isConsumed }) {
                cancelled = true
                break
            }
            val pressed = event.changes.count { it.pressed }
            if (pressed >= 2 || multiTouch) {
                if (!multiTouch) multiStartedAt = event.changes.first().uptimeMillis
                multiTouch = true
                lastTapAt = 0L
                if (pressed > 2) blocked = true
                val stablePair = pressed == 2 && event.changes.count { it.pressed && it.previousPressed } == 2
                if (!blocked && stablePair) {
                    val zoom = event.calculateZoom()
                    val pan = event.calculatePan()
                    val centroid = event.calculateCentroid(false)
                    if (pinch == null) pinch = PinchIntent(multiStartedAt, event.calculateCentroidSize(false), viewConfiguration.touchSlop)
                    val intent = pinch
                    if (transforming) {
                        onTransform(zoom, pan, centroid, size)
                    } else if (intent.update(event.changes.first().uptimeMillis, zoom, pan.x, pan.y, canPan())) {
                        transforming = true
                        onTransform(intent.zoom, Offset(intent.panX, intent.panY), centroid, size)
                    }
                }
                // A stray second touch cancels tap/seek and suppresses the remaining finger's swipe.
                event.changes.forEach { it.consume() }
            } else {
                val change = event.changes.firstOrNull { it.id == first.id } ?: break
                if (change.pressed) {
                    val delta = change.position - previous
                    travel += delta
                    if (travel.getDistance() > viewConfiguration.touchSlop) moved = true
                    if (!dragging && abs(travel.y) > viewConfiguration.touchSlop &&
                        abs(travel.y) >= abs(travel.x)) dragging = true
                    if (dragging) {
                        lastTapAt = 0L
                        onVertical(first.position.x < size.width / 2f, -delta.y / size.height.coerceAtLeast(1))
                        change.consume()
                    }
                    previous = change.position
                }
            }
        } while (event.changes.any { it.pressed })
        if (!multiTouch && !moved && !cancelled) {
            val now = SystemClock.uptimeMillis()
            val forward = first.position.x > size.width / 2f
            if (now - lastTapAt < 420L && forward == lastTapForward) {
                lastTapAt = 0L
                onDoubleTap(forward)
            } else {
                lastTapAt = now
                lastTapForward = forward
                onTap()
            }
        }
    }
}
