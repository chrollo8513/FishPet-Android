package com.example.fishpet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FishMotionTest {
    @Test
    fun `healthy fish swims inside the aquarium with visible motion`() {
        val pose = FishMotion.pose(FishState(), nowMillis = 2_000L)

        assertTrue(pose.horizontalFraction in 0.18f..0.82f)
        assertTrue(pose.tailAngleDegrees != 0f)
        assertTrue(pose.energy > 0.9f)
    }

    @Test
    fun `starving and critical fish move less than healthy fish`() {
        val healthy = FishMotion.pose(FishState(health = 100.0, hunger = 10.0), nowMillis = 2_000L)
        val starving = FishMotion.pose(FishState(health = 45.0, hunger = 78.0), nowMillis = 2_000L)
        val critical = FishMotion.pose(FishState(health = 15.0, hunger = 95.0), nowMillis = 2_000L)

        assertTrue(starving.energy < healthy.energy)
        assertTrue(critical.energy < starving.energy)
        assertTrue(kotlin.math.abs(critical.tailAngleDegrees) < kotlin.math.abs(starving.tailAngleDegrees))
    }

    @Test
    fun `baby fish has a faster swim tempo than an adult`() {
        val baby = FishMotion.pose(FishState(growth = 0.0), nowMillis = 2_000L)
        val adult = FishMotion.pose(FishState(growth = 90.0), nowMillis = 2_000L)

        assertTrue(baby.tempo > adult.tempo)
    }

    @Test
    fun `dead fish is motionless and cannot enter an eating pose`() {
        val pose = FishMotion.pose(FishState(dead = true, health = 0.0, feedingSceneUntilSeconds = 9_000L), nowMillis = 2_000L)

        assertEquals(0f, pose.tailAngleDegrees, 0f)
        assertEquals(0f, pose.bobOffset, 0f)
        assertFalse(pose.isEating)
    }

    @Test
    fun `feeding scene produces an eating pose`() {
        val pose = FishMotion.pose(FishState(feedingSceneUntilSeconds = 3_000L), nowMillis = 2_000L)

        assertTrue(pose.isEating)
        assertTrue(pose.horizontalFraction > 0.5f)
    }

    @Test
    fun `floating care badges remain above and follow the fish pose`() {
        val left = FishHudLayout.position(
            SwimmingPose(.25f, 2f, 0f, 0f, 0f, 1f, 1f, 1f, false),
            bodyHeight = 60f,
        )
        val right = FishHudLayout.position(
            SwimmingPose(.70f, -3f, 0f, 0f, 0f, 1f, 1f, 1f, false),
            bodyHeight = 60f,
        )

        assertTrue(left.y < 210f)
        assertTrue(right.x > left.x)
    }
}
