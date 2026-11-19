package com.bench.einthusan.ui.player

import org.junit.Assert.*
import org.junit.Test

class PinchIntentTest {
    @Test fun ignoresTinyAccidentalMovement() {
        val intent = PinchIntent(1000L, 200f, 24f)
        assertFalse(intent.update(1500L, 1.02f, 1f, 1f, false))
    }

    @Test fun ignoresEvenLargeMovementDuringABriefSecondTouch() {
        val intent = PinchIntent(1000L, 200f, 24f)
        assertFalse(intent.update(1050L, 2f, 0f, 0f, false))
        assertTrue(intent.update(1150L, 1f, 0f, 0f, false))
    }

    @Test fun panningRequiresAnAlreadyZoomedVideoAndDeliberateMotion() {
        val normal = PinchIntent(1000L, 200f, 24f)
        assertFalse(normal.update(1500L, 1f, 100f, 0f, false))
        val zoomed = PinchIntent(1000L, 200f, 24f)
        assertTrue(zoomed.update(1500L, 1f, 100f, 0f, true))
    }

    @Test fun requiresBothPercentageAndPhysicalMovementThresholds() {
        val wide = PinchIntent(1000L, 1000f, 24f)
        assertFalse(wide.update(1500L, 1.04f, 0f, 0f, false))
        val narrow = PinchIntent(1000L, 20f, 24f)
        assertFalse(narrow.update(1500L, 1.1f, 0f, 0f, false))
    }
}
