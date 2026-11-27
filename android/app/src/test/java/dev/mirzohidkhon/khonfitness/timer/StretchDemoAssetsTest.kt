package dev.mirzohidkhon.khonfitness.timer

import dev.mirzohidkhon.khonfitness.data.DemoView
import dev.mirzohidkhon.khonfitness.data.StretchDemo
import dev.mirzohidkhon.khonfitness.data.demoPhotoPath
import dev.mirzohidkhon.khonfitness.ui.components.Figures
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** The shipped photos and guides.json, read from the source assets. */
class StretchDemoAssetsTest {
    private val assets = File("src/main/assets")
    private val guides = Json.decodeFromString<Map<String, StretchDemo>>(File(assets, "stretch_demos/guides.json").readText())

    @Test fun `every figure key has a guide`() {
        assertEquals(Figures.keys.toSet(), guides.keys)
    }

    @Test fun `every photo a step or a rep names exists in both views`() {
        for ((key, guide) in guides) for (pose in guide.steps.map { it.pose } + guide.reps) for (view in DemoView.entries) {
            val path = demoPhotoPath(key, pose, view)
            assertTrue("$path is missing", File(assets, path).isFile)
        }
    }

    @Test fun `the folder holds only guides json and the photos the guides name`() {
        val named = guides.flatMap { (key, guide) ->
            (guide.steps.map { it.pose } + guide.reps).flatMap { pose -> DemoView.entries.map { demoPhotoPath(key, pose, it).substringAfter('/') } }
        }.toSet()
        assertEquals(named + "guides.json", File(assets, "stretch_demos").list()!!.toSet())
        assertEquals(38, named.size)
    }

    @Test fun `each step shows the photo the owner chose`() {
        val table = mapOf(
            "wgs" to "start start hold", "needle" to "start hold hold", "ninety" to "start hold hold hold",
            "plow" to "start start hold hold", "hipflexor" to "start start hold", "fold" to "start hold hold",
            "elephant" to "start hold hold", "catcow" to "setup start hold", "shoulderir" to "start hold hold",
        )
        assertEquals(table, guides.mapValues { (_, g) -> g.steps.joinToString(" ") { it.pose } })
    }

    @Test fun `only the stretches that move have rep photos, start then hold`() {
        assertEquals(setOf("wgs", "elephant", "catcow"), guides.filterValues { it.moving }.keys)
        for ((key, guide) in guides) assertEquals(key, if (guide.moving) listOf("start", "hold") else emptyList(), guide.reps)
    }
}
