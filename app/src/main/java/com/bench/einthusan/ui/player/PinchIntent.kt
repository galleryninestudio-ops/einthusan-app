package com.bench.einthusan.ui.player

import kotlin.math.abs
import kotlin.math.hypot

/** Ignore touch jitter and brief second-finger contacts before committing to a transform. */
internal class PinchIntent(
    private val startedAt: Long,
    private val initialRadius: Float,
    private val touchSlop: Float,
) {
    var zoom = 1f
        private set
    var panX = 0f
        private set
    var panY = 0f
        private set

    fun update(now: Long, factor: Float, dx: Float, dy: Float, canPan: Boolean): Boolean {
        if (!factor.isFinite() || factor <= 0 || !dx.isFinite() || !dy.isFinite()) return false
        zoom *= factor
        panX += dx
        panY += dy
        if (now - startedAt < 120L) return false
        val deliberateZoom = abs(zoom - 1f) >= 0.08f &&
            abs(zoom - 1f) * initialRadius >= touchSlop * 1.5f
        val deliberatePan = canPan && hypot(panX, panY) >= touchSlop * 1.5f
        return deliberateZoom || deliberatePan
    }
}
