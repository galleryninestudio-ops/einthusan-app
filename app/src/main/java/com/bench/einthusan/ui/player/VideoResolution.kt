package com.bench.einthusan.ui.player

/** Labels decoded dimensions only; catalogue quality flags are not resolution measurements. */
internal fun videoResolution(width: Int, height: Int): String? {
    if (width <= 0 || height <= 0) return null
    val dimensions = "$width × $height"
    return when {
        width >= 3840 || height >= 2160 -> "4K · $dimensions"
        height == 1080 -> "1080p · $dimensions"
        height == 720 -> "720p · $dimensions"
        else -> dimensions
    }
}
