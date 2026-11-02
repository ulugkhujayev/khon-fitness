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
    /** Editor actions (ActionRow), so each action looks the same wherever it appears. */
    val Trash = icon("trash") { stroke("M4 7h16M10 3h4M6 7l1 13h10l1-13M10 11v6M14 11v6") }
    val Archive = icon("archivebox") { stroke("M3 4h18v4H3zM5 8v12h14V8M10 12h4") }
    val Unarchive = icon("tray.and.arrow.up") { stroke("M4 14v6h16v-6M12 15V4M8 8l4-4 4 4") }
    val Merge = icon("arrow.triangle.merge") { stroke("M6 4v4a6 6 0 0 0 6 6M18 4v4a6 6 0 0 1-6 6v6M9 17l3 3 3-3") }
    val ArrowUp = icon("arrow.up") { stroke("M12 20V4M5 11l7-7 7 7") }
    val ArrowDown = icon("arrow.down") { stroke("M12 4v16M5 13l7 7 7-7") }
    /** What a day holds (Today's card, plan sheets): a program, cardio, rest. */
    val Dumbbell = icon("dumbbell.fill") { fill("M4.5 6.5a1.5 1.5 0 0 1 3 0v11a1.5 1.5 0 0 1-3 0zM16.5 6.5a1.5 1.5 0 0 1 3 0v11a1.5 1.5 0 0 1-3 0zM1.5 9.5a1 1 0 0 1 2 0v5a1 1 0 0 1-2 0zM20.5 9.5a1 1 0 0 1 2 0v5a1 1 0 0 1-2 0z"); stroke("M7.5 12h9") }
    val Runner = icon("figure.run") { fill("M15.5 2.4a2 2 0 1 0 0 4a2 2 0 1 0 0-4z"); stroke("M6.5 10l3-2.5h4.5l2.5 3.5 3 .5M14 7.5l-2.5 5.5 3.5 2.5-1.5 5M11.5 13l-2.5 3H5") }
    val Moon = icon("moon.fill") { fill("M20.5 14.6A8.5 8.5 0 1 1 9.4 3.5a7 7 0 0 0 11.1 11.1z") }
    /** Today's rows: the stretch routine, the eating window, band sessions. */
    val Stretch = icon("figure.flexibility") { fill("M12 2.2a2 2 0 1 0 0 4a2 2 0 1 0 0-4z"); stroke("M4.5 8l7.5 1.5 7.5-1.5M12 9.5v5M7 21l5-6.5 5 6.5") }
    val ForkKnife = icon("fork.knife") { stroke("M7 3v18M4.5 3v5a2.5 2.5 0 0 0 5 0V3M17 21V3c-2 1.5-3.5 4-3.5 7.5V14H17") }
    val Heart = icon("heart.fill") { fill("M12 20.6l-1.4-1.3C5.6 14.8 2.5 12 2.5 8.6 2.5 5.8 4.7 3.6 7.5 3.6c1.6 0 3.1.7 4.5 2 1.4-1.3 2.9-2 4.5-2 2.8 0 5 2.2 5 5 0 3.4-3.1 6.2-8.1 10.7z") }
    /** History's Import button: an arrow into a tray. */
    val Download = icon("square.and.arrow.down") { stroke("M12 3.5v11M7.5 10l4.5 4.5 4.5-4.5M4.5 15v4.5h15V15") }
    /** Import's empty cards: reading, and all caught up. */
    val DownloadCircle = icon("arrow.down.circle") { stroke("M12 3a9 9 0 1 0 0 18a9 9 0 1 0 0-18z"); stroke("M12 7.5v8M8.5 12l3.5 3.5 3.5-3.5") }
    val CheckCircle = icon("checkmark.circle.fill") {
        addPath(pathData = PathParser().parsePathString("M12 2a10 10 0 1 0 0 20a10 10 0 1 0 0-20zM10.4 16.4l-4.3-4.3 1.4-1.4 2.9 2.9 6.1-6.1 1.4 1.4z").toNodes(), fill = SolidColor(Color.White), pathFillType = androidx.compose.ui.graphics.PathFillType.EvenOdd)
    }
    /** The health store, on Import's cards that ask for access. */
    val HeartSquare = icon("heart.text.square.fill") {
        addPath(pathData = PathParser().parsePathString("M7 3h10a4 4 0 0 1 4 4v10a4 4 0 0 1-4 4H7a4 4 0 0 1-4-4V7a4 4 0 0 1 4-4zM12 17.2l.9-.8c3-2.7 4.8-4.3 4.8-6.3 0-1.6-1.3-2.9-2.9-2.9-.9 0-1.9.5-2.8 1.3-.9-.8-1.9-1.3-2.8-1.3-1.6 0-2.9 1.3-2.9 2.9 0 2 1.8 3.6 4.8 6.3z").toNodes(), fill = SolidColor(Color.White), pathFillType = androidx.compose.ui.graphics.PathFillType.EvenOdd)
    }
    /** A value that cannot change (an exercise's kind once used), in place of the chevron. */
    val Lock = icon("lock.fill") { fill("M7 10V7.5a5 5 0 0 1 10 0V10h.5A1.5 1.5 0 0 1 19 11.5v8a1.5 1.5 0 0 1-1.5 1.5h-11A1.5 1.5 0 0 1 5 19.5v-8A1.5 1.5 0 0 1 6.5 10zM9 10h6V7.5a3 3 0 0 0-6 0z") }
    /** Settings rows: export, version, reset, restore, reopen. */
    val Upload = icon("square.and.arrow.up") { stroke("M12 14.5v-11M7.5 8L12 3.5 16.5 8M4.5 12v7.5h15V12") }
    val Info = icon("info.circle") { stroke("M12 3a9 9 0 1 0 0 18a9 9 0 1 0 0-18z"); stroke("M12 11v5.5"); fill("M12 6.8a1.2 1.2 0 1 0 0 2.4a1.2 1.2 0 1 0 0-2.4z") }
    val Reset = icon("arrow.counterclockwise") { stroke("M4.5 12a7.5 7.5 0 1 0 2.2-5.3"); stroke("M4.5 3.5v4h4") }
    val Restore = icon("clock.arrow.circlepath") { stroke("M4.5 12a7.5 7.5 0 1 0 2.2-5.3"); stroke("M4.5 3.5v4h4"); stroke("M12 8v4.5l3 2") }
    val Reopen = icon("arrow.clockwise") { stroke("M19.5 12a7.5 7.5 0 1 1-2.2-5.3"); stroke("M19.5 3.5v4h-4") }
    /** Clear button of a search field: a filled circle with an x cut out. */
    val ClearFill = icon("xmark.circle.fill") {
        addPath(pathData = PathParser().parsePathString("M12 2a10 10 0 1 0 0 20a10 10 0 1 0 0-20zM8.5 7.1L12 10.6l3.5-3.5 1.4 1.4-3.5 3.5 3.5 3.5-1.4 1.4-3.5-3.5-3.5 3.5-1.4-1.4 3.5-3.5-3.5-3.5z").toNodes(), fill = SolidColor(Color.White), pathFillType = androidx.compose.ui.graphics.PathFillType.EvenOdd)
    }
}
