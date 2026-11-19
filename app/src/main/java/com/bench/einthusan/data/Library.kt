package com.bench.einthusan.data

import android.content.Context
import com.bench.einthusan.parse.Movie
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject

/**
 * Watchlist, resume points and last-used language. Flat JSON in SharedPreferences
 * so the app carries no database and no KSP.
 */
class Library(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("library", Context.MODE_PRIVATE)

    /**
     * Increments on every write. Screens re-read from this instead of holding a
     * snapshot taken once, which is why removing a film from Continue watching
     * used to leave the row on screen until the language was reopened.
     */
    private val revision = MutableStateFlow(0L)
    val changes = revision.asStateFlow()

    private fun ping() {
        revision.update { it + 1 }
    }

    var lastLang: String
        get() = prefs.getString("last_lang", "tamil") ?: "tamil"
        set(v) = prefs.edit().putString("last_lang", v).apply()

    var premiumKnown: Boolean
        get() = prefs.getBoolean("premium", false)
        set(v) = prefs.edit().putBoolean("premium", v).apply()

    fun isSaved(id: String): Boolean = savedRaw().any { it.id == id }

    fun toggle(m: Movie): Boolean {
        val list = savedRaw().toMutableList()
        val existing = list.indexOfFirst { it.id == m.id }
        val nowSaved = if (existing >= 0) { list.removeAt(existing); false } else { list.add(0, m); true }
        writeSaved(list)
        ping()
        return nowSaved
    }

    fun saved(): List<Movie> = savedRaw()

    fun setPosition(id: String, ms: Long) {
        if (ms <= 5_000L) return
        val obj = positionsRaw()
        obj.put(id, ms)
        prefs.edit().putString("positions", obj.toString()).apply()
        ping()
    }

    fun position(id: String): Long = positionsRaw().optLong(id, 0L)

    private fun savedRaw(): MutableList<Movie> {
        val raw = prefs.getString("saved", "[]") ?: "[]"
        val out = ArrayList<Movie>()
        runCatching {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                MovieJson.read(o)?.let(out::add)
            }
        }
        return out
    }

    private fun writeSaved(list: List<Movie>) {
        val arr = JSONArray()
        list.forEach { arr.put(MovieJson.write(it)) }
        prefs.edit().putString("saved", arr.toString()).apply()
    }

    private fun positionsRaw(): JSONObject =
        runCatching { JSONObject(prefs.getString("positions", "{}") ?: "{}") }.getOrElse { JSONObject() }

    /* ---------------- continue watching ---------------- */

    data class Progress(val movie: Movie, val positionMs: Long, val durationMs: Long) {
        val fraction: Float
            get() = if (durationMs > 0L) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
        val leftMs: Long get() = (durationMs - positionMs).coerceAtLeast(0L)
    }

    /** Called from the player a few times a minute; keeps at most twenty entries. */
    fun recordProgress(movie: Movie, positionMs: Long, durationMs: Long) {
        if (positionMs < 15_000L || durationMs <= 0L) return
        if (positionMs.toFloat() / durationMs > 0.94f) {
            clearProgress(movie.id)
            return
        }
        val list = recentRaw().filterNot { it.movie.id == movie.id }.toMutableList()
        list.add(0, Progress(movie, positionMs, durationMs))
        writeRecent(list.take(20))
        ping()
    }

    fun continueWatching(): List<Progress> = recentRaw()

    fun clearProgress(id: String) {
        writeRecent(recentRaw().filterNot { it.movie.id == id })
        ping()
    }

    private fun recentRaw(): List<Progress> {
        val raw = prefs.getString("recent", "[]") ?: "[]"
        val out = ArrayList<Progress>()
        runCatching {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val m = MovieJson.read(o) ?: continue
                out.add(Progress(m, o.optLong("pos"), o.optLong("dur")))
            }
        }
        return out
    }

    private fun writeRecent(list: List<Progress>) {
        val arr = JSONArray()
        for (p in list) {
            arr.put(MovieJson.write(p.movie).put("pos", p.positionMs).put("dur", p.durationMs))
        }
        prefs.edit().putString("recent", arr.toString()).apply()
    }
}
