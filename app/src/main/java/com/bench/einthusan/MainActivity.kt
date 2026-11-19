package com.bench.einthusan

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import com.bench.einthusan.net.Net
import com.bench.einthusan.parse.Movie
import com.bench.einthusan.ui.AppRoot
import com.bench.einthusan.ui.AppState
import com.bench.einthusan.ui.Route
import com.bench.einthusan.ui.player.Pip
import com.bench.einthusan.ui.theme.BenchTheme
import com.bench.einthusan.ui.theme.Ink
import com.bench.einthusan.ui.theme.LANGS

class MainActivity : ComponentActivity() {

    /** Deep link target, observed by the composition so a warm link re-routes. */
    private val link = mutableStateOf<Route?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Net.init(this)

        // Share the app's OkHttp client with image loading: one connection pool,
        // one cookie jar, and no second network stack in the APK.
        SingletonImageLoader.setSafe { context ->
            ImageLoader.Builder(context)
                .components { add(OkHttpNetworkFetcherFactory(callFactory = { Net.client })) }
                .crossfade(true)
                .build()
        }

        val app = AppState(this)
        Pip.bind(this)

        // Warm the edge pick off the critical path: the first play then has a
        // host ready instead of probing two CDNs while the viewer waits.
        Thread {
            runCatching { kotlinx.coroutines.runBlocking { com.bench.einthusan.net.Cdn.pick() } }
        }.start()

        link.value = routeFrom(intent)

        enableEdgeToEdge()

        setContent {
            BenchTheme {
                Surface(Modifier.fillMaxSize(), color = Ink) {
                    AppRoot(app, link, onLinkHandled = { link.value = null })
                }
            }
        }
    }

    /**
     * Leaving the app during playback hands the film to the corner window.
     * Nothing happens from any other screen, or while the film is paused.
     */
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        Pip.onUserLeave(this)
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration,
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        Pip.onPipChanged(isInPictureInPictureMode)
    }

    override fun onStart() {
        super.onStart()
        Pip.onActivityStarted()
    }

    override fun onStop() {
        super.onStop()
        Pip.onActivityStop()
    }

    override fun onDestroy() {
        Pip.unbind(this)
        super.onDestroy()
    }

    /** A link that arrives while the activity is alive is routed, not ignored. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        routeFrom(intent)?.let { link.value = it }
    }

    /**
     * einthusan://watch?id=<movieId>&lang=<language>[&play=1][&title=<name>]
     * Launches straight into the player so playback can be driven cold — and so
     * a shared link from anywhere lands on the right film.
     */
    private fun routeFrom(intent: Intent?): Route? {
        val data = intent?.data ?: return null
        if (data.host != "watch") return null
        val id = data.getQueryParameter("id")?.takeIf { it.isNotBlank() } ?: return null
        if (data.scheme != "einthusan") return null
        val lang = data.getQueryParameter("lang")?.lowercase()
            ?.takeIf { q -> LANGS.any { it.key == q } }
            ?: "tamil"
        val title = data.getQueryParameter("title")?.takeIf { it.isNotBlank() } ?: id
        val play = data.getQueryParameter("play") == "1"

        val movie = Movie(
            id = id,
            title = title,
            year = data.getQueryParameter("year").orEmpty(),
            lang = lang,
            poster = null,
            uhd = data.getQueryParameter("uhd") == "1",
            subtitle = false,
            synopsis = null,
            badge = null,
            playable = true,
        )
        return if (play) Route.Player(movie) else Route.Detail(movie)
    }
}
