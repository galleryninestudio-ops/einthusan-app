package com.bench.einthusan.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.bench.einthusan.parse.Movie
import com.bench.einthusan.ui.theme.Accent
import com.bench.einthusan.ui.theme.Gold
import com.bench.einthusan.ui.theme.Ink2
import com.bench.einthusan.ui.theme.Surface1
import com.bench.einthusan.ui.theme.Surface2
import com.bench.einthusan.ui.theme.TextHi
import com.bench.einthusan.ui.theme.TextLow
import com.bench.einthusan.ui.theme.TextMid
import kotlinx.coroutines.delay

/* ---------------- motion ---------------- */

@Composable
fun Modifier.pressScale(onClick: () -> Unit): Modifier {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.955f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 900f),
        label = "press",
    )
    return this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(interactionSource = source, indication = null, onClick = onClick)
}

@Composable
fun Stagger(
    index: Int,
    modifier: Modifier = Modifier,
    distance: Float = 34f,
    content: @Composable () -> Unit,
) {
    var on by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay((index.coerceAtMost(14)) * 42L)
        on = true
    }
    val p by animateFloatAsState(
        targetValue = if (on) 1f else 0f,
        animationSpec = tween(460, easing = FastOutSlowInEasing),
        label = "stagger",
    )
    Box(
        modifier.graphicsLayer {
            alpha = p
            translationY = (1f - p) * distance
            scaleX = 0.97f + 0.03f * p
            scaleY = 0.97f + 0.03f * p
        }
    ) { content() }
}

@Composable
fun Shimmer(modifier: Modifier = Modifier, shape: RoundedCornerShape = RoundedCornerShape(14.dp)) {
    val t = rememberInfiniteTransition(label = "shimmer")
    val x by t.animateFloat(
        initialValue = -400f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(tween(1250, easing = LinearEasing), RepeatMode.Restart),
        label = "x",
    )
    Box(
        modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(Surface1, Surface2, Surface1),
                    start = Offset(x, 0f),
                    end = Offset(x + 380f, 0f),
                )
            )
    )
}

/* ---------------- icons not in the core set ---------------- */

/* ---------------- chips, pills, bars ---------------- */

@Composable
fun Pill(
    text: String,
    selected: Boolean = false,
    accent: Color = Accent,
    onClick: (() -> Unit)? = null,
) {
    val bg = if (selected) Color.White else Color(0x0FFFFFFF)
    val fg = if (selected) Color(0xFF0B0B0B) else TextMid
    val border = if (selected) Color.White else Color(0x1FFFFFFF)

    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(50))
            .then(if (onClick != null) Modifier.pressScale(onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = fg,
            maxLines = 1,
        )
    }
}

@Composable
fun Badge(text: String, color: Color = Gold, solid: Boolean = false) {
    Box(
        Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (solid) color else color.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = if (solid) Ink2 else color,
            fontWeight = FontWeight.Bold,
        )
    }
}

/* ---------------- cards ---------------- */

@Composable
fun PosterCard(
    movie: Movie,
    modifier: Modifier = Modifier,
    showTitle: Boolean = true,
    onClick: () -> Unit,
) {
    Column(modifier.pressScale(onClick)) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(14.dp))
                .background(Surface1)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(movie.poster)
                    .crossfade(240)
                    .build(),
                contentDescription = movie.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.55f to Color.Transparent,
                            1f to Color(0xCC000000),
                        )
                    )
            )
            if (movie.uhd) {
                Box(Modifier.align(Alignment.TopEnd).padding(6.dp)) {
                    Badge("HD", Gold, solid = true)
                }
            }
            if (movie.badge != null) {
                Box(Modifier.align(Alignment.BottomStart).padding(6.dp)) {
                    Badge(movie.badge, Accent)
                }
            }
        }
        if (showTitle) {
            Text(
                movie.title,
                style = MaterialTheme.typography.titleMedium,
                color = TextHi,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 9.dp),
            )
            Text(
                listOf(movie.year, movie.lang).filter { it.isNotBlank() }.joinToString("  ·  "),
                style = MaterialTheme.typography.bodySmall,
                color = TextLow,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}
