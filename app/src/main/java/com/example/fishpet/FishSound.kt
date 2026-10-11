package com.example.fishpet

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioTrack
import android.media.AudioFormat
import android.os.Handler
import android.os.Looper
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sin

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

/** Tiny synthesized PCM effects avoid telephony-tone routing differences and bundled media files. */
object FishSoundWaveform {
    private const val SAMPLE_RATE = 11_025

    fun samples(effect: FishSoundEffect): ShortArray {
        val durationMs = when (effect) {
            FishSoundEffect.FEED -> 150
            FishSoundEffect.EAT -> 90
            FishSoundEffect.HAPPY -> 150
            FishSoundEffect.STARVING -> 180
            FishSoundEffect.DEATH -> 230
            FishSoundEffect.HATCH -> 180
        }
        val baseFrequency = when (effect) {
            FishSoundEffect.FEED -> 210.0
            FishSoundEffect.EAT -> 530.0
            FishSoundEffect.HAPPY -> 720.0
            FishSoundEffect.STARVING -> 250.0
            FishSoundEffect.DEATH -> 180.0
            FishSoundEffect.HATCH -> 640.0
        }
        val count = SAMPLE_RATE * durationMs / 1_000
        return ShortArray(count) { index ->
            val progress = index.toDouble() / count.coerceAtLeast(1)
            val envelope = (1.0 - progress).let { it * it }
            val frequency = when (effect) {
                FishSoundEffect.FEED, FishSoundEffect.DEATH -> baseFrequency * (1.0 - progress * .35)
                FishSoundEffect.HAPPY, FishSoundEffect.HATCH -> baseFrequency * (1.0 + progress * .35)
                else -> baseFrequency
            }
            val main = sin(2.0 * PI * frequency * index / SAMPLE_RATE)
            val sparkle = sin(2.0 * PI * frequency * 2.0 * index / SAMPLE_RATE) * .22
            ((main + sparkle) * envelope * 7_800.0).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
    }

    fun durationMs(effect: FishSoundEffect): Long = samples(effect).size * 1_000L / SAMPLE_RATE
}

/** A short audio-focus-aware sonification path that releases every resource after each effect. */
object FishSounds {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    fun play(context: Context, effect: FishSoundEffect) {
        val appContext = context.applicationContext
        val audio = appContext.getSystemService(AudioManager::class.java)
        if (!FishSoundPolicy.shouldPlay(
                enabled = FishSoundSettings.isEnabled(appContext),
                isSilent = audio.ringerMode == AudioManager.RINGER_MODE_SILENT,
                mediaVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC),
            )
        ) return

        val focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(attributes)
            .build()
        if (audio.requestAudioFocus(focus) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) return

        var track: AudioTrack? = null
        try {
            val waveform = FishSoundWaveform.samples(effect)
            track = AudioTrack.Builder()
                .setAudioAttributes(attributes)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(11_025)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                )
                .setBufferSizeInBytes(waveform.size * Short.SIZE_BYTES)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            track.write(waveform, 0, waveform.size)
            track.play()
            val playingTrack = track
            mainHandler.postDelayed({
                try {
                    playingTrack.stop()
                } catch (_: IllegalStateException) {
                    // The device can invalidate a short effect during an audio-route change.
                }
                playingTrack.release()
                audio.abandonAudioFocusRequest(focus)
            }, max(120L, FishSoundWaveform.durationMs(effect) + 90L))
        } catch (_: RuntimeException) {
            track?.release()
            audio.abandonAudioFocusRequest(focus)
        }
    }

    fun playDelayed(context: Context, effect: FishSoundEffect, delayMs: Long) {
        val appContext = context.applicationContext
        mainHandler.postDelayed({ play(appContext, effect) }, delayMs)
    }
}
