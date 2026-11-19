package com.bench.einthusan.ui.player

import org.junit.Assert.*
import org.junit.Test

class VideoTransformTest {
    @Test fun zoomsAroundTheFingersRatherThanJumpingToTheCentre() {
        val state = VideoTransform().change(2f, 0f, 0f, 750f, 300f, 1000f, 600f)
        assertEquals(2f, state.scale, 0.001f)
        assertEquals(-250f, state.offsetX, 0.001f)
        assertEquals(0f, state.offsetY, 0.001f)
    }

    @Test fun clampsPanningToTheEnlargedVideoBounds() {
        val state = VideoTransform(2f).change(1f, 10000f, -10000f, 500f, 300f, 1000f, 600f)
        assertEquals(500f, state.offsetX, 0.001f)
        assertEquals(-300f, state.offsetY, 0.001f)
    }

    @Test fun shrinkingBackToNormalRecentresTheVideo() {
        val state = VideoTransform(2f, 200f, 100f).change(0.1f, 0f, 0f, 750f, 200f, 1000f, 600f)
        assertEquals(VideoTransform(), state)
    }

    @Test fun capsZoomAndIgnoresInvalidTouchMeasurements() {
        val normal = VideoTransform()
        assertEquals(4f, normal.change(100f, 0f, 0f, 500f, 300f, 1000f, 600f).scale, 0.001f)
        assertEquals(normal, normal.change(Float.NaN, 0f, 0f, 500f, 300f, 1000f, 600f))
        assertEquals(normal, normal.change(2f, 0f, 0f, Float.NaN, 300f, 1000f, 600f))
    }
}
