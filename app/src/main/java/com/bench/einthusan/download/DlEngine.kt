package com.bench.einthusan.download

import android.content.Context
import com.bench.einthusan.net.Net
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request

/**
 * Chunked, resumable transfer.
 *
 * A single connection to this CDN tops out around 1.2 MB/s; six ranged
 * connections pull the same file far faster, and each one writes its own part
 * file, so stopping and restarting resumes every chunk from its own byte.
 * When all parts are complete they are joined and published into Movies on the
 * device — MediaStore on Android 10+, a plain file below that.
 */
object DlEngine {

    private val running = java.util.concurrent.ConcurrentHashMap<String, Boolean>()

    fun isRunning(id: String) = running[id] == true

    fun cancel(id: String) {
        running.computeIfPresent(id) { _, _ -> false }
    }

    suspend fun run(context: Context, task: DlTask) = withContext(Dispatchers.IO) {
        if (running.putIfAbsent(task.id, true) != null) return@withContext
        var meter: Meter? = null
        var knownTotal = task.total
        DlStore.update(context, task.id) { it.copy(status = DlStatus.RUNNING, error = null) }

        try {
            val total = if (task.total > 0L) task.total else probe(task.url)
            if (total <= 0L) error("Host did not report a size")

            knownTotal = total
            val ranges = DownloadRanges.split(total, task.chunks)
            val chunks = ranges.size
            DlStore.update(context, task.id) { it.copy(total = total, chunks = chunks) }
            val dir = DlStore.taskDir(context, task.id)
            val counter = AtomicLong(0)

            // Seed the counter with whatever survived the last run.
            ranges.forEachIndexed { i, range ->
                val part = File(dir, "$i.part")
                if (part.length() > range.size) RandomAccessFile(part, "rw").use { it.setLength(0) }
                counter.addAndGet(part.length())
            }
            DlStore.setProgress(task.id, DlProgress(counter.get(), total, 0L))

            meter = Meter(counter, total, task.id)
            meter.start(this)

            coroutineScope {
                ranges.mapIndexed { i, range ->
                    async {
                        RangeTransfer.download(Net.client, task.url, File(dir, "$i.part"), range, total, counter) {
                            running[task.id] != true
                        }
                    }
                }.awaitAll()
            }

            meter.stop()
            if (running[task.id] != true) {
                DlStore.update(context, task.id) {
                    it.copy(status = if (it.status == DlStatus.QUEUED) DlStatus.QUEUED else DlStatus.PAUSED, total = total)
                }
                return@withContext
            }

            if (DlStore.find(context, task.id) == null) return@withContext
            val merged = File(dir, "merged.mp4")
            join(dir, chunks, merged)

            val published = DownloadFiles.publish(context, task, merged)
            if (DlStore.find(context, task.id) == null) {
                DownloadFiles.delete(context, task.copy(playUri = published))
                dir.deleteRecursively()
                return@withContext
            }
            dir.deleteRecursively()

            DlStore.update(context, task.id) {
                it.copy(status = DlStatus.DONE, total = total, playUri = published, error = null)
            }
            DlStore.setProgress(task.id, DlProgress(total, total, 0L))
        } catch (e: CancellationException) {
            DlStore.update(context, task.id) { it.copy(status = DlStatus.PAUSED, total = knownTotal) }
            throw e
        } catch (e: Exception) {
            DlStore.update(context, task.id) { it.copy(status = DlStatus.FAILED, total = knownTotal, error = e.message) }
        } finally {
            meter?.stop()
            running.remove(task.id)
            DlStore.setProgress(task.id, null)
        }
    }

    private fun probe(url: String): Long {
        val req = Request.Builder().url(url).head().build()
        Net.client.newCall(req).execute().use { res ->
            check(res.isSuccessful) { "HTTP ${res.code}" }
            return res.header("Content-Length")?.toLongOrNull() ?: -1L
        }
    }

    private fun join(dir: File, chunks: Int, target: File) {
        target.delete()
        target.outputStream().buffered(1 shl 20).use { out ->
            for (i in 0 until chunks) {
                val part = File(dir, "$i.part")
                check(part.exists()) { "Missing download part $i" }
                part.inputStream().buffered(1 shl 20).use { it.copyTo(out, 512 * 1024) }
            }
        }
    }

    /** Speed meter: pushes bytes and a rolling MB/s to the UI twice a second. */
    private class Meter(
        private val counter: AtomicLong,
        private val total: Long,
        private val id: String,
    ) {
        private var job: kotlinx.coroutines.Job? = null
        private var lastBytes = counter.get()
        private var lastAt = System.currentTimeMillis()

        fun start(scope: kotlinx.coroutines.CoroutineScope) {
            job = scope.launch {
                while (true) {
                    val now = System.currentTimeMillis()
                    val bytes = counter.get()
                    val elapsed = (now - lastAt).coerceAtLeast(1L)
                    val bps = ((bytes - lastBytes) * 1000L) / elapsed
                    lastBytes = bytes
                    lastAt = now
                    DlStore.setProgress(id, DlProgress(bytes, total, bps))
                    delay(500)
                }
            }
        }

        fun stop() {
            job?.cancel()
            job = null
        }
    }
}
