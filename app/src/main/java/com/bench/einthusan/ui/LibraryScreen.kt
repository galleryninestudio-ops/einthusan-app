package com.bench.einthusan.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.bench.einthusan.download.DlEngine
import com.bench.einthusan.download.DlStatus
import com.bench.einthusan.download.DlStore
import com.bench.einthusan.download.DlTask
import com.bench.einthusan.download.DownloadFiles
import com.bench.einthusan.download.DownloadService
import com.bench.einthusan.parse.Movie
import com.bench.einthusan.ui.theme.Display
import com.bench.einthusan.ui.theme.Ink
import com.bench.einthusan.ui.theme.TextHi
import com.bench.einthusan.ui.theme.TextLow
import com.bench.einthusan.ui.theme.TextMid
import java.util.Locale
import kotlinx.coroutines.delay

/**
 * Library: the films you kept (saved) and the films on the device (downloads).
 * Downloads are chunked transfers with a foreground service behind them, so the
 * numbers here are live bytes, not a spinner.
 */
@Composable
fun LibraryScreen(app: AppState, nav: Nav) {
    val context = LocalContext.current
    var tab by remember { mutableIntStateOf(0) }
    var tasks by remember { mutableStateOf(DlStore.all(context)) }
    val progress by DlStore.progress.collectAsState()

    // Saved reads through the library revision, so unsaving anywhere is
    // reflected here without reopening the screen.
    val libraryRevision by app.library.changes.collectAsState()
    val saved = remember(libraryRevision) { app.library.saved() }

    LaunchedEffect(tab) {
        if (tab == 1) tasks = DlStore.all(context)
    }

    // Refresh the list while anything is moving; progress itself is a StateFlow.
    LaunchedEffect(tab, tasks.map { it.status }) {
        if (tab != 1) return@LaunchedEffect
        while (tasks.any { DlStore.isActive(it) }) {
            tasks = DlStore.all(context)
            delay(900)
        }
        tasks = DlStore.all(context)
    }

    Column(Modifier.fillMaxSize().background(Ink)) {
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Library",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontFamily = Display,
                    fontWeight = FontWeight.Black,
                    fontSize = 34.sp,
                ),
                color = TextHi,
            )
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Seg("Saved", tab == 0, saved.size) { tab = 0 }
            Seg("Downloads", tab == 1, tasks.size) { tab = 1 }
        }

        Spacer(Modifier.height(14.dp))

        if (tab == 0) {
            if (saved.isEmpty()) {
                EmptyShelf("Nothing saved yet", "Tap the bookmark on any film and it waits here.")
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 40.dp),
                    horizontalArrangement = Arrangement.spacedBy(11.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    items(saved, key = { it.id }) { m ->
                        PosterCard(
                            movie = m,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { nav.push(Route.Detail(m)) },
                        )
                    }
                }
            }
        } else {
            if (tasks.isEmpty()) {
                EmptyShelf("No downloads", "Open a film and tap Download. It lands in Movies/Einthusan.")
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 40.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(tasks, key = { it.id }) { task ->
                        val live = progress[task.id]
                        DownloadRow(
                            task = task,
                            bytes = live?.bytes ?: DlStore.bytesOnDisk(context, task),
                            total = live?.total ?: task.total,
                            speed = live?.speedBps ?: 0L,
                            onPause = {
                                DlEngine.cancel(task.id)
                                DlStore.put(context, task.copy(status = DlStatus.PAUSED))
                                tasks = DlStore.all(context)
                            },
                            onResume = {
                                DlStore.put(context, task.copy(status = DlStatus.QUEUED))
                                DownloadService.start(context)
                                tasks = DlStore.all(context)
                            },
                            onDelete = {
                                DlEngine.cancel(task.id)
                                DownloadFiles.delete(context, task)
                                DlStore.remove(context, task.id)
                                tasks = DlStore.all(context)
                            },
                            onPlay = {
                                val file = DownloadFiles.playUri(task)
                                if (file != null) {
                                    val movie = Movie(
                                        id = task.movieId,
                                        title = task.title,
                                        year = "",
                                        lang = app.library.lastLang,
                                        poster = task.poster,
                                        uhd = false,
                                        subtitle = false,
                                        synopsis = null,
                                        badge = null,
                                        playable = true,
                                    )
                                    nav.push(Route.Player(movie, file))
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Seg(text: String, active: Boolean, count: Int, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (active) Color.White else Color(0x0FFFFFFF))
            .border(1.dp, if (active) Color.White else Color(0x1FFFFFFF), RoundedCornerShape(50))
            .pressScale(onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = if (active) Color(0xFF0B0B0B) else TextMid,
        )
        if (count > 0) {
            Spacer(Modifier.width(7.dp))
            Text(
                count.toString(),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = if (active) Color(0xFF0B0B0B).copy(alpha = 0.55f) else TextLow,
            )
        }
    }
}

@Composable
private fun DownloadRow(
    task: DlTask,
    bytes: Long,
    total: Long,
    speed: Long,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onDelete: () -> Unit,
    onPlay: () -> Unit,
) {
    val fraction = if (total > 0L) (bytes.toFloat() / total).coerceIn(0f, 1f) else 0f

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x0AFFFFFF))
            .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(16.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(52.dp)
                .height(78.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF1A1C21))
        ) {
            if (task.poster != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(task.poster)
                        .crossfade(180)
                        .build(),
                    contentDescription = task.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(
                task.title,
                style = MaterialTheme.typography.titleMedium,
                color = TextHi,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                line(task, bytes, total, speed, fraction),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                color = when (task.status) {
                    DlStatus.DONE -> TextHi
                    DlStatus.FAILED -> Color(0xFFEF7865)
                    else -> TextLow
                },
            )
            if (task.status != DlStatus.FAILED) {
                Spacer(Modifier.height(7.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0x26FFFFFF))
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(if (task.status == DlStatus.DONE) 1f else fraction)
                            .height(4.dp)
                            .background(Color.White)
                    )
                }
            }
        }

        Spacer(Modifier.width(10.dp))

        when (task.status) {
            DlStatus.DONE -> Pill("Play", selected = true, onClick = onPlay)
            DlStatus.RUNNING -> RoundIcon(onClick = onPause) { PauseGlyph() }
            DlStatus.QUEUED -> RoundIcon(onClick = onPause) { PauseGlyph() }
            else -> RoundIcon(onClick = onResume) { PlayGlyph() }
        }
        Spacer(Modifier.width(8.dp))
        RoundIcon(onClick = onDelete) { CrossGlyph() }
    }
}

