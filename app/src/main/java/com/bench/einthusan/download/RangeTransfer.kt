package com.bench.einthusan.download

import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.atomic.AtomicLong
import okhttp3.Request

/** HTTP range transfer without Android storage or service dependencies. */
internal object RangeTransfer {
    /** One range, appended to a part file, resuming at its current length. */
    fun download(
        client: okhttp3.OkHttpClient,
        url: String,
        part: File,
        range: ByteRange,
        total: Long,
        counter: AtomicLong,
        cancelled: () -> Boolean,
    ) {
        var have = part.length()
        val want = range.size
        if (have >= want) return

        val from = range.start + have
        val req = Request.Builder()
            .url(url)
            .header("Range", "bytes=$from-${range.end}")
            .build()

        client.newCall(req).execute().use { res ->
            DownloadRanges.validateResponse(res.code, res.header("Content-Range"), ByteRange(from, range.end), total)
            val body = res.body

            RandomAccessFile(part, "rw").use { out ->
                out.seek(have)
                body.byteStream().use { input ->
                    val buffer = ByteArray(256 * 1024)
                    while (true) {
                        if (cancelled()) return
                        val n = input.read(buffer)
                        if (n <= 0) break
                        check(have + n <= want) { "Host sent more bytes than requested" }
                        out.write(buffer, 0, n)
                        have += n
                        counter.addAndGet(n.toLong())
                    }
                }
            }
            check(have == want) { "Stream ended before the requested range was complete" }
        }
    }

}
