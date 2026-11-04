package dev.mirzohidkhon.khonfitness.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import dev.mirzohidkhon.khonfitness.ui.screens.PRESET_COLORS

/**
 * The source check behind ContrastTest.everyTextColorIsAPair. It finds every color that text or a symbol takes in the
 * screens: the color of a Text, a TextStyle, a SpanStyle or a style's copy, an Icon's tint, a button's content color,
 * and every parameter of our own composables that flows into one of those (named or positional, defaults included).
 * It reads each argument as a whole expression, over as many lines as it takes, and resolves it to colors: `K` tokens,
 * `.copy(alpha = …)` of a token, aliases (`val x = …` in the file), `if`, `when` and `?:` branches, `x?.let { … }`,
 * and color functions such as `exerciseColor`. A cardio type's own color (`Color(type.color)`) stands for every
 * [PRESET_COLORS] entry. Each color has to be the text color of a [TextPairs] entry, or turn into one over that
 * entry's fill. Hits: a color no entry has, [K.Dim] text that is not an off control, a raw color (a hex value or
 * white or black at some alpha) outside the theme, and an expression the check cannot resolve.
 *
 * The check does not know the fill under each text; it holds the color to the pairs, and the pairs to WCAG AA.
 */
object ColorUseCheck {
    private enum class Use { TEXT, SYMBOL }

    /** Built-in calls and the argument that colors their text or symbol. */
    private val builtIn: Map<String, Map<String, Use>> = mapOf(
        "Text" to mapOf("color" to Use.TEXT),
        "TextStyle" to mapOf("color" to Use.TEXT),
        "SpanStyle" to mapOf("color" to Use.TEXT),
        "copy" to mapOf("color" to Use.TEXT),
        "Icon" to mapOf("tint" to Use.SYMBOL),
        "buttonColors" to mapOf("contentColor" to Use.TEXT),
        "textButtonColors" to mapOf("contentColor" to Use.TEXT),
    )

    /** Every `K` color token by name, read from the object itself so a new token needs no entry here. */
    private val tokens: Map<String, Color> = K::class.java.declaredMethods
        .filter { it.name.startsWith("get") && it.returnType == java.lang.Long.TYPE && it.parameterCount == 0 }
        .associate { it.name.removePrefix("get").substringBefore('-') to Color((it.invoke(K) as Long).toULong()) }

    private val typeColors = PRESET_COLORS.map { Color(it) }

    /** A function; [colorBody] is the offset and expression of a `fun x(…): Color = …`. */
    private class Fn(val file: Src, val name: String, val open: Int, val params: List<Param>, val body: IntRange, val colorBody: Pair<Int, String>?)
    private class Param(val name: String, val isColor: Boolean, val default: String?, val at: Int)
    private class Src(val name: String, val text: String, val code: String) {
        fun line(at: Int) = text.substring(0, at.coerceAtMost(text.length)).count { it == '\n' } + 1
    }

    /** One resolved branch of a color expression. */
    private sealed class Leaf(val src: String) {
        class Colors(src: String, val colors: List<Color>, val raw: Boolean) : Leaf(src)
        class FromParam(src: String, val fn: Fn, val param: String) : Leaf(src)
        class Inherit(src: String) : Leaf(src)
        class Unknown(src: String) : Leaf(src)
    }

