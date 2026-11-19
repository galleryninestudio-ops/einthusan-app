package com.bench.einthusan.data

import android.content.Context

/**
 * Player preferences. Small, flat, and every one of them changes something you
 * can see on screen: how the picture is scaled, how fast it runs, whether the
 * chrome stays out of the way.
 */
class Settings(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)

    /** 0 fit, 1 stretch, 2 crop, 3 fill — mirrors AspectRatioFrameLayout. */
    var resizeMode: Int
        get() = prefs.getInt("resize", 0)
        set(v) = prefs.edit().putInt("resize", v).apply()

    /** 0.5 - 2.0 */
    var speed: Float
        get() = prefs.getFloat("speed", 1f)
        set(v) = prefs.edit().putFloat("speed", v).apply()

    /** Bytes pulled ahead of a play. 0 turns preload off. */
    val preloadBytes: Long
        get() = prefs.getInt("preload_mb", 24).coerceIn(0, 200) * 1024L * 1024L
}
