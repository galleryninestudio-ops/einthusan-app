package com.bench.einthusan.download

/** Inclusive byte range in one immutable media file. */
internal data class ByteRange(val start: Long, val end: Long) {
    val size: Long get() = end - start + 1
}

internal object DownloadRanges {
    fun split(total: Long, chunks: Int): List<ByteRange> {
        require(total > 0) { "Host did not report a size" }
        val count = chunks.coerceIn(1, 12).coerceAtMost(total.coerceAtMost(12).toInt())
        val size = total / count + if (total % count == 0L) 0 else 1
        return (0 until count).mapNotNull { i ->
            val start = i * size
            if (start >= total) null else ByteRange(start, start + minOf(size, total - start) - 1)
        }
    }

    fun validateResponse(code: Int, contentRange: String?, requested: ByteRange, total: Long) {
        check(code == 206) { "Host did not honor byte range (HTTP $code)" }
        val match = Regex("bytes (\\d+)-(\\d+)/(\\d+)").matchEntire(contentRange.orEmpty())
            ?: error("Host returned an invalid Content-Range")
        check(match.groupValues[1].toLong() == requested.start &&
            match.groupValues[2].toLong() == requested.end &&
            match.groupValues[3].toLong() == total) { "Host returned a different byte range" }
    }
}
