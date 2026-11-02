package dev.mirzohidkhon.khonfitness

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The P5 write rule as a check over the source tree: outside `data/`, no code reaches the DAO (except the repository
 * built in `KhonApp.kt`), and no code calls a repository method that takes a whole stretch, routine, routine row,
 * stretch session or window row, other than the four that are safe with one: [ALLOWED].
 */
class WriteGuardTest {
    private val root = File("src/main/java")

    private fun sources(): Map<String, String> = root.walkTopDown().filter { it.isFile && it.extension == "kt" }
        .associate { it.relativeTo(root).path to it.readText() }

    @Test fun `the source tree keeps the rule`() {
        val all = sources()
        assertTrue("no sources under ${root.absolutePath}", all.isNotEmpty())
        val repo = all.entries.single { it.key.endsWith("data/Repository.kt") }.value
        assertEquals(ALLOWED, rowMethods(repo)) // the check sees the four allowed methods, so it would see a fifth
        assertEquals(emptyList<String>(), violations(all))
    }

    @Test fun `the guard catches a whole-row method, its call, and a DAO reached from a screen`() {
        val repo = "class KhonRepository(private val dao: KhonDao) {\n    suspend fun saveStretch(s: Stretch) = dao.upsert(s)\n" +
            "    suspend fun saveEatingWindow(\n        w: EatingWindow?,\n    ) = dao.upsert(w)\n}\n"
        val found = violations(mapOf(
            "dev/x/data/Repository.kt" to repo,
            "dev/x/ui/screens/StretchEditors.kt" to "fun f() { vm.run { vm.repo.saveStretch(draft) } }",
            "dev/x/ui/screens/WindowSheet.kt" to "fun g() { (context.applicationContext as KhonApp).db.dao().clearAll() }",
            "dev/x/KhonApp.kt" to "    val repo: KhonRepository by lazy { KhonRepository(db.dao()) }",
        ))
        assertEquals(listOf(
            "data/Repository.kt: saveEatingWindow takes a whole row",
            "data/Repository.kt: saveStretch takes a whole row",
            "ui/screens/StretchEditors.kt:1 calls saveStretch",
            "ui/screens/WindowSheet.kt:1 reaches the DAO",
        ), found.map { it.removePrefix("dev/x/") })
    }

    companion object {
        val ROW_TYPES = listOf("Stretch", "StretchRoutine", "RoutineStretch", "StretchSession", "EatingWindow")
        val ALLOWED = setOf("createStretch", "createRoutine", "saveStretchEdits", "recordStretchSession")

        /** The repository's methods with a parameter of a row type. */
        fun rowMethods(repoSource: String): Set<String> {
            val start = repoSource.indexOf("class KhonRepository")
            if (start < 0) return emptySet()
            val body = repoSource.substring(start, repoSource.indexOf("\n}\n", start).let { if (it < 0) repoSource.length else it })
            val types = ROW_TYPES.joinToString("|")
            return Regex("""fun\s+(\w+)\s*\(([^)]*)""").findAll(body)
                .filter { m -> Regex(""":\s*($types)\??\s*(,|=|$)""", RegexOption.MULTILINE).containsMatchIn(m.groupValues[2]) }
                .map { it.groupValues[1] }.toSet()
        }

        fun violations(sources: Map<String, String>): List<String> {
            val repo = sources.entries.singleOrNull { it.key.endsWith("data/Repository.kt") }
            val rowMethods = repo?.let { rowMethods(it.value) }.orEmpty()
            val found = (rowMethods - ALLOWED).sorted().map { "${repo!!.key}: $it takes a whole row" }.toMutableList()
            for ((path, text) in sources.toSortedMap()) {
                if ("/data/" in "/$path") continue
                text.lines().forEachIndexed { i, line ->
                    if (".dao()" in line && !(path.endsWith("KhonApp.kt") && "KhonRepository(db.dao())" in line)) found += "$path:${i + 1} reaches the DAO"
                    (rowMethods - ALLOWED).filter { Regex("""\.$it\s*\(""").containsMatchIn(line) }.forEach { found += "$path:${i + 1} calls $it" }
                }
            }
            return found
        }
    }
}
