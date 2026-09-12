package dev.mirzohidkhon.khonfitness.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/** Small stroke icon set drawn with paths, so the app needs no icon dependency. */
object Icons {
    private fun icon(name: String, build: androidx.compose.ui.graphics.vector.ImageVector.Builder.() -> Unit) =
        ImageVector.Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply(build).build()

    private fun ImageVector.Builder.stroke(d: String) = addPath(pathData = PathParser().parsePathString(d).toNodes(), stroke = SolidColor(Color.White), strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
    private fun ImageVector.Builder.fill(d: String) = addPath(pathData = PathParser().parsePathString(d).toNodes(), fill = SolidColor(Color.White))

    val Today = icon("today") { stroke("M3 5h18v16H3z"); stroke("M3 10h18M8 3v4M16 3v4"); fill("M12 13.9a1.6 1.6 0 1 0 0 3.2a1.6 1.6 0 1 0 0-3.2z") }
    val History = icon("history") { stroke("M4 19V9M10 19V5M16 19v-8M22 19H2") }
    val Programs = icon("programs") { stroke("M8 6h13M8 12h13M8 18h13"); fill("M4 4.8a1.2 1.2 0 1 0 0 2.4a1.2 1.2 0 1 0 0-2.4z"); fill("M4 10.8a1.2 1.2 0 1 0 0 2.4a1.2 1.2 0 1 0 0-2.4z"); fill("M4 16.8a1.2 1.2 0 1 0 0 2.4a1.2 1.2 0 1 0 0-2.4z") }
    val Check = icon("check") { stroke("M5 12.5l4.5 4.5L19 7") }
    val Gear = icon("gear") { stroke("M12 15.5a3.5 3.5 0 1 0 0-7a3.5 3.5 0 1 0 0 7z"); stroke("M19.4 15a1.7 1.7 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.7 1.7 0 0 0-1.8-.3a1.7 1.7 0 0 0-1 1.5V21a2 2 0 1 1-4 0v-.1a1.7 1.7 0 0 0-1.1-1.5a1.7 1.7 0 0 0-1.8.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.7 1.7 0 0 0 .3-1.8a1.7 1.7 0 0 0-1.5-1H3a2 2 0 1 1 0-4h.1a1.7 1.7 0 0 0 1.5-1.1a1.7 1.7 0 0 0-.3-1.8l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.7 1.7 0 0 0 1.8.3H9a1.7 1.7 0 0 0 1-1.5V3a2 2 0 1 1 4 0v.1a1.7 1.7 0 0 0 1 1.5a1.7 1.7 0 0 0 1.8-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.7 1.7 0 0 0-.3 1.8V9a1.7 1.7 0 0 0 1.5 1H21a2 2 0 1 1 0 4h-.1a1.7 1.7 0 0 0-1.5 1z") }
    /** SF-like chevrons and plus, 2.2 stroke, for navigation bars and disclosure indicators. */
    val ChevronRight = icon("chevron.right") { addPath(pathData = PathParser().parsePathString("M9 5l7 7-7 7").toNodes(), stroke = SolidColor(Color.White), strokeLineWidth = 2.4f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) }
    val ChevronLeft = icon("chevron.left") { addPath(pathData = PathParser().parsePathString("M15 5l-7 7 7 7").toNodes(), stroke = SolidColor(Color.White), strokeLineWidth = 2.4f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) }
    val Plus = icon("plus") { addPath(pathData = PathParser().parsePathString("M12 5v14M5 12h14").toNodes(), stroke = SolidColor(Color.White), strokeLineWidth = 2.4f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) }
    val Search = icon("search") { stroke("M11 18a7 7 0 1 0 0-14a7 7 0 1 0 0 14z"); stroke("M21 21l-4.5-4.5") }
}
