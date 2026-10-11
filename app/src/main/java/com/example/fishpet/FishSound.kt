package com.example.fishpet

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper

enum class FishSoundEffect { FEED, EAT, HAPPY, STARVING, DEATH, HATCH }

object FishSoundSettings {
    private const val PREFERENCES = "fishpet"
    private const val KEY_ENABLED = "sound_effects_enabled"

    fun isEnabled(context: Context): Boolean = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        .getBoolean(KEY_ENABLED, true)

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }
}

object FishSoundPolicy {
    fun shouldPlay(enabled: Boolean, isSilent: Boolean, mediaVolume: Int): Boolean =
        enabled && !isSilent && mediaVolume > 0
}

/** Short generated tones avoid bundling media, keep the app small, and never block pet updates. */
object FishSounds {
    private val mainHandler = Handler(Looper.getMainLooper())

    fun play(context: Context, effect: FishSoundEffect) {
        try {
            val audio = context.getSystemService(AudioManager::class.java)
            if (!FishSoundPolicy.shouldPlay(
                    enabled = FishSoundSettings.isEnabled(context),
                    isSilent = audio.ringerMode == AudioManager.RINGER_MODE_SILENT,
                    mediaVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC),
                )
            ) return
            val tone = ToneGenerator(AudioManager.STREAM_MUSIC, 35)
            val (toneType, durationMs) = when (effect) {
                FishSoundEffect.FEED -> ToneGenerator.TONE_PROP_BEEP2 to 100
                FishSoundEffect.EAT -> ToneGenerator.TONE_PROP_BEEP to 65
                FishSoundEffect.HAPPY -> ToneGenerator.TONE_PROP_ACK to 110
                FishSoundEffect.STARVING -> ToneGenerator.TONE_PROP_NACK to 130
                FishSoundEffect.DEATH -> ToneGenerator.TONE_PROP_NACK to 180
                FishSoundEffect.HATCH -> ToneGenerator.TONE_PROP_PROMPT to 150
            }
            if (tone.startTone(toneType, durationMs)) {
                mainHandler.postDelayed({ tone.release() }, durationMs.toLong() + 80L)
            } else {
                tone.release()
            }
        } catch (_: RuntimeException) {
            // Audio routing and background playback vary by device; pet care must always continue.
        }
    }

    fun playDelayed(context: Context, effect: FishSoundEffect, delayMs: Long) {
        val appContext = context.applicationContext
        mainHandler.postDelayed({ play(appContext, effect) }, delayMs)
    }
}
