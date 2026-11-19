package com.bench.einthusan.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import com.bench.einthusan.data.Repo
import com.bench.einthusan.parse.Movie
import com.bench.einthusan.ui.theme.Accent
import com.bench.einthusan.ui.theme.Ink
import com.bench.einthusan.ui.theme.Stroke
import com.bench.einthusan.ui.theme.Surface1
import com.bench.einthusan.ui.theme.TextHi
import com.bench.einthusan.ui.theme.TextLow
import com.bench.einthusan.ui.theme.TextMid
import com.bench.einthusan.util.suspendResult
import kotlinx.coroutines.delay

@Composable
fun SearchScreen(app: AppState, nav: Nav) {
    val lang = app.library.lastLang
    var q by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var searched by remember { mutableStateOf(false) }

    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(q) {
        if (q.isBlank()) {
            results = emptyList(); searched = false; loading = false; return@LaunchedEffect
        }
        delay(360)
        loading = true
        val r = suspendResult { Repo.results(lang = lang, query = q.trim(), page = 1) }.getOrNull()
        results = r?.movies.orEmpty()
        loading = false
        searched = true
    }

    Column(Modifier.fillMaxSize().background(Ink)) {
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {

            Row(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Surface1)
                    .border(1.dp, if (q.isNotEmpty()) Accent.copy(alpha = 0.5f) else Stroke, RoundedCornerShape(16.dp))
                    .padding(horizontal = 13.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Search, null, tint = TextLow, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(9.dp))
                Box(
                    Modifier
                        .weight(1f)
                        .pressScale { runCatching { focus.requestFocus() } }
                ) {
                    if (q.isEmpty()) {
                        Text(
                            "Search films, directors, music…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextLow,
                        )
                    }
                    BasicTextField(
                        value = q,
                        onValueChange = { q = it },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextHi),
                        cursorBrush = SolidColor(Accent),
                        modifier = Modifier.fillMaxWidth().focusRequester(focus),
                    )
                }
                if (q.isNotEmpty()) {
                    Icon(
                        Icons.Filled.Clear,
                        "Clear",
                        tint = TextMid,
                        modifier = Modifier.size(18.dp).pressScale { q = "" },
                    )
                }
            }
        }

        when {
            loading -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(11.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) { items(9) { Shimmer(Modifier.fillMaxWidth().aspectRatio(2f / 3f)) } }
            }

            results.isEmpty() && searched -> {
                Column(
                    Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        "Nothing for \u201C$q\u201D in $lang.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextLow,
                    )
                }
            }

            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize().navigationBarsPadding(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 40.dp),
                    horizontalArrangement = Arrangement.spacedBy(11.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    items(results, key = { it.id }) { m ->
                        PosterCard(
                            movie = m,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                keyboard?.hide()
                                nav.push(Route.Detail(m))
                            },
                        )
                    }
                }
            }
        }
    }
}
