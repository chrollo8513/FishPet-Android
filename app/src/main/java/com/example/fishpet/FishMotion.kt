package com.example.fishpet

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

enum class SwimmingState { HEALTHY, HUNGRY, STARVING, CRITICAL, DEAD }

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
            val bite = sin(phase * 5.0).toFloat()
            return SwimmingPose(.63f, bite * 2f, bite * 8f, bite * 5f, -8f, 1f, energy, tempo, true)
        }
        return SwimmingPose(
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
    }
}
