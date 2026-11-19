package com.bench.einthusan.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.bench.einthusan.data.Library
import com.bench.einthusan.data.Repo
import com.bench.einthusan.parse.Movie
import com.bench.einthusan.ui.theme.Ink
import com.bench.einthusan.ui.theme.Surface2
import com.bench.einthusan.ui.theme.TextHi
import com.bench.einthusan.ui.theme.TextLow
import com.bench.einthusan.ui.theme.TextMid
import com.bench.einthusan.ui.theme.langOf
import com.bench.einthusan.util.suspendResult
import kotlinx.coroutines.flow.distinctUntilChanged

private data class Mode(
    val label: String,
    val find: String? = null,
    val alpha: String? = null,
    val decade: String? = null,
)

private val MODES = listOf(
    Mode("Recent", find = "Recent"),
    Mode("Staff", find = "StaffPick"),
    Mode("Regional", find = "RegionalHit"),
    Mode("A–Z", find = "Alphabets", alpha = "A"),
    Mode("Years", find = "Decade", decade = "2010"),
)

@OptIn(
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    ExperimentalFoundationApi::class,
)
@Composable
fun BrowseScreen(app: AppState, nav: Nav, langKey: String) {
    val lang = langOf(langKey)
    val gridState = rememberLazyGridState()

    var mode by remember { mutableStateOf(MODES[0]) }
    var alpha by remember { mutableStateOf("A") }
    var decade by remember { mutableStateOf("2010") }

    val movies = remember { mutableStateListOf<Movie>() }
    var page by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf(false) }
    var sheetFor by remember { mutableStateOf<Library.Progress?>(null) }

    suspend fun loadNext() {
        if (loading || done) return
        loading = true
        val next = page + 1
        val m = mode
        val paged = suspendResult {
            Repo.results(
                lang = langKey,
                find = m.find,
                alpha = if (m.alpha != null) alpha else null,
                decade = if (m.decade != null) decade else null,
                page = next,
            )
        }.getOrNull()

        if (paged == null || paged.movies.isEmpty()) {
            done = true
        } else {
            movies.addAll(paged.movies.filterNot { candidate -> movies.any { it.id == candidate.id } })
            page = next
            if (next >= paged.totalPages) done = true
        }
        loading = false
    }

    LaunchedEffect(mode, alpha, decade) {
        movies.clear(); page = 0; done = false; loading = false
        gridState.scrollToItem(0)
        loadNext()

        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .distinctUntilChanged()
            .collect { last ->
                if (last >= movies.size - 5) loadNext()
            }
    }

    Box(Modifier.fillMaxSize().background(Ink)) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    lang.label,
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextHi,
                )
                Text(
                    "${lang.script}  ·  " + when {
                        loading && movies.isEmpty() -> "loading"
                        movies.isEmpty() -> "nothing here"
                        else -> "${movies.size} films"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextLow,
                )
            }
        }

        // All five filters on one line: nothing hides off the edge of the screen.
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            MODES.forEach { m ->
                val active = m == mode
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .background(if (active) Color.White else Color(0x0FFFFFFF))
                        .border(
                            1.dp,
                            if (active) Color.White else Color(0x1FFFFFFF),
                            RoundedCornerShape(50),
                        )
                        .pressScale {
                            mode = m
                            if (m.alpha != null) alpha = "A"
                            if (m.decade != null) decade = "2010"
                        }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        m.label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            letterSpacing = 0.4.sp,
                        ),
                        color = if (active) Color(0xFF0B0B0B) else TextMid,
                        maxLines = 1,
                    )
                }
            }
        }

        if (mode.alpha != null) {
            Spacer(Modifier.height(10.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(('A'..'Z').map { it.toString() }) { letter ->
                    Pill(letter, selected = letter == alpha) { alpha = letter }
                }
            }
        }

        if (mode.decade != null) {
            Spacer(Modifier.height(10.dp))
            // Every decade on screen at once: no hidden selection off the right edge.
            androidx.compose.foundation.layout.FlowRow(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                listOf("1940", "1950", "1960", "1970", "1980", "1990", "2000", "2010", "2020").forEach { d ->
                    Pill(d, selected = d == decade) { decade = d }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Films already started in this language, pulled fresh each time the screen opens.
        val libraryRevision by app.library.changes.collectAsState()
        val resumeRow = remember(langKey, libraryRevision) {
            app.library.continueWatching().filter { it.movie.lang.equals(langKey, true) }
        }

        if (movies.isEmpty() && loading) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(11.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                items(12) { Shimmer(Modifier.fillMaxWidth().aspectRatio(2f / 3f)) }
            }
        } else {
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize().navigationBarsPadding(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 40.dp),
                horizontalArrangement = Arrangement.spacedBy(11.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                if (resumeRow.isNotEmpty() && mode == MODES.first()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        ContinueWatching(
                            items = resumeRow,
                            onOpen = { nav.push(Route.Player(it)) },
                            onHold = { sheetFor = it },
                        )
                    }
                }
                items(movies, key = { it.id }) { m ->
                    PosterCard(
                        movie = m,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { nav.push(Route.Detail(m)) },
                    )
                }
                if (loading) {
                    items(3) { Shimmer(Modifier.fillMaxWidth().aspectRatio(2f / 3f)) }
                }
                if (!loading && movies.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Column(
                            Modifier.fillMaxWidth().padding(top = 60.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                if (mode.alpha != null) "Nothing under $alpha in ${lang.label}"
                                else if (mode.decade != null) "Nothing from the ${decade}s in ${lang.label}"
                                else "Nothing here yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextLow,
                            )
                        }
                    }
                }
            }
        }
    }

        // Long-press on a resume card: the actions that used to live on a tiny X.
        sheetFor?.let { p ->
            val saved = app.library.isSaved(p.movie.id)
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color(0xD9000000))
                    .pointerInput(Unit) { detectTapGestures { sheetFor = null } },
            )
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 14.dp)
                    .padding(bottom = 14.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF101216))
                    .border(1.dp, Color(0x1FFFFFFF), RoundedCornerShape(24.dp))
                    .padding(vertical = 8.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        p.movie.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextHi,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        leftLabel(p.leftMs),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextLow,
                    )
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x14FFFFFF)))

                SheetRow("Play from where you stopped") {
                    sheetFor = null
                    nav.push(Route.Player(p.movie))
                }
                SheetRow("Details") {
                    sheetFor = null
                    nav.push(Route.Detail(p.movie))
                }
                SheetRow(if (saved) "Remove from saved" else "Add to saved") {
                    app.library.toggle(p.movie)
                    sheetFor = null
                }
                SheetRow("Remove from Continue watching", danger = true) {
                    app.library.clearProgress(p.movie.id)
                    sheetFor = null
                }
            }
        }

    }
}

