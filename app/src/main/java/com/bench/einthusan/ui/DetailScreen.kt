package com.bench.einthusan.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.bench.einthusan.data.MediaCache
import com.bench.einthusan.data.Repo
import com.bench.einthusan.download.DlEngine
import com.bench.einthusan.download.DlStatus
import com.bench.einthusan.download.DlStore
import com.bench.einthusan.parse.Movie
import com.bench.einthusan.ui.theme.Accent
import com.bench.einthusan.ui.theme.Display
import com.bench.einthusan.ui.theme.Gold
import com.bench.einthusan.ui.theme.Ink
import com.bench.einthusan.ui.theme.Stroke
import com.bench.einthusan.ui.theme.TextHi
import com.bench.einthusan.ui.theme.TextLow
import com.bench.einthusan.ui.theme.TextMid
import com.bench.einthusan.ui.theme.langOf
import com.bench.einthusan.util.suspendResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * One film, top to bottom: a still, the name, one obvious button, then the
 * words. No chips, no banners, no tier copy — the poster and the play button
 * carry the whole page.
 */
@Composable
fun DetailScreen(app: AppState, nav: Nav, movie: Movie) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val langKey = remember(movie) {
        val probe = movie.lang.lowercase()
        if (probe.isNotBlank() && probe.all { it.isLetter() }) probe else app.library.lastLang
    }
    val lang = langOf(langKey)

    var saved by remember { mutableStateOf(app.library.isSaved(movie.id)) }
    var busy by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    var expanded by remember { mutableStateOf(false) }
    var dlTask by remember(movie.id) { mutableStateOf(DlStore.findForMovie(context, movie.id)) }
    val dlProgress by DlStore.progress.collectAsState()
    val resumeAt = remember { app.library.position(movie.id) }

    LaunchedEffect(dlTask?.status) {
        while (dlTask != null && (DlStore.isActive(dlTask!!) )) {
            delay(600)
            dlTask = DlStore.findForMovie(context, movie.id)
        }
        dlTask = DlStore.findForMovie(context, movie.id)
    }

    // Warm the signed stream URL and its first megabytes while the poster is
    // still decoding, so Play lands on prepared, local bytes.
    LaunchedEffect(movie.id, langKey) {
        val info = suspendResult {
            Repo.watch(movie.id, langKey, uhd = app.wantsUhd(movie.uhd))
        }.getOrNull()
        app.settings.preloadBytes.takeIf { it > 0 }?.let { budget ->
            MediaCache.warm(context, info?.mp4 ?: info?.hls, budget)
        }
    }

    Box(Modifier.fillMaxSize().background(Ink)) {

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 40.dp),
        ) {
            item {
                Box(Modifier.fillMaxWidth().height(430.dp)) {
                    AsyncImage(
                        model = movie.poster,
                        contentDescription = movie.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    0f to Color(0x66000000),
                                    0.5f to Color(0x1A000000),
                                    0.86f to Ink,
                                    1f to Ink,
                                )
                            )
                    )
                }
            }

            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 4.dp)
                ) {
                    Text(
                        movie.title,
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontFamily = Display,
                            fontSize = 30.sp,
                            lineHeight = 34.sp,
                        ),
                        color = TextHi,
                    )

                    Spacer(Modifier.height(10.dp))
                    Text(
                        listOfNotNull(
                            movie.year.takeIf { it.isNotBlank() },
                            lang.label,
                            "Premium HD".takeIf { movie.uhd },
                            "Subtitled".takeIf { movie.subtitle },
                        ).joinToString("   ·   "),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                        color = TextMid,
                    )

                    Spacer(Modifier.height(22.dp))

                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .pressScale { nav.push(Route.Player(movie)) },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            Icons.Filled.PlayArrow, null,
                            tint = Color(0xFF0B0B0B), modifier = Modifier.size(26.dp),
                        )
                        Spacer(Modifier.width(7.dp))
                        Text(
                            if (resumeAt > 0) "Resume  ${formatMs(resumeAt)}" else "Play",
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                            color = Color(0xFF0B0B0B),
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GhostAction(
                            label = if (saved) "Saved" else "Save",
                            active = saved,
                            modifier = Modifier.weight(1f),
                            onClick = { saved = app.library.toggle(movie) },
                        )

                        val live = dlTask?.let { dlProgress[it.id] }
                        val fraction = when {
                            dlTask?.status == DlStatus.DONE -> 1f
                            live != null && live.total > 0L -> live.fraction
                            dlTask?.status == DlStatus.RUNNING -> 0f
                            else -> null
                        }
                        GhostAction(
                            label = when (dlTask?.status) {
                                DlStatus.DONE -> "Downloaded"
                                DlStatus.RUNNING, DlStatus.QUEUED ->
                                    "${((fraction ?: 0f) * 100).toInt()}%"
                                DlStatus.PAUSED, DlStatus.FAILED -> "Resume"
                                else -> "Download"
                            },
                            active = dlTask?.status == DlStatus.DONE,
                            busy = dlTask?.status == DlStatus.RUNNING || dlTask?.status == DlStatus.QUEUED,
                            fraction = fraction,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                val task = dlTask
                                when (task?.status) {
                                    DlStatus.DONE -> nav.push(Route.Saved)
                                    DlStatus.RUNNING, DlStatus.QUEUED -> {
                                        DlEngine.cancel(task.id)
                                        DlStore.put(context, task.copy(status = DlStatus.PAUSED))
                                        dlTask = DlStore.findForMovie(context, movie.id)
                                    }
                                    DlStatus.PAUSED, DlStatus.FAILED -> {
                                        DlStore.put(context, task.copy(status = DlStatus.QUEUED, error = null))
                                        com.bench.einthusan.download.DownloadService.start(context)
                                        dlTask = DlStore.findForMovie(context, movie.id)
                                    }
                                    else -> {
                                        busy = true
                                        notice = null
                                        scope.launch {
                                            val info = suspendResult {
                                                Repo.watch(movie.id, langKey, uhd = app.wantsUhd(movie.uhd))
                                            }.getOrNull()
                                            val url = info?.mp4 ?: info?.hls
                                            if (url != null) {
                                                DlStore.enqueue(
                                                    context = context,
                                                    movieId = movie.id,
                                                    title = movie.title,
                                                    poster = movie.poster,
                                                    url = url,
                                                )
                                                com.bench.einthusan.download.DownloadService.start(context)
                                                dlTask = DlStore.findForMovie(context, movie.id)
                                                notice = null
                                            } else {
                                                notice = "This title has no stream right now."
                                            }
                                            busy = false
                                        }
                                    }
                                }
                            },
                        )
                    }

                    AnimatedVisibility(visible = notice != null, enter = fadeIn(), exit = fadeOut()) {
                        Text(
                            notice.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMid,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }

                    if (movie.uhd && !app.premium) {
                        Spacer(Modifier.height(18.dp))
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0x14F5D45A))
                                .pressScale { nav.push(Route.Login) }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(BenchIcons.Spark, null, tint = Gold, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Premium HD available",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextHi,
                                )
                                Text(
                                    "Sign in with Premium. Resolution varies by title.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMid,
                                )
                            }
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight, null,
                                tint = Gold.copy(alpha = 0.8f), modifier = Modifier.size(18.dp),
                            )
                        }
                    }

                    if (!movie.synopsis.isNullOrBlank()) {
                        Spacer(Modifier.height(26.dp))
                        SectionLabel("Story")
                        Spacer(Modifier.height(10.dp))
                        Text(
                            movie.synopsis,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.5.sp, lineHeight = 23.sp),
                            color = TextMid,
                            maxLines = if (expanded) Int.MAX_VALUE else 4,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (movie.synopsis.length > 180) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                if (expanded) "Less" else "More",
                                style = MaterialTheme.typography.labelLarge,
                                color = Accent,
                                modifier = Modifier.pressScale { expanded = !expanded },
                            )
                        }
                    }

                    Spacer(Modifier.height(26.dp))
                    SectionLabel("Details")
                    Spacer(Modifier.height(4.dp))

                    DetailRow("Language", lang.label)
                    DetailRow("Year", movie.year.ifBlank { "—" })
                    DetailRow("Quality", if (movie.uhd) "HD with Premium" else "Standard")
                    DetailRow("Subtitles", if (movie.subtitle) "Available" else "None")
                    if (resumeAt > 0) DetailRow("Last watched", formatMs(resumeAt))

                    Spacer(Modifier.height(30.dp))
                }
            }
        }

    }
}

