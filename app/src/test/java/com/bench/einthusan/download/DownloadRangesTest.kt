package com.bench.einthusan.download

import org.junit.Assert.*
import org.junit.Test

class DownloadRangesTest {
    @Test fun coversEveryByteWithoutGapsOrOverlap() {
        for (total in listOf(1L, 2L, 5L, 6L, 7L, 100L, 1024L, Long.MAX_VALUE)) {
            val ranges = DownloadRanges.split(total, 6)
            assertEquals(0L, ranges.first().start)
            assertEquals(total - 1, ranges.last().end)
            assertEquals(total, ranges.sumOf { it.size })
            ranges.zipWithNext().forEach { (a, b) -> assertEquals(a.end + 1, b.start) }
            assertTrue(ranges.all { it.size > 0 })
        }
    }

    @Test fun rejectsMissingSizeAndClampsChunkCount() {
        assertThrows(IllegalArgumentException::class.java) { DownloadRanges.split(0, 6) }
        assertEquals(1, DownloadRanges.split(100, 0).size)
        assertTrue(DownloadRanges.split(100, 99).size <= 12)
    }

    @Test fun rejectsIgnoredOrMismatchedRangeResponses() {
        val requested = ByteRange(100, 199)
        DownloadRanges.validateResponse(206, "bytes 100-199/1000", requested, 1000)
        for ((code, header) in listOf(200 to null, 206 to null, 206 to "bytes 0-99/1000",
            206 to "bytes 100-199/999", 206 to "bytes 100-200/1000")) {
            assertThrows(IllegalStateException::class.java) {
                DownloadRanges.validateResponse(code, header, requested, 1000)
            }
        }
    }
}
