package com.bench.einthusan.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.bench.einthusan.R

/* ---------- base surface, darkened from their #0b0b0b / #3f3f3f ---------- */

val Ink = Color(0xFF0B0B0B)
val Ink2 = Color(0xFF121215)
val Surface1 = Color(0xFF17181C)
val Surface2 = Color(0xFF1F2126)
val Stroke = Color(0xFF2C3038)

val TextHi = Color(0xFFFFFFFF)
val TextMid = Color(0xFFA6ADB6)
val TextLow = Color(0xFF6A717A)

val Accent = Color(0xFFF4F5F7)
val Gold = Color(0xFFF5D45A)

/* ---------- official language gradients, lifted off their stylesheet ---------- */

data class Lang(
    val key: String,
    val label: String,
    val script: String,
    val top: Color,
    val bottom: Color,
) {
    /** Their rule is `linear-gradient(to left top, dark, light)`. */
    val brush: Brush get() = Brush.linearGradient(listOf(top, bottom))
}

val LANGS = listOf(
    Lang("tamil", "Tamil", "தமிழ்", Color(0xFFD83116), Color(0xFFEF7865)),
    Lang("hindi", "Hindi", "हिन्दी", Color(0xFF9E2E6F), Color(0xFFD161A2)),
    Lang("telugu", "Telugu", "తెలుగు", Color(0xFF12A4C5), Color(0xFF4ED1EF)),
    Lang("malayalam", "Malayalam", "മലയാളം", Color(0xFF6D8C20), Color(0xFFA9D33F)),
    Lang("kannada", "Kannada", "ಕನ್ನಡ", Color(0xFFDA3E98), Color(0xFFEA94C6)),
    Lang("bengali", "Bengali", "বাংলা", Color(0xFF237CD8), Color(0xFF78B0E9)),
    Lang("marathi", "Marathi", "मराठी", Color(0xFF0DAF64), Color(0xFF32F098)),
    Lang("punjabi", "Punjabi", "ਪੰਜਾਬੀ", Color(0xFFDCB00D), Color(0xFFF5D45A)),
)

fun langOf(key: String?): Lang = LANGS.firstOrNull { it.key == key } ?: LANGS[0]

/* ---------- white glass ----------
 * Chrome is monochrome: translucent white fills, hairline white borders, solid
 * white for the one primary action on a screen. Colour belongs to the content —
 * posters and the eight category tiles — never to the interface around them.
 */

val Glass = Color(0x14FFFFFF)
val GlassLineStrong = Color(0x33FFFFFF)

/* ---------- type ---------- */
/*
 * Both faces are the variable TTFs the site loads from Google Fonts
 * (Roboto 300-900, Raleway 200/400/900). One file per family, the weight axis
 * is applied by the platform on each Font() entry.
 */

private fun roboto(w: Int) = Font(
    R.font.roboto,
    FontWeight(w),
    variationSettings = FontVariation.Settings(FontVariation.weight(w)),
)

private fun raleway(w: Int) = Font(
    R.font.raleway,
    FontWeight(w),
    variationSettings = FontVariation.Settings(FontVariation.weight(w)),
)

private val Roboto = FontFamily(
    roboto(300), roboto(400), roboto(500), roboto(600), roboto(700), roboto(900),
)

private val Raleway = FontFamily(
    raleway(200), raleway(300), raleway(400), raleway(700), raleway(900),
)

val Display = Raleway

val BenchType = Typography(
    displayLarge = TextStyle(
        fontFamily = Raleway, fontWeight = FontWeight.Black,
        fontSize = 40.sp, lineHeight = 42.sp, letterSpacing = (-1.4).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = Raleway, fontWeight = FontWeight.Black,
        fontSize = 24.sp, lineHeight = 28.sp, letterSpacing = (-0.6).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = Roboto, fontWeight = FontWeight.Medium,
        fontSize = 18.sp, lineHeight = 23.sp, letterSpacing = (-0.35).sp,
    ),
    titleMedium = TextStyle(
        fontFamily = Roboto, fontWeight = FontWeight.Medium,
        fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = (-0.15).sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = Roboto, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 21.sp, letterSpacing = 0.05.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = Roboto, fontWeight = FontWeight.Normal,
        fontSize = 12.5.sp, lineHeight = 18.sp, letterSpacing = 0.1.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = Raleway, fontWeight = FontWeight.Bold,
        fontSize = 13.sp, lineHeight = 16.sp, letterSpacing = 0.6.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = Raleway, fontWeight = FontWeight.Black,
        fontSize = 10.sp, lineHeight = 14.sp, letterSpacing = 1.4.sp,
    ),
)

private val BenchScheme = darkColorScheme(
    primary = Accent,
    onPrimary = Color(0xFF1A0A06),
    secondary = Gold,
    background = Ink,
    onBackground = TextHi,
    surface = Surface1,
    onSurface = TextHi,
    surfaceVariant = Surface2,
    onSurfaceVariant = TextMid,
    outline = Stroke,
)

@Composable
fun BenchTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BenchScheme,
        typography = BenchType,
        content = content,
    )
}

