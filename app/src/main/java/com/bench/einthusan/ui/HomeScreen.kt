package com.bench.einthusan.ui

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.bench.einthusan.R
import com.bench.einthusan.data.Library
import com.bench.einthusan.data.Repo
import com.bench.einthusan.download.DlStore
import com.bench.einthusan.ui.theme.Accent
import com.bench.einthusan.ui.theme.Display
import com.bench.einthusan.ui.theme.LANGS
import com.bench.einthusan.ui.theme.Lang
import com.bench.einthusan.ui.theme.Surface2
import com.bench.einthusan.ui.theme.TextHi

/**
 * The intro page, and nothing else: wordmark, the line, the eight languages.
 * Their home is a front door, not a feed — the films live one tap in, behind
 * the language you picked, which is also why this screen never hits the
 * listings endpoint and opens instantly.
 */
@Composable
fun HomeScreen(app: AppState, nav: Nav) {
    Box(Modifier.fillMaxSize().background(Color(0xFF0A0B0D))) {
        Image(
            painter = painterResource(R.drawable.bg_fusion),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color(0x3D000000),
                        0.42f to Color(0x26000000),
                        0.78f to Color(0xB30A0B0D),
                        1f to Color(0xFF0A0B0D),
                    )
                )
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 40.dp),
        ) {
            item { TopActions(app, nav) }

            item {
                Spacer(Modifier.height(30.dp))
                Image(
                    painter = painterResource(R.drawable.ic_logo),
                    contentDescription = "Einthusan",
                    modifier = Modifier
                        .padding(horizontal = 74.dp)
                        .fillMaxWidth()
                        .height(46.dp),
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "the largest collection in history of",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = Display,
                        fontWeight = FontWeight.ExtraLight,
                        fontSize = 15.sp,
                        letterSpacing = 0.2.sp,
                    ),
                    color = Color(0xE6FFFFFF),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "SOUTH ASIAN FILMS",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = Display,
                        fontWeight = FontWeight.Black,
                        fontSize = 25.sp,
                        letterSpacing = 0.3.sp,
                    ),
                    color = TextHi,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(30.dp))
            }

            item {
                LanguageGrid { picked ->
                    app.library.lastLang = picked.key
                    nav.push(Route.Browse(picked.key))
                }
            }

        }
    }
}

@Composable
private fun TopActions(app: AppState, nav: Nav) {
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.weight(1f))
        GlassButton(onClick = { nav.push(Route.Search) }) {
            Icon(Icons.Filled.Search, "Search", tint = TextHi, modifier = Modifier.size(19.dp))
        }
        Spacer(Modifier.width(8.dp))
        val moving by DlStore.progress.collectAsState()
        val fraction = moving.values.maxOfOrNull { it.fraction } ?: 0f
        GlassButton(onClick = { nav.push(Route.Saved) }, progress = if (moving.isEmpty()) null else fraction) {
            Icon(BenchIcons.Library, "Library", tint = TextHi, modifier = Modifier.size(19.dp))
        }
        Spacer(Modifier.width(8.dp))
        GlassButton(onClick = { nav.push(Route.Login) }) {
            Icon(
                Icons.Filled.Person, "Account",
                tint = if (app.premium) Accent else TextHi,
                modifier = Modifier.size(19.dp),
            )
        }

    }
}

@Composable
private fun GlassButton(
    onClick: () -> Unit,
    progress: Float? = null,
    icon: @Composable () -> Unit,
) {
    Box(Modifier.size(42.dp), contentAlignment = Alignment.Center) {
        if (progress != null) {
            CircularProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.size(42.dp),
                strokeWidth = 2.dp,
                color = Color.White,
                trackColor = Color(0x33FFFFFF),
            )
        }
        Box(
            Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Color(0x40000000))
                .border(1.dp, Color(0x33FFFFFF), CircleShape)
                .pressScale(onClick),
            contentAlignment = Alignment.Center,
        ) { icon() }
    }
}

@Composable
private fun LanguageGrid(onPick: (Lang) -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp)) {
        LANGS.chunked(2).forEachIndexed { rowIdx, pair ->
            Row(
                Modifier.fillMaxWidth().padding(bottom = 11.dp),
                horizontalArrangement = Arrangement.spacedBy(11.dp),
            ) {
                pair.forEachIndexed { i, lang ->
                    Stagger(
                        index = rowIdx * 2 + i,
                        distance = 18f,
                        modifier = Modifier.weight(1f),
                    ) {
                        LanguageTile(lang) { onPick(lang) }
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/** Their tile: a still on top, the coloured gradient bar under it, noise over it. */
@Composable
private fun LanguageTile(lang: Lang, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Surface2)
            .pressScale(onClick)
    ) {
        Box(Modifier.fillMaxWidth().height(86.dp)) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(Repo.tileArt(lang.key))
                    .crossfade(220)
                    .build(),
                contentDescription = lang.label,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(0f to Color(0x14000000), 1f to Color(0x4D000000))
                    )
            )
        }

        Box(
            Modifier
                .fillMaxWidth()
                .height(28.dp)
                .background(lang.brush),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.tile_noise),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alpha = 0.45f,
                modifier = Modifier.matchParentSize(),
            )
            Text(
                lang.label.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    letterSpacing = 1.9.sp,
                ),
                color = Color.White,
            )
        }
    }
}