/**
 * Resume row for this language: the films already started, the exact place you
 * stopped, one tap from playing.
 */
@Composable
private fun ContinueWatching(
    items: List<Library.Progress>,
    onOpen: (Movie) -> Unit,
    onHold: (Library.Progress) -> Unit,
) {
    Column(Modifier.padding(bottom = 6.dp)) {
        Text(
            "CONTINUE WATCHING",
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp),
            color = TextMid,
            modifier = Modifier.padding(bottom = 10.dp),
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(items, key = { it.movie.id }) { p ->
                Column(
                    Modifier
                        .width(112.dp)
                        .combinedClickable(
                            onClick = { onOpen(p.movie) },
                            onLongClick = { onHold(p) },
                        )
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(158.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Surface2)
                    ) {
                        if (p.movie.poster != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(p.movie.poster)
                                    .crossfade(200)
                                    .build(),
                                contentDescription = p.movie.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            // Deep-linked or half-known entries still get a tile.
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color(0xFF1C1E23), Color(0xFF101115))
                                        )
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    p.movie.title.take(1).uppercase(),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = TextMid,
                                )
                            }
                        }
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        0.62f to Color.Transparent,
                                        1f to Color(0x99000000),
                                    )
                                )
                        )
                        Box(
                            Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(3.dp)
                                .background(Color(0x59FFFFFF))
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth(p.fraction)
                                    .height(3.dp)
                                    .background(Color.White)
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        p.movie.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 13.sp),
                        color = TextHi,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        leftLabel(p.leftMs),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = TextLow,
                    )
                }
            }
        }
    }
}

private fun leftLabel(ms: Long): String {
    val mins = ms / 60_000L
    return when {
        mins <= 0L -> "Almost done"
        mins < 60L -> "$mins min left"
        else -> "${mins / 60} h ${mins % 60} min left"
    }
}

@Composable
private fun SheetRow(label: String, danger: Boolean = false, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .pressScale(onClick)
            .padding(horizontal = 18.dp, vertical = 15.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleMedium,
            color = if (danger) Color(0xFFEF7865) else TextHi,
        )
    }
}
