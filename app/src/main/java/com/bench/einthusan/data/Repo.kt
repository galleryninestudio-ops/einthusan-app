package com.bench.einthusan.data

import android.net.Uri
import com.bench.einthusan.net.Cdn
import com.bench.einthusan.net.Net
import com.bench.einthusan.parse.Paged
import com.bench.einthusan.parse.Parser
import com.bench.einthusan.parse.WatchInfo
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

object Repo {

    @Volatile
    var premium: Boolean = false

    fun resultsUrl(
        lang: String,
        find: String? = null,
        alpha: String? = null,
        decade: String? = null,
        query: String? = null,
        page: Int = 1,
        sort: String? = null,
    ): String = buildString {
        append("/movie/results/?lang=").append(lang)
        if (!query.isNullOrBlank()) append("&query=").append(Uri.encode(query))
        if (find != null) append("&find=").append(find)
        if (alpha != null) append("&alpha=").append(alpha)
        if (decade != null) append("&decade=").append(decade)
        if (sort != null) append("&sort=").append(Uri.encode(sort))
        append("&page=").append(page)
    }

    suspend fun results(
        lang: String,
        find: String? = null,
        alpha: String? = null,
        decade: String? = null,
        query: String? = null,
        page: Int = 1,
        sort: String? = null,
    ): Paged {
        val html = Net.get(resultsUrl(lang, find, alpha, decade, query, page, sort)) ?: return Paged(emptyList(), page)
        return Parser.results(html, page)
    }

    private val watchCache = java.util.concurrent.ConcurrentHashMap<String, Pair<Long, WatchInfo>>()

    fun clearWatchCache() = watchCache.clear()

    private fun cacheKey(id: String, lang: String, uhd: Boolean) = "$id|$lang|$uhd"

    /**
     * Watch info, memoised. The detail screen warms this while the poster is
     * still settling, so tapping Play goes straight to the decoder instead of
     * waiting on a page fetch first.
     */
    suspend fun watch(
        id: String,
        lang: String,
        uhd: Boolean = false,
        fresh: Boolean = false,
    ): WatchInfo {
        val key = cacheKey(id, lang, uhd)
        if (!fresh) {
            val hit = watchCache[key]
            if (hit != null && System.currentTimeMillis() - hit.first < 600_000L &&
                (hit.second.expiresAt == 0L || hit.second.expiresAt > System.currentTimeMillis() / 1000 + 30)
            ) return hit.second
        }
        val info = watchLive(id, lang, uhd)
        if (info.playable && (!uhd || info.premiumHd)) watchCache[key] = System.currentTimeMillis() to info else watchCache.remove(key)
        return info
    }

    /**
     * Premium watch pages redirect into /premium/movie/watch/. Follow that
     * redirect before checking the stream; a subscription page has no player
     * and falls back to the standard request without changing account status.
     */
    private suspend fun watchLive(id: String, lang: String, uhd: Boolean = false): WatchInfo =
        coroutineScope {
            // The site's mobile redirect adds html5=true but drops uhd=true.
            // Request the player page directly so the selected mode survives.
            val prefix = if (premium) "/premium" else ""
            val base = "$prefix/movie/watch/$id/?html5=true&lang=$lang"

            // The page fetch and the edge probe do not depend on each other, so
            // the first play does not pay for them one after the other.
            val hostDeferred = async { Cdn.pick() }
            if (uhd && premium) {
                val (code, body) = Net.getWithStatus("$base&uhd=true", follow = true)
                if (code in 200..299 && body != null) {
                    val candidate = Parser.watch(body, hostDeferred.await())
                    val stream = candidate.mp4
                    val available = stream?.let { Cdn.availableStream(it) }
                    if (available != null) {
                        return@coroutineScope candidate.copy(mp4 = available, premiumHd = true)
                    }
                }
            }
            val page = Net.get(base)
            if (page == null) {
                hostDeferred.cancel()
                return@coroutineScope WatchInfo(mp4 = null, hls = null)
            }
            Parser.watch(page, hostDeferred.await())
        }

    /**
     * The site's own language tile stills. Deterministic asset paths on the
     * image CDN, so no page has to be scraped to paint the home grid.
     */
    fun tileArt(lang: String): String = "https://cdn2.einthusan.io/etv/s3/etc/img/$lang.jpg"
}