@Composable
private fun RoundIcon(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color(0x14FFFFFF))
            .border(1.dp, Color(0x1FFFFFFF), CircleShape)
            .pressScale(onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun PauseGlyph() {
    Canvas(Modifier.size(12.dp)) {
        drawRoundRect(
            Color.White,
            topLeft = Offset(size.width * 0.16f, 0f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.24f, size.height),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.5f, 1.5f),
        )
        drawRoundRect(
            Color.White,
            topLeft = Offset(size.width * 0.60f, 0f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.24f, size.height),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.5f, 1.5f),
        )
    }
}

@Composable
private fun PlayGlyph() {
    Canvas(Modifier.size(12.dp)) {
        val p = androidx.compose.ui.graphics.Path().apply {
            moveTo(0f, 0f)
            lineTo(size.width, size.height / 2f)
            lineTo(0f, size.height)
            close()
        }
        drawPath(p, Color.White)
    }
}

@Composable
private fun CrossGlyph() {
    Canvas(Modifier.size(11.dp)) {
        drawLine(
            Color.White,
            start = Offset(0f, 0f),
            end = Offset(size.width, size.height),
            strokeWidth = 1.8f,
            cap = StrokeCap.Round,
        )
        drawLine(
            Color.White,
            start = Offset(size.width, 0f),
            end = Offset(0f, size.height),
            strokeWidth = 1.8f,
            cap = StrokeCap.Round,
        )
    }
}

private fun line(task: DlTask, bytes: Long, total: Long, speed: Long, fraction: Float): String {
    return when (task.status) {
        DlStatus.DONE -> "Downloaded · ${sizeLabel(total)}"
        DlStatus.FAILED -> task.error?.let { "Failed · $it" } ?: "Failed"
        DlStatus.PAUSED -> "Paused · ${(fraction * 100).toInt()}% · resume to continue"
        DlStatus.QUEUED -> "Waiting to start…"
        DlStatus.RUNNING -> {
            val pct = (fraction * 100).toInt()
            val mbps = if (speed > 0L) String.format(Locale.US, " · %.1f MB/s", speed / 1048576.0) else ""
            if (total > 0L) "$pct% · ${sizeLabel(bytes)} of ${sizeLabel(total)}$mbps"
            else "Connecting…$mbps"
        }
    }
}

private fun sizeLabel(bytes: Long): String {
    if (bytes <= 0L) return "—"
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1024) String.format(Locale.US, "%.2f GB", mb / 1024) else String.format(Locale.US, "%.0f MB", mb)
}

@Composable
private fun EmptyShelf(title: String, body: String) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .size(92.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(Brush.linearGradient(listOf(Color(0x1AFFFFFF), Color(0x05FFFFFF))))
                .border(1.dp, Color(0x1FFFFFFF), RoundedCornerShape(26.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(BenchIcons.Bookmark, null, tint = TextMid, modifier = Modifier.size(34.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium, color = TextHi)
        Spacer(Modifier.height(8.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = TextLow)
    }
}
