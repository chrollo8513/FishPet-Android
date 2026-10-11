package com.example.fishpet

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FishSoundPolicyTest {
    @Test
    fun `sound only plays when enabled audible and media volume is positive`() {
        assertTrue(FishSoundPolicy.shouldPlay(enabled = true, isSilent = false, mediaVolume = 1))
        assertFalse(FishSoundPolicy.shouldPlay(enabled = false, isSilent = false, mediaVolume = 10))
        assertFalse(FishSoundPolicy.shouldPlay(enabled = true, isSilent = true, mediaVolume = 10))
        assertFalse(FishSoundPolicy.shouldPlay(enabled = true, isSilent = false, mediaVolume = 0))
    }

    @Test
    fun `each generated effect contains audible waveform samples`() {
        FishSoundEffect.entries.forEach { effect ->
            assertTrue(FishSoundWaveform.samples(effect).any { it != 0.toShort() })
        }
    }
}
