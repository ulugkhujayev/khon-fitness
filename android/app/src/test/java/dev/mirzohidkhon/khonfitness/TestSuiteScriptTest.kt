package dev.mirzohidkhon.khonfitness

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.concurrent.TimeUnit

/** scripts/test-suite.sh runs a command again only after a blocked safepoint or a test task timeout with no failed test. */
class TestSuiteScriptTest {
    @get:Rule val temp = TemporaryFolder()
    private val repo = File(System.getProperty("user.dir"), "../..").canonicalFile
    private lateinit var tree: File
    private val script get() = File(tree, "scripts/test-suite.sh")

    @Before fun copy() {
        tree = temp.newFolder("tree").toPath().toRealPath().toFile()
        File(repo, "scripts/test-suite.sh").copyTo(script)
        script.setExecutable(true)
    }

    private class Run(val status: Int, val output: String) {
        val runs get() = Regex("^run \\d+$", RegexOption.MULTILINE).findAll(output).count()
    }

    private fun suite(command: List<String>, dir: File = tree): Run {
        val log = temp.newFile()
        val builder = ProcessBuilder(listOf(script.path) + command).directory(dir).redirectErrorStream(true).redirectOutput(log)
        val process = builder.start()
        if (!process.waitFor(120, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            fail("test-suite.sh did not end:\n${log.readText()}")
        }
        return Run(process.exitValue(), log.readText())
    }

    /**
     * Prints "run N"; from run [passOn] on it passes. Before that it prints [lines], sleeps [sleep] seconds and
     * exits with [status].
     */
    private fun command(passOn: Int, status: Int = 1, sleep: Int = 0, vararg lines: String): List<String> {
        val counter = temp.newFile()
        val code = """
            n=${'$'}(( ${'$'}(cat "$1") + 1 )); echo "${'$'}n" > "$1"; echo "run ${'$'}n"
            if [ "${'$'}n" -ge "$2" ]; then echo "BUILD SUCCESSFUL"; exit 0; fi
            status=$3; pause=$4; shift 4; printf '%s\n' "$@"; sleep "${'$'}pause"; exit "${'$'}status"
        """.trimIndent()
        counter.writeText("0")
        return listOf("bash", "-c", code, "fixture", counter.path, "$passOn", "$status", "$sleep") + lines
    }

    private val abort = arrayOf(
        "[30.012s][warning][safepoint] # SafepointSynchronize::begin: Timeout detected:",
        "[30.012s][warning][safepoint] # SafepointSynchronize::begin: Timed out while spinning to reach a safepoint.",
        "# A fatal error has been detected by the Java Runtime Environment:",
        "#  SIGILL (0x4) at pc=0x000000010e0305b4, pid=90406, tid=25347",
    )

    @Test fun `two safepoint aborts then a pass give exit 0 after three runs`() {
        val r = suite(command(passOn = 3, status = 1, sleep = 0, *abort))
        assertEquals(r.output, 0, r.status)
        assertEquals(3, r.runs)
        assertEquals(2, Regex("ended on a blocked safepoint").findAll(r.output).count())
    }

    @Test fun `the fatal form of the abort runs again too`() {
        val r = suite(command(2, 134, 0, abort[0], abort[2], "# Safepoint sync time longer than 30000ms detected when executing G1CollectFull."))
        assertEquals(r.output, 0, r.status)
        assertEquals(2, r.runs)
    }

    @Test fun `three safepoint aborts give exit 1`() {
        val r = suite(command(passOn = 4, status = 1, sleep = 0, *abort))
        assertEquals(r.output, 1, r.status)
        assertEquals(3, r.runs)
        assertTrue(r.output, r.output.contains("test-suite: 3 runs hung or ended on a blocked safepoint"))
    }

    @Test fun `a plain failure gives its own status after one run`() {
        val r = suite(command(2, 5, 0, "BUILD FAILED"))
        assertEquals(r.output, 5, r.status)
        assertEquals(1, r.runs)
    }

    @Test fun `only one of the two abort marks gives the command status after one run`() {
        assertEquals(1, suite(command(2, 5, 0, abort[0])).runs)
        assertEquals(1, suite(command(2, 5, 0, abort[2])).runs)
    }

    @Test fun `a failed test gives status 7 after one run, with or without the abort lines`() {
        val failed = "ExampleTest > actualFailure FAILED"
        listOf(
            arrayOf(failed, "AssertionError: Safepoint sync time longer than"),
            arrayOf(failed, "# Safepoint sync time longer than 30000ms detected when executing G1CollectFull."),
            arrayOf(failed, *abort),
            arrayOf("12 tests completed, 1 failed", *abort),
        ).forEach { lines ->
            val r = suite(command(2, 7, 0, *lines))
            assertEquals(r.output, 7, r.status)
            assertEquals(r.output, 1, r.runs)
            assertFalse(r.output, r.output.contains("BUILD SUCCESSFUL"))
        }
    }

    @Test fun `no arguments run the suite and the release checks from the repository root`() {
        val gradlew = File(tree, "android/gradlew").apply { parentFile.mkdirs() }
        gradlew.writeText("#!/bin/bash\necho \"cwd ${'$'}(pwd -P)\"\necho \"args $*\"\n")
        gradlew.setExecutable(true)
        val r = suite(emptyList(), dir = temp.root)
        assertEquals(r.output, 0, r.status)
        assertEquals("cwd ${tree.path}\nargs -p android :app:testDebugUnitTest :app:assembleDebug :app:lintVitalRelease\n", r.output)
    }

    private val taskTimeout = arrayOf(
        "Requesting stop of task ':app:testDebugUnitTest' as it has exceeded its configured timeout of 10m.",
        "> Timeout has been exceeded",
    )

    @Test fun `a test task that passed its timeout runs again`() {
        val r = suite(command(passOn = 2, status = 1, sleep = 0, *taskTimeout))
        assertEquals(r.output, 0, r.status)
        assertEquals(2, r.runs)
        assertTrue(r.output, r.output.contains("test-suite: run 1 passed the timeout of the test task"))
    }

    @Test fun `three task timeouts give exit 1`() {
        val r = suite(command(passOn = 4, status = 1, sleep = 0, *taskTimeout))
        assertEquals(r.output, 1, r.status)
        assertEquals(3, r.runs)
    }

    @Test fun `a task timeout after a failed test does not run again`() {
        listOf("ExampleTest > actualFailure FAILED", "12 tests completed, 1 failed").forEach { failed ->
            val r = suite(command(2, 7, 0, failed, *taskTimeout))
            assertEquals(r.output, 7, r.status)
            assertEquals(r.output, 1, r.runs)
        }
    }

    @Test fun `the script ends no process and sets no time limit of its own`() {
        val text = script.readText()
        listOf("kill", "pkill", "sleep", "RUN_LIMIT_SECONDS").forEach { assertFalse(it, Regex("\\b$it\\b").containsMatchIn(text)) }
    }

    @Test fun `the test task has a timeout of ten minutes at most`() {
        val seconds = System.getProperty("testTaskTimeoutSeconds")?.toLong()
        assertTrue("$seconds", seconds != null && seconds in 1..600)
    }

    private val guard = listOf("-XX:+UnlockDiagnosticVMOptions", "-XX:+SafepointTimeout", "-XX:SafepointTimeoutDelay=", "-XX:+AbortVMOnSafepointTimeout", "-XX:ErrorFile=")
    /** The android.jar on the compile classpath has no java.lang.management; the test JVM has it. */
    @Suppress("UNCHECKED_CAST")
    private val workerArgs: List<String> = Class.forName("java.lang.management.RuntimeMXBean").getMethod("getInputArguments")
        .invoke(Class.forName("java.lang.management.ManagementFactory").getMethod("getRuntimeMXBean").invoke(null)) as List<String>

    @Test fun `the test worker runs with the safepoint guard and a 30 s delay`() {
        listOf("-XX:+UnlockDiagnosticVMOptions", "-XX:+SafepointTimeout", "-XX:SafepointTimeoutDelay=30000", "-XX:+AbortVMOnSafepointTimeout")
            .forEach { assertTrue("$it in $workerArgs", it in workerArgs) }
    }

    @Test fun `a JVM that blocks a safepoint aborts and the script runs it again`() {
        val flags = workerArgs.filter { a -> guard.any { a.startsWith(it) } }
            .map { if (it.startsWith("-XX:SafepointTimeoutDelay=")) "-XX:SafepointTimeoutDelay=2000" else it }
        assertEquals(flags.toString(), guard.size, flags.size)
        val reports = File(flags.single { it.startsWith("-XX:ErrorFile=") }.substringAfter('=')).parentFile.apply { mkdirs() }
        fun crashes(dir: File) = dir.listFiles { f -> f.name.startsWith("hs_err") }.orEmpty().toSet()
        val before = crashes(reports)
        val classes = File(SafepointProbe::class.java.protectionDomain.codeSource.location.toURI()).path
        val java = File(System.getProperty("java.home"), "bin/java").path
        val probe = listOf(java) + flags + listOf(
            "-Xbatch", "-XX:-TieredCompilation", "-XX:-UseCountedLoopSafepoints",
            "-XX:CompileCommand=dontinline,${SafepointProbe::class.java.name}::block", "-XX:-CreateCoredumpOnCrash",
            "-cp", classes, SafepointProbe::class.java.name,
        )
        val counter = temp.newFile().apply { writeText("0") }
        val code = """n=${'$'}(( ${'$'}(cat "$1") + 1 )); echo "${'$'}n" > "$1"; echo "run ${'$'}n"; [ "${'$'}n" -ge 2 ] && exit 0; shift; exec "$@""""
        val r = suite(listOf("bash", "-c", code, "fixture", counter.path) + probe)
        val made = crashes(reports) - before
        try {
            assertEquals(r.output, 0, r.status)
            assertEquals(r.output, 2, r.runs)
            assertTrue(r.output, r.output.contains("test-suite: run 1 ended on a blocked safepoint"))
            assertTrue("no new crash report in $reports", made.isNotEmpty())
            assertTrue("a crash report in the working folder", crashes(tree).isEmpty())
        } finally {
            made.forEach { it.delete() }
        }
    }
}
