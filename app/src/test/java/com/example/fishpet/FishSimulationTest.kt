package com.example.fishpet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FishSimulationTest {
    private val balance = FishBalance(
        hungerPerHour = 0.0,
        healthyGrowthPerHour = 1.0,
        feedingCooldownSeconds = 60,
        maxSimulatedElapsedSeconds = 60L * 60L * 24L * 30L,
    )

    @Test
    fun `healthy care advances growth through all stages`() {
        val baby = FishState(growth = 0.0, hunger = 0.0, health = 100.0, updatedAtSeconds = 0L)

        val juvenile = FishSimulator.advance(baby, nowSeconds = 25L * 3600L, balance = balance)
        val adult = FishSimulator.advance(juvenile, nowSeconds = 80L * 3600L, balance = balance)

        assertEquals(GrowthStage.JUVENILE, juvenile.stage())
        assertEquals(GrowthStage.ADULT, adult.stage())
        assertTrue(adult.growth > juvenile.growth)
    }

    @Test
    fun `prolonged starvation kills the fish and persists death`() {
        val starving = FishState(hunger = 96.0, health = 3.0, updatedAtSeconds = 0L)

        val dead = FishSimulator.advance(starving, nowSeconds = 3600L, balance = FishBalance())
        val later = FishSimulator.advance(dead, nowSeconds = 7200L, balance = FishBalance())

        assertTrue(dead.dead)
        assertTrue(later.dead)
        assertEquals(0.0, later.health, 0.0)
    }

    @Test
    fun `feed restores care then rejects rapid duplicate tap`() {
        val hungry = FishState(hunger = 80.0, health = 50.0, updatedAtSeconds = 100L)

        val first = FishSimulator.feed(hungry, nowSeconds = 200L, balance = balance)
        val duplicate = FishSimulator.feed(first.state, nowSeconds = 230L, balance = balance)

        assertEquals(FeedResult.FED, first.result)
        assertTrue(first.state.hunger < hungry.hunger)
        assertTrue(first.state.health > hungry.health)
        assertEquals(FeedResult.COOLDOWN, duplicate.result)
        assertEquals(first.state.foodEaten, duplicate.state.foodEaten)
    }

    @Test
    fun `dead fish cannot be fed`() {
        val dead = FishState(dead = true, health = 0.0, updatedAtSeconds = 100L)

        val update = FishSimulator.feed(dead, nowSeconds = 200L, balance = balance)

        assertEquals(FeedResult.DEAD, update.result)
        assertEquals(0, update.state.foodEaten)
    }

    @Test
    fun `clock rollback leaves care state unchanged`() {
        val current = FishState(hunger = 42.0, health = 75.0, growth = 18.0, updatedAtSeconds = 10_000L)

        val rolledBack = FishSimulator.advance(current, nowSeconds = 9_000L, balance = FishBalance())

        assertEquals(42.0, rolledBack.hunger, 0.0)
        assertEquals(75.0, rolledBack.health, 0.0)
        assertEquals(18.0, rolledBack.growth, 0.0)
        assertEquals(10_000L, rolledBack.updatedAtSeconds)
        assertFalse(rolledBack.dead)
    }

    @Test
    fun `invalid persisted values are clamped into a safe dead state`() {
        val corrupted = FishState(hunger = 500.0, health = -30.0, growth = 999.0, updatedAtSeconds = 0L)

        val safe = FishSimulator.advance(corrupted, nowSeconds = 1L, balance = balance)

        assertEquals(100.0, safe.hunger, 0.0)
        assertEquals(0.0, safe.health, 0.0)
        assertEquals(100.0, safe.growth, 0.0)
        assertTrue(safe.dead)
    }

    @Test
    fun `large clock jump is capped while retaining the current timestamp`() {
        val initial = FishState(hunger = 0.0, health = 100.0, growth = 0.0, updatedAtSeconds = 0L)
        val limited = balance.copy(healthyGrowthPerHour = 0.1, maxSimulatedElapsedSeconds = 30L * 24L * 3600L)

        val advanced = FishSimulator.advance(initial, nowSeconds = 90L * 24L * 3600L, balance = limited)

        assertEquals(72.0, advanced.growth, 0.000_001)
        assertEquals(90L * 24L * 3600L, advanced.updatedAtSeconds)
    }

    @Test
    fun `feeding scene expires from its persisted timestamp without an alarm`() {
        val fed = FishState(feedingSceneUntilSeconds = 112L)

        assertTrue(fed.isFeeding(111L))
        assertFalse(fed.isFeeding(112L))
    }

    @Test
    fun `scene cleanup waits for the latest overlapping interaction`() {
        val state = FishState(feedingSceneUntilSeconds = 112L, playfulSceneUntilSeconds = 110L)

        assertEquals(12L, FishSceneTiming.secondsUntilRefresh(state, nowSeconds = 100L))
        assertEquals(2L, FishSceneTiming.secondsUntilRefresh(state, nowSeconds = 110L))
        assertEquals(null, FishSceneTiming.secondsUntilRefresh(state, nowSeconds = 112L))
    }

    @Test
    fun `feeding schedules a final celebration frame before restoring the normal pose`() {
        val state = FishState(lastFedAtSeconds = 100L, feedingSceneUntilSeconds = 112L)

        assertEquals(103L, FishSceneTiming.nextRefreshAtSeconds(state, nowSeconds = 100L))
        assertEquals(107L, FishSceneTiming.nextRefreshAtSeconds(state, nowSeconds = 103L))
        assertEquals(111L, FishSceneTiming.nextRefreshAtSeconds(state, nowSeconds = 107L))
        assertEquals(EatingPhase.CELEBRATE, FishMotion.pose(state, nowMillis = 111_000L).eatingPhase)
        assertEquals(112L, FishSceneTiming.nextRefreshAtSeconds(state, nowSeconds = 111L))
    }

    @Test
    fun `pet interaction creates a short playful scene only for living fish`() {
        val lively = FishSimulator.pet(FishState(updatedAtSeconds = 100L), nowSeconds = 100L)
        val dead = FishSimulator.pet(FishState(dead = true, health = 0.0, updatedAtSeconds = 100L), nowSeconds = 100L)

        assertTrue(lively.isPlayful(107L))
        assertFalse(lively.isPlayful(108L))
        assertFalse(dead.isPlayful(101L))
    }

    @Test
    fun `pet interaction persists its selected reaction and schedules short scene frames`() {
        val reacted = FishSimulator.pet(
            FishState(updatedAtSeconds = 100L),
            nowSeconds = 100L,
            reaction = FishReaction.HEART,
        )

        assertEquals(FishReaction.HEART, reacted.reaction)
        assertTrue(reacted.isReacting(103L))
        assertEquals(101L, FishSceneTiming.nextRefreshAtSeconds(reacted, nowSeconds = 100L))
        assertEquals(104L, FishSceneTiming.nextRefreshAtSeconds(reacted, nowSeconds = 103L))
        assertEquals(null, FishSceneTiming.nextRefreshAtSeconds(reacted, nowSeconds = 108L))
    }

    @Test
    fun `dead fish does not start the requested reaction`() {
        val dead = FishSimulator.pet(
            FishState(dead = true, health = 0.0, updatedAtSeconds = 100L),
            nowSeconds = 100L,
            reaction = FishReaction.RIPPLE,
        )

        assertEquals(FishReaction.NONE, dead.reaction)
        assertFalse(dead.isReacting(101L))
    }

    @Test
    fun `new baby keeps its chosen color and starts with fresh care values`() {
        val baby = FishSimulator.newBaby(nowSeconds = 120L, color = FishColor.KOI)

        assertEquals(FishColor.KOI, baby.color)
        assertEquals("Bubbles", baby.name)
        assertEquals(0.0, baby.growth, 0.0)
        assertFalse(baby.dead)
    }

    @Test
    fun `renaming trims valid names and keeps the old name for blank input`() {
        val original = FishState(name = "Bubbles")

        assertEquals("Mango", FishSimulator.rename(original, "  Mango  ").name)
        assertEquals("Bubbles", FishSimulator.rename(original, "   ").name)
    }
}
