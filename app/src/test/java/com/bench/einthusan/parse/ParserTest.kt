package com.bench.einthusan.parse

import org.junit.Assert.*
import org.junit.Test

class ParserTest {
    @Test fun readsCatalogueQualityWithoutClaiming4k() {
        val html = """
            <div class="results-info"><p>Page 2 of 4 (24 results)</p></div>
            <section id="UIMovieSummary"><ul><li>
                <div class="block1"><a href="/movie/watch/9032/?lang=hindi"><img src="//images.example.test/poster.jpg"></a><i class="ultrahd"></i></div>
                <div class="block2"><a class="title"><h3>Sultan</h3></a><div class="info"><p>2016 <span>Hindi</span></p><p class="cc">Subtitles</p></div></div>
            </li></ul></section>
        """.trimIndent()
        val result = Parser.results(html, 2)
        assertEquals(4, result.totalPages)
        assertEquals("9032", result.movies.single().id)
        assertEquals("https://images.example.test/poster.jpg", result.movies.single().poster)
        assertTrue(result.movies.single().uhd)
        assertTrue(result.movies.single().subtitle)
    }

    @Test fun preservesSignatureAndExtractsExpiryFromWatchPage() {
        val info = Parser.watch("""<section id="UIVideoPlayer" data-mp4-link="https://12.34.56.78/etv/content/B9032.mp4?e=1234567890&amp;md5=signature"></section>""", "cdn3.einthusan.io")
        assertEquals("https://cdn3.einthusan.io/etv/content/B9032.mp4?e=1234567890&md5=signature", info.mp4)
        assertEquals(1234567890L, info.expiresAt)
        assertFalse(info.premiumHd)
        assertTrue(info.playable)
    }

    @Test fun rejectsPagesWithoutAStream() {
        assertFalse(Parser.watch("<html>Sign in</html>", "cdn1.einthusan.io").playable)
    }
}
