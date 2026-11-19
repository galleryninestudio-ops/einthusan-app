package com.bench.einthusan.ui.player

import androidx.media3.common.Player
import org.junit.Assert.*
import org.junit.Test

class PipPlaybackPolicyTest {
    @Test fun bufferingWithPlayRequestedStillOffersPause() {
        val policy = PipPlaybackPolicy()
        assertTrue(policy.playbackRequested(true, Player.STATE_BUFFERING))
        assertTrue(policy.playbackRequested(true, Player.STATE_READY))
        assertFalse(policy.playbackRequested(false, Player.STATE_BUFFERING))
        assertFalse(policy.playbackRequested(true, Player.STATE_ENDED))
        assertFalse(policy.playbackRequested(true, Player.STATE_IDLE))
    }

    @Test fun stoppedActivityCannotAutoEnterOrStartLateAudio() {
        val policy = PipPlaybackPolicy()
        assertTrue(policy.canPlay)
        policy.stopped()
        assertFalse(policy.canPlay)
        assertFalse(policy.shouldAutoEnter(true, Player.STATE_READY))
        policy.started()
        assertTrue(policy.canPlay)
    }

    @Test fun closingCannotBeUndoneByASpuriousStartCallback() {
        val policy = PipPlaybackPolicy()
        policy.close()
        policy.started()
        assertFalse(policy.canPlay)
        assertFalse(policy.shouldAutoEnter(true, Player.STATE_BUFFERING))
    }

    @Test fun pausedPlaybackNeverAutoEntersPip() {
        assertFalse(PipPlaybackPolicy().shouldAutoEnter(false, Player.STATE_READY))
    }
}
