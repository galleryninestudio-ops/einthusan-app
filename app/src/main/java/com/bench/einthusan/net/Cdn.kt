package com.bench.einthusan.net

import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The watch page hands out a throwaway IP as the stream host. The signature is
 * bound to path + expiry only, never the host, so swapping in a live CDN keeps
 * the same query string valid.
 *
 * Measured on this account: cdn1 and cdn3 serve the media path (206, video/mp4),
 * cdn2 answers 404 for /etv/content/ even though its ping file is alive. So the
 * pool is the two that actually serve, and a host that 404s mid-session is
 * demoted instead of retried.
 */
object Cdn {

    private val HOSTS = listOf("cdn1.einthusan.io", "cdn3.einthusan.io")

    private val IP_HOST = Regex("""^https?://\d{1,3}(?:\.\d{1,3}){3}(/etv/.*)$""")

    private val mutex = Mutex()

    @Volatile private var best: String = HOSTS[0]
    @Volatile private var bestAt: Long = 0L
    @Volatile private var restored = false

    /** host -> time it last failed. Cleared after five minutes. */
    private val dead = ConcurrentHashMap<String, Long>()

    /**
     * Last known good edge, restored from disk. A cold launch then rewrites
     * straight away instead of pausing the first play for two round trips.
     */
    private fun restore() {
        if (restored) return
        restored = true
        val saved = runCatching { Net.store.getString("cdn_host", null) }.getOrNull()
        if (saved != null && HOSTS.contains(saved)) {
            best = saved
            bestAt = runCatching { Net.store.getLong("cdn_at", 0L) }.getOrDefault(0L)
        }
    }

    /** Fastest responder wins; the probe runs both edges at once. */
    suspend fun pick(): String {
        restore()
        val now = System.currentTimeMillis()
        if (now - bestAt < 600_000L && bestAt != 0L && !isDead(best, now)) return best

        return mutex.withLock {
            val t = System.currentTimeMillis()
            if (t - bestAt < 600_000L && bestAt != 0L && !isDead(best, t)) return@withLock best

            val candidates = HOSTS.filter { !isDead(it, t) }.ifEmpty { HOSTS }
            val winner = coroutineScope {
                candidates
                    .map { h -> async { h to Net.head("https://$h/ping.m4v") } }
                    .map { it.await() }
                    .minByOrNull { it.second }
                    ?.first ?: HOSTS[0]
            }
            best = winner
            bestAt = t
            runCatching {
                Net.store.edit().putString("cdn_host", winner).putLong("cdn_at", t).apply()
            }
            winner
        }
    }

    /** A host that 404s the media path is not a candidate any more. */
    fun markBad(host: String?) {
        if (host.isNullOrBlank()) return
        dead[host] = System.currentTimeMillis()
        if (host == best) bestAt = 0L
    }

    fun hostOf(url: String?): String? {
        if (url.isNullOrBlank()) return null
        return Regex("""^https?://([^/]+)/""").find(url)?.groupValues?.get(1)
    }

    private fun isDead(host: String, now: Long): Boolean {
        val at = dead[host] ?: return false
        if (now - at > 300_000L) { dead.remove(host); return false }
        return true
    }

    /** UHD files can exist on one edge only; check the file, not just ping.m4v. */
    suspend fun availableStream(url: String): String? {
        val original = hostOf(url) ?: return null
        val hosts = if (original in HOSTS) listOf(original) + HOSTS.filter { it != original }
            else listOf(original)
        for (host in hosts) {
            val candidate = url.replaceFirst(Regex("^https?://[^/]+"), "https://$host")
            if (Net.streamAvailable(candidate)) return candidate
        }
        return null
    }

    /** Swap the decoy host for a live one. Leaves real hostnames untouched. */
    fun rewrite(raw: String?, host: String = best): String? {
        if (raw.isNullOrBlank()) return null
        val m = IP_HOST.find(raw) ?: return raw
        return "https://$host" + m.groupValues[1]
    }

}