@Composable
private fun GhostAction(
    label: String,
    active: Boolean,
    modifier: Modifier = Modifier,
    busy: Boolean = false,
    fraction: Float? = null,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .height(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x0FFFFFFF))
            .border(1.dp, if (active) Accent.copy(alpha = 0.5f) else Stroke, RoundedCornerShape(14.dp))
            .pressScale(onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (fraction != null) {
            // Ring, then the number beside it, and a fill along the bottom edge
            // so the state is readable even at a glance.
            Box(
                Modifier.matchParentSize().padding(horizontal = 1.dp, vertical = 1.dp),
                contentAlignment = Alignment.BottomStart,
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(fraction.coerceIn(0f, 1f))
                        .height(2.dp)
                        .background(Color(0x99FFFFFF))
                )
            }
            Text(
                label,
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp),
                color = Color.White,
            )
        } else if (busy) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = Color.White,
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    when (label) {
                        "Saved" -> Icons.Filled.Check
                        "Save" -> BenchIcons.Bookmark
                        else -> BenchIcons.Download
                    },
                    null,
                    tint = if (active) Accent else TextMid,
                    modifier = Modifier.size(17.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (active) Accent else TextMid,
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 2.sp),
        color = TextLow,
    )
}

@Composable
private fun DetailRow(key: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            key,
            style = MaterialTheme.typography.bodySmall,
            color = TextLow,
            modifier = Modifier.width(120.dp),
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
            color = TextHi,
        )
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x0FFFFFFF)))
}
