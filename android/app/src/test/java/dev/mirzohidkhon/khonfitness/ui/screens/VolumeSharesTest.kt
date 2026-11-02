package dev.mirzohidkhon.khonfitness.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class VolumeSharesTest {
    @Test fun `bars are shares of the biggest count of either week`() {
        val shares = volumeShares(listOf(MuscleVolume("chest", 6.0, 12.0), MuscleVolume("back", 3.0, 0.0)))
        assertEquals(VolumeShare(0.5, 1.0), shares[0])
        assertEquals(VolumeShare(0.25, 0.0), shares[1])
    }

    @Test fun `a small count still shows and zero stays zero`() {
        val shares = volumeShares(listOf(MuscleVolume("chest", 100.0, 0.5), MuscleVolume("calves", 0.0, 0.0)))
        assertEquals(0.02, shares[0].last, 0.0)
        assertEquals(VolumeShare(0.0, 0.0), shares[1])
    }

    @Test fun `counts under one keep their size against one`() {
        assertEquals(VolumeShare(0.5, 0.0), volumeShares(listOf(MuscleVolume("chest", 0.5, 0.0)))[0])
    }
}
