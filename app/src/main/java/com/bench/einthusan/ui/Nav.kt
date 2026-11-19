package com.bench.einthusan.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import com.bench.einthusan.parse.Movie
import com.bench.einthusan.ui.player.PlayerScreen

sealed interface Route {
    data object Home : Route
    data class Browse(val lang: String) : Route
    data object Search : Route
    data class Detail(val movie: Movie) : Route
    data class Player(val movie: Movie, val local: String? = null) : Route
    data object Login : Route
    data object Saved : Route
}

class Nav(private val stack: SnapshotStateList<Route>) {

    fun push(route: Route) {
        if (stack.lastOrNull() == route) return
        stack.add(route)
    }

    fun pop(): Boolean {
        if (stack.size <= 1) return false
        stack.removeAt(stack.lastIndex)
        return true
    }

}

/**
 * No in-app back affordance anywhere: the system gesture and the system button
 * are the way out, and they pop this stack.
 */
@Composable
fun AppRoot(app: AppState, link: State<Route?>, onLinkHandled: () -> Unit) {
    val stack = remember {
        mutableStateListOf<Route>(Route.Home)
    }
    val nav = remember { Nav(stack) }

    // A link that arrives while the app is already running lands on top of the
    // stack instead of being swallowed by an activity that never re-composed.
    val incoming by link
    LaunchedEffect(incoming) {
        val target = incoming ?: return@LaunchedEffect
        nav.push(target)
        onLinkHandled()
    }

    BackHandler(enabled = stack.size > 1) { nav.pop() }

    Box(Modifier.fillMaxSize().imePadding()) {
        AnimatedContent(
            targetState = stack.last(),
            transitionSpec = {
                (fadeIn(tween(240)) + slideInHorizontally(tween(300)) { it / 16 }) togetherWith fadeOut(tween(130))
            },
            modifier = Modifier.fillMaxSize(),
            label = "route",
        ) { route ->
            when (route) {
                is Route.Home -> HomeScreen(app, nav)
                is Route.Browse -> BrowseScreen(app, nav, route.lang)
                is Route.Search -> SearchScreen(app, nav)
                is Route.Detail -> DetailScreen(app, nav, route.movie)
                is Route.Player -> PlayerScreen(app, nav, route.movie, route.local)
                is Route.Login -> LoginScreen(app, nav)
                is Route.Saved -> LibraryScreen(app, nav)
            }
        }
    }
}
