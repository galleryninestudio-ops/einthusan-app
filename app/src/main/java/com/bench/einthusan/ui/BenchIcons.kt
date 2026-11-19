package com.bench.einthusan.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object BenchIcons {

    val Spark: ImageVector by lazy {
        ImageVector.Builder(
            name = "spark", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(12f, 2.4f)
                lineTo(14.1f, 9.2f)
                lineTo(21f, 11.3f)
                lineTo(14.1f, 13.4f)
                lineTo(12f, 20.2f)
                lineTo(9.9f, 13.4f)
                lineTo(3f, 11.3f)
                lineTo(9.9f, 9.2f)
                close()
            }
        }.build()
    }

    /** Bookmark + arrow: one door for saved films and downloaded ones. */
    val Library: ImageVector by lazy {
        ImageVector.Builder(
            name = "library", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            path(
                fill = null, stroke = SolidColor(Color.White), strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(6.5f, 4f); lineTo(17.5f, 4f)
                curveTo(18.1f, 4f, 18.5f, 4.4f, 18.5f, 5f)
                lineTo(18.5f, 20f); lineTo(12f, 15.4f); lineTo(5.5f, 20f)
                lineTo(5.5f, 5f)
                curveTo(5.5f, 4.4f, 5.9f, 4f, 6.5f, 4f)
                close()
            }
            path(
                fill = null, stroke = SolidColor(Color.White), strokeLineWidth = 1.7f,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(12f, 7.6f); lineTo(12f, 12.4f)
                moveTo(9.9f, 10.4f); lineTo(12f, 12.5f); lineTo(14.1f, 10.4f)
            }
        }.build()
    }

    val Lock: ImageVector by lazy {
        ImageVector.Builder(
            name = "lock", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            path(
                fill = null, stroke = SolidColor(Color.White), strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(6.5f, 10.5f); lineTo(17.5f, 10.5f); lineTo(17.5f, 20f); lineTo(6.5f, 20f); close()
                moveTo(8.8f, 10.5f); lineTo(8.8f, 7.6f)
                curveTo(8.8f, 5.8f, 10.2f, 4.2f, 12f, 4.2f)
                curveTo(13.8f, 4.2f, 15.2f, 5.8f, 15.2f, 7.6f)
                lineTo(15.2f, 10.5f)
            }
        }.build()
    }

    val Sliders: ImageVector by lazy {
        ImageVector.Builder(
            name = "sliders", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            path(
                fill = null, stroke = SolidColor(Color.White), strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(4f, 8f); lineTo(20f, 8f)
                moveTo(4f, 16f); lineTo(20f, 16f)
                moveTo(9f, 8f); lineTo(9f, 8f)
                moveTo(15f, 16f); lineTo(15f, 16f)
            }
            path(fill = SolidColor(Color.White)) {
                moveTo(9f, 5.6f)
                curveTo(10.3f, 5.6f, 11.4f, 6.7f, 11.4f, 8f)
                curveTo(11.4f, 9.3f, 10.3f, 10.4f, 9f, 10.4f)
                curveTo(7.7f, 10.4f, 6.6f, 9.3f, 6.6f, 8f)
                curveTo(6.6f, 6.7f, 7.7f, 5.6f, 9f, 5.6f)
                close()
                moveTo(15f, 13.6f)
                curveTo(16.3f, 13.6f, 17.4f, 14.7f, 17.4f, 16f)
                curveTo(17.4f, 17.3f, 16.3f, 18.4f, 15f, 18.4f)
                curveTo(13.7f, 18.4f, 12.6f, 17.3f, 12.6f, 16f)
                curveTo(12.6f, 14.7f, 13.7f, 13.6f, 15f, 13.6f)
                close()
            }
        }.build()
    }

    val Bookmark: ImageVector by lazy {
        ImageVector.Builder(
            name = "bookmark", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            path(
                fill = null, stroke = SolidColor(Color.White), strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(6.5f, 4f); lineTo(17.5f, 4f)
                curveTo(18.1f, 4f, 18.5f, 4.4f, 18.5f, 5f)
                lineTo(18.5f, 20f); lineTo(12f, 15.4f); lineTo(5.5f, 20f)
                lineTo(5.5f, 5f)
                curveTo(5.5f, 4.4f, 5.9f, 4f, 6.5f, 4f)
                close()
            }
        }.build()
    }

    val Download: ImageVector by lazy {
        ImageVector.Builder(
            name = "download", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            path(
                fill = null, stroke = SolidColor(Color.White), strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(12f, 3.5f); lineTo(12f, 15.5f)
                moveTo(7.5f, 11f); lineTo(12f, 15.5f); lineTo(16.5f, 11f)
                moveTo(5f, 20f); lineTo(19f, 20f)
            }
        }.build()
    }

}
