package com.bench.einthusan.ui.player

import androidx.media3.common.Player

/** Visibility outlives temporary PiP flags; a stopped/closed screen must never start audio. */
internal class PipPlaybackPolicy {
    private var visible = true
    private var closed = false

    val canPlay: Boolean get() = visible && !closed

    fun started() { visible = true }
    fun stopped() { visible = false }
    fun close() { closed = true; visible = false }

    fun playbackRequested(playWhenReady: Boolean, state: Int): Boolean =
        playWhenReady && (state == Player.STATE_READY || state == Player.STATE_BUFFERING)

    fun shouldAutoEnter(playWhenReady: Boolean, state: Int): Boolean =
        canPlay && playbackRequested(playWhenReady, state)
}
