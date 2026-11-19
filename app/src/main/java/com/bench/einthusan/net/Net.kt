package com.bench.einthusan.net

import android.content.Context
import android.content.SharedPreferences
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

/**
 * Cookies survive process death. The site keeps a gorilla/sessions triple
 * (sid / _gorilla_csrf / tid) and drops the session if they vanish, so they
 * are written to disk on every response.
 */
class PersistCookieJar(private val prefs: SharedPreferences) : CookieJar {

    private val store = LinkedHashMap<String, Cookie>()
    private val lock = Any()

    init {
        runCatching { restore() }
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        if (cookies.isEmpty()) return
        synchronized(lock) {
            for (c in cookies) {
                val key = c.name + "|" + c.domain
                if (c.expiresAt <= System.currentTimeMillis()) store.remove(key) else store[key] = c
            }
            persist()
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val now = System.currentTimeMillis()
        synchronized(lock) {
            val out = ArrayList<Cookie>(4)
            val dead = ArrayList<String>(2)
            for ((k, c) in store) {
                if (c.expiresAt <= now) { dead.add(k); continue }
                if (c.matches(url)) out.add(c)
            }
            if (dead.isNotEmpty()) {
                dead.forEach { store.remove(it) }
                persist()
            }
            return out
        }
    }

    fun clear() {
        synchronized(lock) { store.clear(); prefs.edit().remove(KEY).apply() }
    }

    private fun persist() {
        val arr = JSONArray()
        for (c in store.values) {
            val o = JSONObject()
            o.put("n", c.name)
            o.put("v", c.value)
            o.put("d", c.domain)
            o.put("p", c.path)
            o.put("e", c.expiresAt)
            o.put("s", c.secure)
            o.put("h", c.httpOnly)
            arr.put(o)
        }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    private fun restore() {
        val raw = prefs.getString(KEY, null) ?: return
        val arr = JSONArray(raw)
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val rebuilt = Cookie.Builder()
                .name(o.getString("n"))
                .value(o.getString("v"))
                .domain(o.getString("d"))
                .path(o.getString("p"))
                .expiresAt(o.getLong("e"))
                .apply {
                    if (o.optBoolean("s")) secure()
                    if (o.optBoolean("h")) httpOnly()
                }
                .build()
            store[rebuilt.name + "|" + rebuilt.domain] = rebuilt
        }
    }

    private companion object {
        const val KEY = "cookie_jar_v1"
    }
}

object Net {

    const val BASE = "https://einthusan.tv"

    /** Chrome mobile string. The site serves a leaner document to real browsers. */
    const val UA =
        "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36"

    const val ARC_VERSION = "12"
    const val APP_VERSION = "364"

    lateinit var jar: PersistCookieJar
        private set

    lateinit var tabId: String
        private set

    private lateinit var prefs: SharedPreferences

    /** Shared app prefs for the small caches (CDN choice) that survive a restart. */
    val store: SharedPreferences get() = prefs

    val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .cookieJar(jar)
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(22, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .followRedirects(false)
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("User-Agent", UA)
                        .header("Accept-Language", "en-US,en;q=0.9")
                        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                        .build()
                )
            }
            .build()
    }

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.applicationContext.getSharedPreferences("net", Context.MODE_PRIVATE)
        jar = PersistCookieJar(prefs)
        tabId = prefs.getString("tab_id", null) ?: run {
            val t = "android-" + (100000..999999).random()
            prefs.edit().putString("tab_id", t).apply()
            t
        }
    }

    suspend fun get(path: String, follow: Boolean = true): String? =
        getWithStatus(path, follow).second

    suspend fun getWithStatus(path: String, follow: Boolean = true): Pair<Int, String?> =
        withContext(Dispatchers.IO) {
            runCatching {
                val req = Request.Builder()
                    .url(if (path.startsWith("http")) path else BASE + path)
                    .build()

                var res = client.newCall(req).execute()
                var hops = 0
                while (follow && res.isRedirect && hops < 3) {
                    val loc = res.header("Location") ?: break
                    res.close()
                    val next = if (loc.startsWith("http")) loc else BASE + loc
                    res = client.newCall(
                        Request.Builder().url(next)
                            .header("Referer", BASE + "/").build()
                    ).execute()
                    hops++
                }
                res.use { it.code to it.body.string() }
            }.getOrDefault(0 to null)
        }

    /** Check a signed stream before replacing a working video with it. */
    suspend fun streamAvailable(url: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder().url(url).header("Range", "bytes=0-0").build()
            client.newCall(request).execute().use { response ->
                val type = response.header("Content-Type").orEmpty()
                response.isSuccessful &&
                    (type.startsWith("video/") || type.startsWith("application/octet-stream"))
            }
        }.getOrDefault(false)
    }

    suspend fun head(url: String): Long = withContext(Dispatchers.IO) {
        runCatching {
            val t0 = System.currentTimeMillis()
            val req = Request.Builder().url(url).head().build()
            client.newCall(req).execute().use { res ->
                if (res.isSuccessful) System.currentTimeMillis() - t0 else Long.MAX_VALUE
            }
        }.getOrDefault(Long.MAX_VALUE)
    }

    /** Scrapes the per-page CSRF token out of <html data-pageid="...">. */
    suspend fun token(path: String): String? = withContext(Dispatchers.IO) {
        val html = get(path) ?: return@withContext null
        Regex("data-pageid=\"([^\"]+)\"")
            .find(html)?.groupValues?.get(1)
            ?.let(::decodeEntities)
    }

    /** POST /ajax<path> with the site's envelope. Returns the raw JSON or null. */
    suspend fun ajax(path: String, event: String, json: String): JSONObject? =
        withContext(Dispatchers.IO) {
            val token = token(path) ?: return@withContext null
            val body = FormBody.Builder()
                .add("xEvent", event)
                .add("xJson", json)
                .add("arcVersion", ARC_VERSION)
                .add("appVersion", APP_VERSION)
                .add("tabID", tabId)
                .add("gorilla.csrf.Token", token)
                .build()

            runCatching {
                val req = Request.Builder()
                    .url(BASE + "/ajax" + path)
                    .header("X-Requested-With", "XMLHttpRequest")
                    .header("Referer", BASE + path)
                    .header("Accept", "application/json, text/javascript, */*; q=0.01")
                    .post(body)
                    .build()

                client.newCall(req).execute().use { res ->
                    val text = res.body.string()
                    if (res.isSuccessful && text.trimStart().startsWith("{")) JSONObject(text) else null
                }
            }.getOrNull()
        }

    private val ENTITY = Regex("&#(\\d+);")

    fun decodeEntities(s: String): String =
        ENTITY.replace(s) { it.groupValues[1].toIntOrNull()?.toChar()?.toString() ?: it.value }

    fun absolute(url: String?): String? = when {
        url == null -> null
        url.startsWith("//") -> "https:$url"
        url.startsWith("/") -> BASE + url
        else -> url
    }
}
