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
}
