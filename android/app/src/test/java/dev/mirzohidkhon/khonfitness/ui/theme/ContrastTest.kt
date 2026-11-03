package dev.mirzohidkhon.khonfitness.ui.theme

import androidx.compose.ui.graphics.Color
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
}