    /** Hits as "File.kt:line expression: reason"; [files] maps a file name to its Kotlin source. */
    fun hits(files: Map<String, String>): List<String> {
        val srcs = files.map { (n, t) -> Src(n, t, blank(t)) }
        val fns = srcs.flatMap { functions(it) }
        val textParams = HashMap<Fn, MutableMap<String, Use>>()
        fun fnAt(src: Src, at: Int) = fns.filter { it.file === src && at in it.body }.minByOrNull { it.body.last - it.body.first }
        fun lookup(src: Src, name: String) = fns.firstOrNull { it.file === src && it.name == name } ?: fns.firstOrNull { it.name == name }

        // Every argument that colors text or a symbol: (file, offset, expression, use).
        fun sites(): List<Triple<Src, Pair<Int, String>, Use>> = srcs.flatMap { src ->
            Regex("""([A-Za-z_][\w.]*)\s*\(""").findAll(src.code).flatMap { m ->
                val chain = m.groupValues[1]
                val name = chain.substringAfterLast('.')
                val open = m.range.last
                if (fns.any { it.file === src && it.open == open }) return@flatMap emptySequence()
                val own = if (chain.startsWith("androidx.") || name in builtIn) null else lookup(src, name)
                val wanted: Map<String, Use> = own?.let { textParams[it] } ?: builtIn[name].orEmpty()
                if (wanted.isEmpty()) return@flatMap emptySequence()
                args(src.code, open).withIndex().mapNotNull { (i, a) ->
                    val named = Regex("""^\s*(\w+)\s*=(?!=)""").find(src.code.substring(a.first, a.last + 1))
                    val param = named?.groupValues?.get(1) ?: own?.params?.getOrNull(i)?.name ?: return@mapNotNull null
                    val use = wanted[param] ?: return@mapNotNull null
                    val from = if (named != null) a.first + named.range.last + 1 else a.first
                    Triple(src, from to src.code.substring(from, a.last + 1), use)
                }.asSequence()
            }.toList()
        }

        /** [part] picks a side of a pair (`a to b`): 0 for `.first`, 1 for `.second` or a destructured second name. */
        fun resolve(src: Src, at: Int, expr: String, off: Boolean = false, depth: Int = 0, part: Int = -1): List<Pair<Leaf, Boolean>> {
            val e = trimParens(expr.trim().removeSuffix(",").trim())
            if (depth > 40) return listOf(Leaf.Unknown(e) to off)
            fun again(x: String, o: Boolean = off, p: Int = part) = resolve(src, at, x, o, depth + 1, p)
            Regex("""^if\s*\(""").find(e)?.let { m ->
                val close = match(e, m.range.last)
                val cond = e.substring(m.range.last + 1, close).trim()
                val rest = e.substring(close + 1)
                val split = topLevel(rest, "else").firstOrNull() ?: return listOf(Leaf.Unknown(e) to off)
                // An off control may show in any color (WCAG 1.4.3 exempts inactive controls).
                val offThen = cond.startsWith("!") && "enabled" in cond
                val offElse = !cond.startsWith("!") && "enabled" in cond
                return again(rest.substring(0, split), off || offThen) + again(rest.substring(split + 4), off || offElse)
            }
            Regex("""^when\b[^{]*\{""").find(e)?.let { m ->
                val body = e.substring(m.range.last + 1, match(e, m.range.last))
                return splitTop(body, '\n', ';').filter { it.isNotBlank() }.flatMap { entry ->
                    val arrow = topLevel(entry, "->").firstOrNull() ?: return@flatMap listOf(Leaf.Unknown(entry.trim()) to off)
                    again(entry.substring(arrow + 2))
                }
            }
            topLevel(e, "?:").takeIf { it.isNotEmpty() }?.let { cuts ->
                val bounds = listOf(-2) + cuts + e.length
                return bounds.zipWithNext().flatMap { (a, b) -> again(e.substring(a + 2, b)) }
            }
            topLevel(e, "to").firstOrNull()?.let { cut -> return again(if (part == 1) e.substring(cut + 2) else e.substring(0, cut), p = -1) }
            Regex("""^(.+?)\??\.let\s*\{""").find(e)?.takeIf { e.endsWith("}") && match(e, it.range.last) == e.length - 1 }?.let { m ->
                return again(e.substring(m.range.last + 1, e.length - 1).replaceFirst(Regex("""^\s*\w+\s*->"""), ""))
            }
            // A copy at some alpha of whatever the base resolves to.
            Regex("""^(.+)\.copy\(\s*alpha\s*=\s*([^)]+)\)$""").find(e)?.let { m ->
                val alpha = m.groupValues[2].trim().removeSuffix(",").trim().let { if (it == "TINT_ALPHA") TINT_ALPHA else it.removeSuffix("f").toFloatOrNull() }
                    ?: return listOf(Leaf.Unknown(e) to off)
                return again(m.groupValues[1]).map { (l, o) ->
                    (if (l is Leaf.Colors) Leaf.Colors(e, l.colors.map { it.copy(alpha = alpha) }, l.raw || l.src.startsWith("Color.")) else l) to o
                }
            }
            Regex("""^K\.(\w+)$""").find(e)?.let { m -> return listOf((tokens[m.groupValues[1]]?.let { Leaf.Colors(e, listOf(it), false) } ?: Leaf.Unknown(e)) to off) }
            when (e) {
                "Color.White" -> return listOf(Leaf.Colors(e, listOf(Color.White), false) to off)
                "Color.Black" -> return listOf(Leaf.Colors(e, listOf(Color.Black), false) to off)
                "Color.Unspecified", "LocalContentColor.current", "null" -> return listOf(Leaf.Inherit(e) to off)
            }
            Regex("""^(?:androidx\.compose\.ui\.graphics\.)?Color\((.*)\)$""").find(e)?.let { m ->
                val hex = Regex("""^0x([0-9A-Fa-f]{8})$""").find(m.groupValues[1].trim())
                return listOf((if (hex != null) Leaf.Colors(e, listOf(Color(hex.groupValues[1].toLong(16))), true) else Leaf.Colors(e, typeColors, false)) to off)
            }
            // inkOn picks black or white on its fill; ContrastTest.tileSymbolsRead holds it to 4.5:1.
            if (e.startsWith("inkOn(")) return listOf(Leaf.Colors(e, listOf(Color.Black, Color.White), false) to off)
            Regex("""^(\w+)\s*\(""").find(e)?.let { m ->
                val side = when (e.substring(match(e, m.range.last) + 1)) { "" -> part; ".first", "?.first" -> 0; ".second", "?.second" -> 1; else -> return@let }
                val f = lookup(src, m.groupValues[1])
                val body = f?.colorBody ?: return listOf(Leaf.Unknown(e) to off)
                return resolve(f.file, body.first, body.second, off, depth + 1, side)
            }
            Regex("""^\w+$""").find(e)?.let {
                val fn = fnAt(src, at)
                if (fn != null && fn.params.any { it.name == e && it.isColor }) return listOf(Leaf.FromParam(e, fn, e) to off)
                val mine = { d: MatchResult -> fnAt(src, d.range.first).let { it == null || it == fn } }
                val decl = Regex("""\b(?:val|var)\s+$e\s*(?::\s*Color\??\s*)?=(?!=)""").findAll(src.code).filter(mine)
                    .let { all -> all.lastOrNull { it.range.first < at } ?: all.firstOrNull() }
                if (decl != null) return resolve(src, decl.range.first, expressionAfter(src.code, decl.range.last + 1), off, depth + 1)
                // val (title, titleColor) = when (…) { … -> name to K.Text }
                val pair = Regex("""\bval\s*\(([^)]*)\)\s*=(?!=)""").findAll(src.code).filter(mine).lastOrNull { d -> e in d.groupValues[1].split(',').map { it.trim() } }
                    ?: return listOf(Leaf.Unknown(e) to off)
                val side = pair.groupValues[1].split(',').map { it.trim() }.indexOf(e)
                return resolve(src, pair.range.first, expressionAfter(src.code, pair.range.last + 1), off, depth + 1, side)
            }
            return listOf(Leaf.Unknown(e) to off)
        }

        // Parameters of our composables that reach a text or symbol color, until no new one turns up.
        do {
            var grew = false
            for ((src, site, use) in sites()) for ((leaf, _) in resolve(src, site.first, site.second)) {
                if (leaf !is Leaf.FromParam) continue
                val known = textParams.getOrPut(leaf.fn) { HashMap() }
                if (known[leaf.param] == null || (use == Use.TEXT && known[leaf.param] == Use.SYMBOL)) { known[leaf.param] = use; grew = true }
            }
        } while (grew)

        val all = sites() + textParams.flatMap { (fn, ps) -> ps.mapNotNull { (p, use) -> fn.params.first { it.name == p }.let { q -> q.default?.let { Triple(fn.file, q.at to it, use) } } } }
        return all.flatMap { (src, site, use) ->
            resolve(src, site.first, site.second).mapNotNull { (leaf, off) -> problem(leaf, off, use)?.let { "${src.name}:${src.line(site.first + site.second.length - site.second.trimStart().length)} ${site.second.trim().replace(Regex("""\s+"""), " ")}: $it" } }
        }.distinct()
    }

    private fun problem(leaf: Leaf, off: Boolean, use: Use): String? = when {
        off -> null
        leaf is Leaf.Unknown -> "cannot resolve ${leaf.src}"
        leaf is Leaf.Colors && leaf.raw -> "raw color ${leaf.src}; put it in K"
        leaf is Leaf.Colors -> leaf.colors.firstNotNullOfOrNull { c ->
            when {
                c == K.Dim -> if (use == Use.TEXT) "K.Dim text that is not an off control" else null
                TextPairs.none { p -> p.fg == c || (c.alpha < 1f && c.compositeOver(p.bg) == p.fg) } -> "${leaf.src} (%08X) is in no TextPairs entry".format(c.value.toLong() ushr 32)
                else -> null
            }
        }
        else -> null
    }

    /** Every `fun` with its parameters and its body (a block, or an `= expression`). */
    private fun functions(src: Src): List<Fn> = Regex("""\bfun\s+(?:<[^>]*>\s*)?(?:[\w.]+\.)?(\w+)\s*\(""").findAll(src.code).mapNotNull { m ->
        val open = m.range.last
        val close = match(src.code, open)
        val params = args(src.code, open).map { r ->
            val p = src.code.substring(r.first, r.last + 1)
            val head = Regex("""^\s*(?:@\w+\s+)*(?:vararg\s+)?(\w+)\s*:\s*""").find(p)
            val eq = topLevel(p, "=").firstOrNull()
            val type = if (head == null) "" else p.substring(head.range.last + 1, eq ?: p.length).trim()
            Param(head?.groupValues?.get(1) ?: "", type == "Color" || type == "Color?", eq?.let { p.substring(it + 1) }, r.first + (eq?.plus(1) ?: 0))
        }
        var i = close + 1
        while (i < src.code.length && src.code[i] != '{' && src.code[i] != '=' && src.code[i] != '\n') i++
        val name = m.groupValues[1]
        when {
            i >= src.code.length -> null
            src.code[i] == '{' -> Fn(src, name, open, params, close..match(src.code, i), null)
            src.code[i] == '=' -> {
                val body = expressionAfter(src.code, i + 1)
                val isColor = Regex("""^\s*:\s*(Color|Pair<Color,[^>]*>\??)\s*$""").matches(src.code.substring(close + 1, i))
                Fn(src, name, open, params, close..(i + body.length), if (isColor) (i + 1) to body else null)
            }
            else -> Fn(src, name, open, params, close..close, null)
        }
    }.toList()

    /** The source with string contents and comments turned into spaces, so brackets in them do not count. */
    fun blank(s: String): String {
        val out = StringBuilder(s)
        val stack = ArrayList<Int>() // brace depth of each open `${`, inside strings
        var i = 0
        var inString = false
        var raw = false
        fun wipe(a: Int, b: Int) { for (k in a until b) if (out[k] != '\n') out[k] = ' ' }
        while (i < s.length) {
            if (inString) {
                when {
                    !raw && s[i] == '\\' -> { wipe(i, minOf(i + 2, s.length)); i += 2 }
                    s.startsWith("\${", i) -> { wipe(i, i + 2); stack.add(0); inString = false; i += 2 }
                    raw && s.startsWith("\"\"\"", i) -> { inString = false; i += 3 }
                    !raw && s[i] == '"' -> { inString = false; i++ }
                    else -> { wipe(i, i + 1); i++ }
                }
                continue
            }
            when {
                s.startsWith("//", i) -> { val e = s.indexOf('\n', i).let { if (it < 0) s.length else it }; wipe(i, e); i = e }
                s.startsWith("/*", i) -> { val e = s.indexOf("*/", i + 2).let { if (it < 0) s.length else it + 2 }; wipe(i, e); i = e }
                s.startsWith("\"\"\"", i) -> { inString = true; raw = true; i += 3 }
                s[i] == '"' -> { inString = true; raw = false; i++ }
                s[i] == '\'' -> { val e = if (s.getOrNull(i + 1) == '\\') s.indexOf('\'', i + 3) else i + 2; wipe(i + 1, e); i = e + 1 }
                stack.isNotEmpty() && s[i] == '{' -> { stack[stack.size - 1]++; i++ }
                stack.isNotEmpty() && s[i] == '}' -> {
                    if (stack.last() == 0) { stack.removeAt(stack.size - 1); wipe(i, i + 1); inString = true } else stack[stack.size - 1]--
                    i++
                }
                else -> i++
            }
        }
        return out.toString()
    }

    /** Index of the bracket that closes the one at [open]. */
    private fun match(s: String, open: Int): Int {
        var d = 0
        for (i in open until s.length) when (s[i]) {
            '(', '[', '{' -> d++
            ')', ']', '}' -> if (--d == 0) return i
        }
        return s.length - 1
    }

    /** The argument ranges of the call whose `(` is at [open], split at commas at depth 0. */
    private fun args(s: String, open: Int): List<IntRange> {
        val close = match(s, open)
        val out = ArrayList<IntRange>()
        var start = open + 1
        var d = 0
        for (i in open + 1..close) {
            val c = s[i]
            if (c in "([{") d++ else if (c in ")]}" && i < close) d-- else if ((c == ',' && d == 0) || i == close) {
                if (s.substring(start, i).isNotBlank()) out.add(start until i)
                start = i + 1
            }
        }
        return out
    }

    /** Offsets of [token] in [s] at bracket depth 0, as a whole word for a word token. */
    private fun topLevel(s: String, token: String): List<Int> {
        val out = ArrayList<Int>()
        var d = 0
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c in "([{") d++ else if (c in ")]}") d--
            else if (d == 0 && s.startsWith(token, i) && (!token[0].isLetter() || ((i == 0 || !s[i - 1].isLetterOrDigit()) && s.getOrNull(i + token.length)?.isLetterOrDigit() != true))) {
                out.add(i); i += token.length; continue
            }
            i++
        }
        return out
    }

    private fun splitTop(s: String, vararg at: Char): List<String> {
        val out = ArrayList<String>()
        var d = 0
        var start = 0
        s.forEachIndexed { i, c -> if (c in "([{") d++ else if (c in ")]}") d-- else if (d == 0 && c in at) { out.add(s.substring(start, i)); start = i + 1 } }
        return out + s.substring(start)
    }

    private fun trimParens(e: String): String = if (e.startsWith("(") && match(e, 0) == e.length - 1) trimParens(e.substring(1, e.length - 1).trim()) else e

    /** The expression that starts at [from]: up to a line end at depth 0 that the next line does not continue. */
    private fun expressionAfter(s: String, from: Int): String {
        var d = 0
        var i = from
        while (i < s.length) {
            val c = s[i]
            if (c in "([{") d++ else if (c in ")]}") { if (d == 0) break; d-- }
            else if (d == 0 && (c == ',' || c == ';')) break
            else if (d == 0 && c == '\n') {
                val before = s.substring(from, i).trimEnd()
                val next = s.substring(i + 1).trimStart()
                val goesOn = before.isEmpty() || listOf("=", "(", "?:", "else", "->", "&&", "||", "+").any { before.endsWith(it) } ||
                    listOf(".", "?.", "?:", "else", "&&", "||").any { next.startsWith(it) }
                if (!goesOn) break
            }
            i++
        }
        return s.substring(from, i)
    }
}
