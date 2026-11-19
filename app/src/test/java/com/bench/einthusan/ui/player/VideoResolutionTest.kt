package com.bench.einthusan.ui.player

import org.junit.Assert.*
import org.junit.Test

class VideoResolutionTest {
    @Test fun neverTreats720PixelWidthAs720pOrHdAs4k() {
        assertEquals("720 × 480", videoResolution(720, 480))
        assertEquals("1080p · 1920 × 1080", videoResolution(1920, 1080))
        assertEquals("1920 × 800", videoResolution(1920, 800))
        assertEquals("3800 × 1600", videoResolution(3800, 1600))
    }

    @Test fun reportsDecoded4kIncludingCinematicAspectRatios() {
        assertEquals("4K · 3840 × 2160", videoResolution(3840, 2160))
        assertEquals("4K · 3840 × 1600", videoResolution(3840, 1600))
        assertEquals("720p · 1280 × 720", videoResolution(1280, 720))
    }

    @Test fun waitsForValidDecoderDimensions() {
        assertNull(videoResolution(0, 0))
        assertNull(videoResolution(1920, -1))
    }
}
