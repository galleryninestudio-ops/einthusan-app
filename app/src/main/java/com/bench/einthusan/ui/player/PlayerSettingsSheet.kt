package com.bench.einthusan.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bench.einthusan.ui.pressScale
import com.bench.einthusan.ui.theme.GlassLineStrong
import com.bench.einthusan.ui.theme.TextHi
import com.bench.einthusan.ui.theme.TextMid

/** Quality, picture fit and playback speed. */
@Composable
internal fun PlayerSheet(
    resizeMode: Int,
    speed: Float,
    qualityOptions: List<Pair<Boolean, String>>,
    ultraHd: Boolean,
    qualityDetail: String,
    onQuality: (Boolean) -> Unit,
    onResize: (Int) -> Unit,
    onSpeed: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val aspects = listOf(0 to "Fit", 1 to "Stretch", 2 to "Crop", 3 to "Fill")
    val speeds = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)

    Column(
        modifier
            .padding(horizontal = 16.dp)
            .width(340.dp)
            .heightIn(max = 300.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(GlassDeep)
            .border(1.dp, GlassLineStrong, RoundedCornerShape(24.dp))
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("QUALITY", style = MaterialTheme.typography.labelSmall, color = TextMid)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            qualityOptions.forEach { (uhd, label) ->
                val active = uhd == ultraHd
                Box(
                    Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                        .background(if (active) Color.White else Color(0x14FFFFFF))
                        .pressScale { onQuality(uhd) }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(label, style = MaterialTheme.typography.labelSmall,
                        color = if (active) Color(0xFF0B0B0B) else TextHi)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(qualityDetail, style = MaterialTheme.typography.bodySmall, color = TextMid)
        Spacer(Modifier.height(16.dp))
        Text(
            "PICTURE",
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.8.sp),
            color = TextMid,
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            aspects.forEach { (mode, label) ->
                val active = mode == resizeMode
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (active) Color.White else Color(0x14FFFFFF))
                        .pressScale { onResize(mode) }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 0.2.sp),
                        color = if (active) Color(0xFF0B0B0B) else TextHi,
                        maxLines = 1,
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            "SPEED",
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.8.sp),
            color = TextMid,
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            speeds.forEach { v ->
                val active = kotlin.math.abs(v - speed) < 0.01f
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (active) Color.White else Color(0x14FFFFFF))
                        .pressScale { onSpeed(v) }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (v == 1f) "1x" else "${v}x",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 0.2.sp),
                        color = if (active) Color(0xFF0B0B0B) else TextHi,
                        maxLines = 1,
                    )
                }
            }
        }

    }
}

