package com.bench.einthusan.ui.player

/** Per-playback pinch transform. Offsets are pixels relative to the viewport centre. */
internal data class VideoTransform(
    val scale: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
) {
    fun change(
        factor: Float,
        panX: Float,
        panY: Float,
        centroidX: Float,
        centroidY: Float,
        width: Float,
        height: Float,
    ): VideoTransform {
        if (width <= 0 || height <= 0 || !factor.isFinite() || factor <= 0 ||
            !panX.isFinite() || !panY.isFinite() || !centroidX.isFinite() || !centroidY.isFinite()) return this
        val next = (scale * factor).coerceIn(1f, 4f)
        val ratio = next / scale
        val x = offsetX * ratio + (centroidX - width / 2) * (1 - ratio) + panX
        val y = offsetY * ratio + (centroidY - height / 2) * (1 - ratio) + panY
        val maxX = width * (next - 1) / 2
        val maxY = height * (next - 1) / 2
        return VideoTransform(next, x.coerceIn(-maxX, maxX), y.coerceIn(-maxY, maxY))
    }
}
