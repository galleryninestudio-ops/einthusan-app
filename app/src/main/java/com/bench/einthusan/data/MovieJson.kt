package com.bench.einthusan.data

import com.bench.einthusan.parse.Movie
import org.json.JSONObject

/** Shared movie representation for saved films and resume entries. Keep legacy keys stable. */
internal object MovieJson {
    fun read(json: JSONObject): Movie? {
        val id = json.optionalText("id") ?: return null
        return Movie(
            id = id,
            title = json.optString("title").ifBlank { id },
            year = json.optString("year"),
            lang = json.optString("lang"),
            poster = json.optionalText("poster"),
            uhd = json.optBoolean("uhd"),
            subtitle = json.optBoolean("sub"),
            synopsis = json.optionalText("syn"),
            badge = json.optionalText("badge"),
            playable = json.optBoolean("play", true),
        )
    }

    fun write(movie: Movie): JSONObject = JSONObject()
        .put("id", movie.id)
        .put("title", movie.title)
        .put("year", movie.year)
        .put("lang", movie.lang)
        .put("poster", movie.poster ?: "")
        .put("uhd", movie.uhd)
        .put("sub", movie.subtitle)
        .put("syn", movie.synopsis ?: "")
        .put("badge", movie.badge ?: "")
        .put("play", movie.playable)

    private fun JSONObject.optionalText(key: String): String? =
        if (isNull(key)) null else optString(key).takeIf { it.isNotBlank() }
}
