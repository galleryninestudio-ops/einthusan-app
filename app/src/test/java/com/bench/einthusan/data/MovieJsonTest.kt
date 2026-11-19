package com.bench.einthusan.data

import com.bench.einthusan.parse.Movie
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class MovieJsonTest {
    @Test fun preservesSavedMovieAndLegacyKeys() {
        val movie = Movie("9032", "Sultan", "2016", "hindi", "https://example.test/poster.jpg",
            true, true, "A story", "Popular", false)
        val json = MovieJson.write(movie)
        assertEquals(movie, MovieJson.read(JSONObject(json.toString())))
        assertEquals(true, json.getBoolean("uhd"))
        assertEquals(true, json.getBoolean("sub"))
        assertEquals("A story", json.getString("syn"))
        assertEquals(false, json.getBoolean("play"))
    }

    @Test fun readsExistingResumeEntryWithoutPlayableField() {
        val movie = MovieJson.read(JSONObject("""{"id":"9032","title":"Sultan","lang":"hindi","pos":42000,"dur":90000,"uhd":true}"""))!!
        assertTrue(movie.playable)
        assertTrue(movie.uhd)
        assertNull(movie.poster)
        assertEquals("Sultan", movie.title)
    }

    @Test fun normalizesMissingBlankAndJsonNullOptionalFields() {
        val movie = MovieJson.read(JSONObject("""{"id":"film","poster":null,"syn":"","badge":" "}"""))!!
        assertNull(movie.poster)
        assertNull(movie.synopsis)
        assertNull(movie.badge)
        assertEquals("film", movie.title)
    }

    @Test fun skipsEntriesWithoutAnId() {
        assertNull(MovieJson.read(JSONObject()))
        assertNull(MovieJson.read(JSONObject("""{"id":null}""")))
        assertNull(MovieJson.read(JSONObject("""{"id":" "}""")))
    }
}
