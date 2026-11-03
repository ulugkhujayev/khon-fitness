package dev.mirzohidkhon.khonfitness.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import dev.mirzohidkhon.khonfitness.ui.screens.PRESET_COLORS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** UI06 P5 rows 1 and 2: every text pair in [TextPairs] meets WCAG AA, ink on fills and placeholders included. */
class ContrastTest {
    @Test fun ratioMatchesWcag() {
        assertEquals(21.0, contrast(Color.White, Color.Black), 0.01)
        assertEquals(1.0, contrast(K.Accent, K.Accent), 0.0)
        // The 0.5.0 label on the accent: white on systemOrange.
        assertEquals(2.06, contrast(Color.White, K.Accent), 0.01)
    }

    @Test fun everyTextPairMeetsAa() {
        val low = TextPairs.map { it to contrast(it.fg, it.bg) }.filter { (p, r) -> r < if (p.large) 3.0 else 4.5 }
        assertTrue(low.joinToString { (p, r) -> "${p.name} %.2f".format(r) }, low.isEmpty())
    }

    @Test fun pairsCoverInkPlaceholderAndTints() {
        val names = TextPairs.map { it.name }
        for (n in listOf("AccentInk on Accent", "GreenInk on Green", "RedInk on Red", "Muted on Surface2", "Muted on Surface3", "Red on its tint over Surface"))
            assertTrue(n, n in names)
    }

    /** [K.Dim] is for glyphs and off controls: a Text drawn in it, other than an off one, is a pair the list misses. */
    @Test fun noReadableTextInDim() {
        val text = Regex("""(Text\(|SpanStyle\(|TextStyle\(|titleColor = |to K\.Dim)""")
        val hits = File("src/main/java").walkTopDown().filter { it.extension == "kt" }.flatMap { f ->
            f.readLines().withIndex().filter { (_, l) -> "K.Dim" in l && text.containsMatchIn(l) && "enabled" !in l }.map { (i, l) -> "${f.name}:${i + 1} ${l.trim()}" }
        }.toList()
        assertTrue(hits.joinToString("\n"), hits.isEmpty())
    }

    /** Fills under a symbol: the theme's colors, every cardio type color, and the gray of a type without one. */
    private val symbolFills = listOf(K.Accent, K.Green, K.Blue, K.Red, K.Dim) + (PRESET_COLORS + 0xFF9B9BA3).map { Color(it) }

    /** An IconTile's symbol ([inkOn]) on its solid fill: 4.5:1 or more, so white on orange (2.1:1) cannot come back. */
    @Test fun tileSymbolsRead() {
        val low = symbolFills.filter { contrast(inkOn(it), it) < 4.5 }
        assertTrue(low.joinToString { "%08X".format(it.value.toLong() ushr 32) }, low.isEmpty())
        assertEquals(K.AccentInk, inkOn(K.Accent))
    }

    /** A SymbolCircle's symbol in its color on that color at 20 percent, on a card and on the page: 3:1 or more. */
    @Test fun circleSymbolsRead() {
        val low = symbolFills.filter { it != K.Dim }.flatMap { c -> listOf(K.Bg, K.Surface).map { c to contrast(c, c.copy(alpha = 0.2f).compositeOver(it)) } }.filter { it.second < 3.0 }
        assertTrue(low.joinToString { (c, r) -> "%08X %.2f".format(c.value.toLong() ushr 32, r) }, low.isEmpty())
    }

    private val ui = File("src/main/java/dev/mirzohidkhon/khonfitness/ui")
    private fun sources(): Map<String, String> =
        ui.walkTopDown().filter { it.extension == "kt" && it.parentFile.name != "theme" }.associate { it.name to it.readText() }

    /**
     * Every color that text or a symbol takes in the screens, resolved through aliases, branches and parameters, is a
     * [TextPairs] color ([ColorUseCheck]). A raw color there fails too: the timer's "Elapsed" at white 60 percent read
     * at 4.4:1 on its green.
     */
    @Test fun everyTextColorIsAPair() {
        val hits = ColorUseCheck.hits(sources())
        assertTrue(hits.joinToString("\n"), hits.isEmpty())
    }

    /** The check sees the real screens: the hits it finds after one break in a real file. */
    private fun hitsAfter(file: String, old: String, new: String): List<String> {
        val src = sources()
        val text = src.getValue(file)
        assertTrue("$file has no $old", old in text)
        return ColorUseCheck.hits(src + (file to text.replaceFirst(old, new)))
    }

    /** Sol's first miss: the routine count back on the private alias of white at 60 percent. */
    @Test fun checkCatchesAnAlias() {
        val hits = hitsAfter("StretchParts.kt", "color = K.OnPhaseMuted, textAlign = TextAlign.Center", "color = OnPhaseDim, textAlign = TextAlign.Center")
        assertTrue(hits.toString(), hits.any { it.startsWith("StretchParts.kt:") && "OnPhaseDim" in it })
    }

    /** Sol's second miss: a bad color split over several lines. */
    @Test fun checkCatchesASplitExpression() {
        val hits = hitsAfter("TimerScreen.kt", "color = K.OnPhaseMuted, style = MaterialTheme.typography.bodyMedium",
            "color =\n                K.Text.copy(\n                    alpha = 0.6f,\n                ),\n                style = MaterialTheme.typography.bodyMedium")
        assertTrue(hits.toString(), hits.any { it.startsWith("TimerScreen.kt:") && "no TextPairs entry" in it })
    }

    /** The other shapes the check resolves, each with a bad pair: a branch alias, a when, a positional and a default argument, and a split raw color. */
    @Test fun checkCatchesEveryShape() {
        val src = """
            private val Soft = Color.White.copy(alpha = 0.6f)
            @Composable fun Row1(on: Boolean) { val c = if (on) K.Text else K.Dim; Text("a", color = c) }
            @Composable fun Row2(n: Int) { Text("b", color = when (n) { 0 -> K.Text
                else -> K.Blue }) }
            @Composable fun Label(text: String, tint: Color = K.Surface3) { Text(text, color = tint) }
            @Composable fun Use() { Label("c", K.Divider); Label("d") }
            @Composable fun Row3() { Text(
                "e",
                color =
                    Soft,
            ) }
            @Composable fun Row4(enabled: Boolean) { Text("f", color = if (enabled) K.Text else K.Dim) }
        """.trimIndent()
        val hits = ColorUseCheck.hits(mapOf("A.kt" to src))
        for ((line, why) in listOf(2 to "K.Dim text", 3 to "K.Blue", 6 to "K.Divider", 5 to "K.Surface3", 10 to "raw color"))
            assertTrue("line $line $why: $hits", hits.any { it.startsWith("A.kt:$line ") && why in it })
        // An off control may be dim.
        assertTrue(hits.toString(), hits.none { it.startsWith("A.kt:12 ") })
        assertEquals(hits.toString(), 5, hits.size)
    }
}
