package com.bench.einthusan.parse

import com.bench.einthusan.net.Cdn
import com.bench.einthusan.net.Net
import org.jsoup.Jsoup

object Parser {

    private val WATCH_ID = Regex("""/movie/watch/([A-Za-z0-9]+)""")
    private val PAGE_OF = Regex("""Page\s+(\d+)\s+of\s+(\d+)""")
    private val SIGNED_E = Regex("""[?&]e=(\d+)""")

    /**
     * Handles /movie/results/ — used for search, A-Z, decade, cast and the
     * curated rows (find=Recent, StaffPick, RegionalHit, ComingSoon, Numbers).
     */
    fun results(html: String, page: Int = 1): Paged {
        val doc = Jsoup.parse(html)
        val list = doc.select("#UIMovieSummary ul > li")
        val movies = ArrayList<Movie>(list.size)

        for (li in list) {
            val anchor = li.selectFirst("div.block1 a") ?: continue
            val href = anchor.attr("href")
            val id = WATCH_ID.find(href)?.groupValues?.get(1) ?: continue

            val infoP = li.selectFirst("div.info")
                ?.select("p")
                ?.firstOrNull { !it.hasClass("cc") }

            val titleEl = li.selectFirst("div.block2 a.title h3")
                ?: li.selectFirst("div.block2 a.title h2")
                ?: li.selectFirst("div.block2 a.title")

            movies.add(
                Movie(
                    id = id,
                    title = titleEl?.text()?.trim().orEmpty().ifEmpty { id },
                    year = infoP?.ownText()?.trim().orEmpty(),
                    lang = li.selectFirst("div.info p span")?.text()?.trim().orEmpty(),
                    poster = Net.absolute(li.selectFirst("div.block1 img")?.attr("src")),
                    uhd = li.selectFirst("i.ultrahd") != null,
                    subtitle = li.selectFirst("p.cc, p.cc i") != null,
                    synopsis = li.selectFirst("p.synopsis")?.text()?.trim(),
                    badge = li.selectFirst("span.popular")?.text()?.trim(),
                    playable = anchor.attr("data-disabled").lowercase() != "true",
                )
            )
        }

        val header = doc.selectFirst(".results-info p")?.text().orEmpty()
        val totalPages = PAGE_OF.find(header)?.groupValues?.get(2)?.toIntOrNull() ?: page

        return Paged(movies, totalPages)
    }

    /** /movie/watch/<id>/ — every stream detail is an attribute on #UIVideoPlayer. */
    fun watch(html: String, cdnHost: String): WatchInfo {
        val doc = Jsoup.parse(html)
        val player = doc.selectFirst("section#UIVideoPlayer")

        val mp4Raw = player?.attr("data-mp4-link")?.takeIf { it.isNotBlank() }
        val hlsRaw = player?.attr("data-hls-link")?.takeIf { it.isNotBlank() }

        val signed = mp4Raw ?: hlsRaw
        val expiry = signed?.let { SIGNED_E.find(it)?.groupValues?.get(1)?.toLongOrNull() } ?: 0L

        return WatchInfo(
            mp4 = Cdn.rewrite(mp4Raw, cdnHost),
            hls = Cdn.rewrite(hlsRaw, cdnHost),
            expiresAt = expiry,
        )
    }

}
