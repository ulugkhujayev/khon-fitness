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

    /**
     * Text drawn in a raw color (a hex value, or white or black at some alpha) in the screens is a pair [TextPairs] does
     * not see: the timer's "Elapsed" at white 60 percent read at 4.4:1 on its green. Such colors live in [K].
     */
    @Test fun noRawTextColorInScreens() {
        val raw = Regex("""Color\(0x|Color\.(White|Black)\.copy\(""")
        val use = Regex("""(Text\(|SpanStyle\(|\bcolor = |\btint = |textColor = |contentColor = )""")
        val hits = File("src/main/java/dev/mirzohidkhon/khonfitness/ui").walkTopDown().filter { it.extension == "kt" && it.parentFile.name != "theme" }.flatMap { f ->
            f.readLines().withIndex().filter { (_, l) -> raw.containsMatchIn(l) && use.containsMatchIn(l) }.map { (i, l) -> "${f.name}:${i + 1} ${l.trim()}" }
        }.toList()
        assertTrue(hits.joinToString("\n"), hits.isEmpty())
    }
}
