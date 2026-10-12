package com.example.fishpet

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

enum class SwimmingState { HEALTHY, HUNGRY, STARVING, CRITICAL, DEAD }

enum class EatingPhase { DROP, APPROACH, BITE, CELEBRATE }

data class FishHudPosition(val x: Float, val y: Float)

object FishHudLayout {
    fun position(pose: SwimmingPose, bodyHeight: Float): FishHudPosition = FishHudPosition(
        x = 100f + pose.horizontalFraction * 520f,
        y = 210f + pose.bobOffset - bodyHeight * .70f - 26f,
    )
}

data class SwimmingPose(
    val horizontalFraction: Float,
    val bobOffset: Float,
    val tailAngleDegrees: Float,
    val finAngleDegrees: Float,
    val bodyTiltDegrees: Float,
    val facingScaleX: Float,
    val energy: Float,
    val tempo: Float,
    val isEating: Boolean,
    val eatingPhase: EatingPhase? = null,
    val mouthOpenFraction: Float = 0f,
    val eyeBlinkFraction: Float = 0f,
    val reaction: FishReaction = FishReaction.NONE,
    val reactionProgress: Float = 0f,
)

/**
 * Pure pose calculator shared by static widget snapshots and the companion preview.
 * It deliberately stores no animation state; time selects the current frame.
 */
object FishMotion {
    fun pose(state: FishState, nowMillis: Long): SwimmingPose {
        if (state.dead) return SwimmingPose(.52f, 0f, 0f, 0f, 15f, 1f, 0f, 0f, false)

        val nowSeconds = nowMillis / 1000L
        val eating = state.isFeeding(nowSeconds)
        val playing = state.isPlayful(nowSeconds)
        val swimState = state.swimmingState
        val stageTempo = when (state.stage()) {
            GrowthStage.BABY -> 1.32f
            GrowthStage.JUVENILE -> 1f
            GrowthStage.ADULT -> .76f
        }
        val energy = when (swimState) {
            SwimmingState.HEALTHY -> 1f
            SwimmingState.HUNGRY -> .72f
            SwimmingState.STARVING -> .42f
            SwimmingState.CRITICAL -> .16f
            SwimmingState.DEAD -> 0f
        }
        val playfulBoost = if (playing) 1.16f else 1f
        val tempo = stageTempo * energy.coerceAtLeast(.12f) * playfulBoost
        val phase = nowMillis / 1_000.0 * (Math.PI * 2.0 / 10.5) * tempo
        val travel = sin(phase).toFloat()
        val direction = cos(phase).toFloat()
        val turnCompression = max(.18f, abs(direction))

        if (eating) {
            val phaseProgress = feedingProgress(state, nowMillis)
            val eatingPhase = when {
                phaseProgress < .24f -> EatingPhase.DROP
                phaseProgress < .52f -> EatingPhase.APPROACH
                phaseProgress < .84f -> EatingPhase.BITE
                else -> EatingPhase.CELEBRATE
            }
            val bite = sin(phase * 5.0).toFloat()
            return when (eatingPhase) {
                EatingPhase.DROP -> SwimmingPose(.52f, bite * 2f, bite * 7f, bite * 4f, -3f, 1f, energy, tempo, true, eatingPhase, .08f)
                EatingPhase.APPROACH -> SwimmingPose(.58f, bite * 3f, bite * 13f, bite * 7f, -6f, 1f, energy, tempo, true, eatingPhase, .20f)
                EatingPhase.BITE -> SwimmingPose(.63f, bite * 2f, bite * 8f, bite * 5f, -8f, 1f, energy, tempo, true, eatingPhase, .70f + abs(bite) * .25f)
                EatingPhase.CELEBRATE -> SwimmingPose(.65f, bite * 5f, bite * 18f, bite * 11f, -2f, 1f, energy, tempo, true, eatingPhase, .28f)
            }
        }
        val base = SwimmingPose(
            horizontalFraction = .50f + travel * .30f,
            bobOffset = sin(phase * 2.0).toFloat() * 7f * energy,
            tailAngleDegrees = sin(phase * 6.0).toFloat() * (22f * energy + if (playing) 5f else 0f),
            finAngleDegrees = sin(phase * 5.0 + .7).toFloat() * 10f * energy,
            bodyTiltDegrees = sin(phase * 2.0).toFloat() * 3f * energy,
            facingScaleX = if (direction >= 0f) turnCompression else -turnCompression,
            energy = energy,
            tempo = tempo,
            isEating = false,
        )
        if (!state.isReacting(nowSeconds)) return base

        val reactionProgress = reactionProgress(state, nowSeconds)
        return when (state.reaction) {
            FishReaction.WIGGLE -> base.copy(
                tailAngleDegrees = sin(reactionProgress * Math.PI * 6.0).toFloat() * 34f,
                bodyTiltDegrees = sin(reactionProgress * Math.PI * 6.0).toFloat() * 8f,
                reaction = state.reaction,
                reactionProgress = reactionProgress,
            )
            FishReaction.BLINK -> base.copy(
                eyeBlinkFraction = if (reactionProgress in .22f..58f) 1f else .10f,
                reaction = state.reaction,
                reactionProgress = reactionProgress,
            )
            FishReaction.RIPPLE -> base.copy(
                bobOffset = -4f + sin(reactionProgress * Math.PI * 4.0).toFloat() * 8f,
                reaction = state.reaction,
                reactionProgress = reactionProgress,
            )
            FishReaction.HEART -> base.copy(
                bobOffset = -12f,
                finAngleDegrees = 18f,
                reaction = state.reaction,
                reactionProgress = reactionProgress,
            )
            FishReaction.BUBBLES -> base.copy(
                bobOffset = -6f,
                reaction = state.reaction,
                reactionProgress = reactionProgress,
            )
            FishReaction.NONE -> base
        }
    }

    private fun feedingProgress(state: FishState, nowMillis: Long): Float {
        val durationMillis = max(1L, state.feedingSceneUntilSeconds - state.lastFedAtSeconds) * 1_000L
        return ((nowMillis - state.lastFedAtSeconds * 1_000L).toFloat() / durationMillis).coerceIn(0f, 1f)
    }

    private fun reactionProgress(state: FishState, nowSeconds: Long): Float {
        val duration = FishBalance().reactionSceneSeconds.coerceAtLeast(1L)
        val startedAt = state.reactionSceneUntilSeconds - duration
        return ((nowSeconds - startedAt).toFloat() / duration).coerceIn(0f, 1f)
    }
}
