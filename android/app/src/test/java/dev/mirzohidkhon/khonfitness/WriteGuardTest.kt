package dev.mirzohidkhon.khonfitness

import dev.mirzohidkhon.khonfitness.data.EatingWindow
import dev.mirzohidkhon.khonfitness.data.KhonRepository
import dev.mirzohidkhon.khonfitness.data.RoutineStretch
import dev.mirzohidkhon.khonfitness.data.Stretch
import dev.mirzohidkhon.khonfitness.data.StretchRoutine
import dev.mirzohidkhon.khonfitness.data.StretchSession
import java.io.File
import java.lang.reflect.GenericArrayType
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.TypeVariable
import java.lang.reflect.WildcardType
import kotlin.coroutines.Continuation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The P5 write rule as a check: [KhonRepository]'s public methods take a stretch, routine, routine row, stretch session
 * or window row (alone or inside a collection, array, map or function type) only in the four that are safe with one,
 * [ALLOWED], read by reflection; and outside `data/` no source reaches the DAO, except the repository built in
 * `KhonApp.kt`.
 */
class WriteGuardTest {
    private val root = File("src/main/java")

    private fun sources(): Map<String, String> = root.walkTopDown().filter { it.isFile && it.extension == "kt" }
        .associate { it.relativeTo(root).path to it.readText() }

    @Test fun `the repository and the source tree keep the rule`() {
        assertEquals(ALLOWED, rowMethods(KhonRepository::class.java)) // the check sees the four allowed methods, so it would see a fifth
        assertEquals(emptySet<String>(), rowMethodViolations(KhonRepository::class.java))
        val all = sources()
        assertTrue("no sources under ${root.absolutePath}", all.isNotEmpty())
        assertEquals(emptyList<String>(), daoViolations(all))
    }

    /** Shaped like the repository, with a row type in each place the check must look. */
    @Suppress("unused", "UNUSED_PARAMETER")
    private class RepositoryShaped {
        suspend fun reorderRoutineStretches(rows: List<RoutineStretch>) {}
        fun saveRoutines(routines: Set<StretchRoutine>?) {}
        fun saveSessions(byId: Map<String, Collection<StretchSession>>) {}
        fun saveWindows(windows: Array<EatingWindow>) {}
        fun editStretch(change: (Stretch) -> Stretch) {}
        suspend fun stretchesOnce(): List<Stretch> = emptyList() // a returned row is fine: only parameters write
        fun createStretch(s: Stretch) {}
        fun setRoutineName(id: String, name: String) {}
    }

    @Test fun `the guard catches a row inside a collection, array, map or function parameter`() {
        assertEquals(setOf("reorderRoutineStretches", "saveRoutines", "saveSessions", "saveWindows", "editStretch"),
            rowMethodViolations(RepositoryShaped::class.java))
    }

    @Test fun `the guard catches the DAO reached with spaces, line breaks or a reference`() {
        val found = daoViolations(mapOf(
            "dev/x/ui/screens/WindowSheet.kt" to "fun g() { (context.applicationContext as KhonApp).db.dao ().clearAll() }",
            "dev/x/ui/screens/StretchScreen.kt" to "fun h() {\n    app.db\n        .dao\n        ()\n        .clearAll()\n}",
            "dev/x/ui/Today.kt" to "val get = db::dao",
            "dev/x/data/Repository.kt" to "class KhonRepository(private val dao: KhonDao) { fun f() = dao.clearAll() }",
            "dev/x/KhonApp.kt" to "    val repo: KhonRepository by lazy { KhonRepository(db.dao()) }\n    val raw = db.dao()",
        ))
        assertEquals(listOf("KhonApp.kt:2", "ui/Today.kt:1", "ui/screens/StretchScreen.kt:3", "ui/screens/WindowSheet.kt:1")
            .map { "$it reaches the DAO" }, found.map { it.removePrefix("dev/x/") })
    }

    companion object {
        val ROW_TYPES: Set<Class<*>> = setOf(Stretch::class.java, StretchRoutine::class.java, RoutineStretch::class.java,
            StretchSession::class.java, EatingWindow::class.java)
        val ALLOWED = setOf("createStretch", "createRoutine", "saveStretchEdits", "recordStretchSession")

        /** Whether [t] is a row type or holds one as a type argument, bound, array element or function parameter. */
        private fun mentionsRow(t: Type): Boolean = when (t) {
            is Class<*> -> if (t.isArray) mentionsRow(t.componentType) else t in ROW_TYPES
            is ParameterizedType -> mentionsRow(t.rawType) || t.actualTypeArguments.any(::mentionsRow)
            is WildcardType -> (t.upperBounds + t.lowerBounds).any(::mentionsRow)
            is GenericArrayType -> mentionsRow(t.genericComponentType)
            is TypeVariable<*> -> t.bounds.any(::mentionsRow)
            else -> false
        }

        /**
         * The public methods of [repository] with a parameter that mentions a row type. A suspend function's last
         * parameter, the [Continuation], carries its return type, so it is left out; synthetic methods (default
         * arguments) repeat a real one. A name mangled for `internal` counts under its source name.
         */
        fun rowMethods(repository: Class<*>): Set<String> = repository.methods.filter { !it.isSynthetic }
            .filter { m ->
                val params = m.genericParameterTypes.toList()
                val own = if (m.parameterTypes.lastOrNull() == Continuation::class.java) params.dropLast(1) else params
                own.any(::mentionsRow)
            }
            .map { it.name.substringBefore('$') }.toSet()

        fun rowMethodViolations(repository: Class<*>): Set<String> = rowMethods(repository) - ALLOWED

        /** `.dao` or `::dao`, with any spaces or line breaks between the parts, whether called or only referenced. */
        private val DAO = Regex("""(\.|::)\s*dao\b""")
        // The DAO as the repository's first argument; a further argument (the settings store) may follow.
        private val CONSTRUCTION = Regex("""KhonRepository\(\s*db\s*\.\s*dao\s*\(\s*\)\s*[,)]""")

        fun daoViolations(sources: Map<String, String>): List<String> {
            val found = mutableListOf<String>()
            for ((path, text) in sources.toSortedMap()) {
                if ("/data/" in "/$path") continue
                val allowed = if (path.endsWith("KhonApp.kt")) CONSTRUCTION.findAll(text).map { it.range }.toList() else emptyList()
                DAO.findAll(text).filter { m -> allowed.none { m.range.first in it } }
                    .forEach { m -> found += "$path:${text.substring(0, m.range.first).count { it == '\n' } + 1} reaches the DAO" }
            }
            return found
        }
    }
}
