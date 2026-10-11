package com.example.fishpet

import kotlin.math.max
import kotlin.math.min

enum class GrowthStage(val label: String) { BABY("Baby"), JUVENILE("Juvenile"), ADULT("Adult") }

enum class HungerStatus(val label: String) { FULL("Full"), HUNGRY("Hungry"), STARVING("Starving"), CRITICAL("Critical") }

enum class FishMood(val label: String) { HAPPY("Happy"), WORRIED("Worried"), SAD("Unhealthy"), CRITICAL("Weak"), DEAD("Dead") }

enum class FeedResult { NONE, FED, COOLDOWN, DEAD }

data class FishBalance(
    val hungerPerHour: Double = 3.0,
    val healthyGrowthPerHour: Double = 0.60,
    val feedingCooldownSeconds: Long = 10L * 60L,
    val feedHungerRestore: Double = 45.0,
    val feedHealthRestore: Double = 8.0,
    val feedingSceneSeconds: Long = 12L,
    val maxSimulatedElapsedSeconds: Long = 30L * 24L * 60L * 60L,
    val juvenileGrowthThreshold: Double = 25.0,
    val adultGrowthThreshold: Double = 75.0,
)

data class FishState(
    val name: String = "Bubbles",
    val hunger: Double = 15.0,
    val health: Double = 100.0,
    val growth: Double = 0.0,
    val updatedAtSeconds: Long = System.currentTimeMillis() / 1000L,
    val lastFedAtSeconds: Long = 0L,
    val bornAtSeconds: Long = updatedAtSeconds,
    val feedingSceneUntilSeconds: Long = 0L,
    val playfulSceneUntilSeconds: Long = 0L,
    val foodEaten: Int = 0,
    val dead: Boolean = false,
) {
    fun stage(balance: FishBalance = FishBalance()): GrowthStage = when {
        growth < balance.juvenileGrowthThreshold -> GrowthStage.BABY
        growth < balance.adultGrowthThreshold -> GrowthStage.JUVENILE
        else -> GrowthStage.ADULT
    }

    val hungerStatus: HungerStatus
        get() = when {
            hunger < 35.0 -> HungerStatus.FULL
            hunger < 70.0 -> HungerStatus.HUNGRY
            hunger < 90.0 -> HungerStatus.STARVING
            else -> HungerStatus.CRITICAL
        }

    val mood: FishMood
        get() = when {
            dead -> FishMood.DEAD
            health < 20.0 || hungerStatus == HungerStatus.CRITICAL -> FishMood.CRITICAL
            health < 50.0 || hungerStatus == HungerStatus.STARVING -> FishMood.SAD
            hungerStatus == HungerStatus.HUNGRY -> FishMood.WORRIED
            else -> FishMood.HAPPY
        }

    fun ageSeconds(nowSeconds: Long): Long = max(0L, nowSeconds - bornAtSeconds)
    fun isFeeding(nowSeconds: Long): Boolean = !dead && nowSeconds < feedingSceneUntilSeconds
    fun isPlayful(nowSeconds: Long): Boolean = !dead && nowSeconds < playfulSceneUntilSeconds

    val swimmingState: SwimmingState
        get() = when {
            dead -> SwimmingState.DEAD
            mood == FishMood.CRITICAL -> SwimmingState.CRITICAL
            mood == FishMood.SAD -> SwimmingState.STARVING
            mood == FishMood.WORRIED -> SwimmingState.HUNGRY
            else -> SwimmingState.HEALTHY
        }
}

data class FishUpdate(val state: FishState, val result: FeedResult = FeedResult.NONE)

/** Calculates one best-effort widget refresh for the latest transient visual scene. */
object FishSceneTiming {
    fun secondsUntilRefresh(state: FishState, nowSeconds: Long): Long? {
        val latestSceneEnd = max(state.feedingSceneUntilSeconds, state.playfulSceneUntilSeconds)
        return (latestSceneEnd - nowSeconds).takeIf { it > 0L }
    }
}

object FishSimulator {
    private const val STEP_SECONDS = 15L * 60L

    fun advance(initial: FishState, nowSeconds: Long, balance: FishBalance = FishBalance()): FishState {
        val safeInitial = initial.copy(
            hunger = safeValue(initial.hunger, 15.0).coerceIn(0.0, 100.0),
            health = safeValue(initial.health, 100.0).coerceIn(0.0, 100.0),
            growth = safeValue(initial.growth, 0.0).coerceIn(0.0, 100.0),
        )
        val normalized = safeInitial.copy(dead = safeInitial.dead || safeInitial.health <= 0.0)
        if (nowSeconds <= normalized.updatedAtSeconds) return normalized
        if (normalized.dead) return normalized.copy(updatedAtSeconds = nowSeconds)

        val elapsed = min(nowSeconds - normalized.updatedAtSeconds, balance.maxSimulatedElapsedSeconds)
        var remaining = elapsed
        var hunger = normalized.hunger
        var health = normalized.health
        var growth = normalized.growth
        var dead = normalized.dead

        while (remaining > 0L && !dead) {
            val seconds = min(remaining, STEP_SECONDS)
            val hours = seconds / 3600.0
            hunger = min(100.0, hunger + balance.hungerPerHour * hours)
            health = (health + healthDeltaPerHour(hunger) * hours).coerceIn(0.0, 100.0)
            if (hunger < 65.0 && health >= 60.0) {
                growth = min(100.0, growth + balance.healthyGrowthPerHour * hours)
            }
            dead = health <= 0.0
            remaining -= seconds
        }
        return normalized.copy(
            hunger = hunger,
            health = health,
            growth = growth,
            updatedAtSeconds = nowSeconds,
            dead = dead,
        )
    }

    fun feed(initial: FishState, nowSeconds: Long, balance: FishBalance = FishBalance()): FishUpdate {
        val advanced = advance(initial, nowSeconds, balance)
        if (advanced.dead) return FishUpdate(advanced, FeedResult.DEAD)
        if (advanced.lastFedAtSeconds > 0L && nowSeconds - advanced.lastFedAtSeconds < balance.feedingCooldownSeconds) {
            return FishUpdate(advanced, FeedResult.COOLDOWN)
        }
        return FishUpdate(
            advanced.copy(
                hunger = max(0.0, advanced.hunger - balance.feedHungerRestore),
                health = min(100.0, advanced.health + balance.feedHealthRestore),
                lastFedAtSeconds = nowSeconds,
                feedingSceneUntilSeconds = nowSeconds + balance.feedingSceneSeconds,
                foodEaten = advanced.foodEaten + 1,
            ),
            FeedResult.FED,
        )
    }

    fun pet(initial: FishState, nowSeconds: Long): FishState {
        val advanced = advance(initial, nowSeconds)
        return if (advanced.dead) advanced else advanced.copy(playfulSceneUntilSeconds = nowSeconds + 8L)
    }

    private fun healthDeltaPerHour(hunger: Double): Double = when {
        hunger < 55.0 -> 0.75
        hunger < 70.0 -> 0.20
        hunger < 85.0 -> -1.50
        hunger < 95.0 -> -4.00
        else -> -10.0
    }

    private fun safeValue(value: Double, fallback: Double): Double = if (value.isFinite()) value else fallback
}
