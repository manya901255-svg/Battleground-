package com.example.game.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

class SoundSynthesizer(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.Default)
    private val sampleRate = 22050
    private var isMuted = false
    @Volatile private var masterVolume = 1.0f
    @Volatile private var sfxVolume = 1.0f
    @Volatile private var bgmVolume = 0.55f
    private var bgmJob: Job? = null

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun setMuted(muted: Boolean) {
        isMuted = muted
    }

    fun setVolume(master: Float, sfx: Float, bgm: Float = bgmVolume) {
        masterVolume = master.coerceIn(0f, 1f)
        sfxVolume = sfx.coerceIn(0f, 1f)
        bgmVolume = bgm.coerceIn(0f, 1f)
    }

    fun startBackgroundMusic() {
        if (bgmJob?.isActive == true) return
        bgmJob = scope.launch {
            var track: AudioTrack? = null
            try {
                val loopSamples = sampleRate * 4 // 4-second ambient tactical pulse loop
                val baseBuffer = ShortArray(loopSamples)
                for (i in 0 until loopSamples) {
                    val t = i.toDouble() / sampleRate
                    val beat = (t * 2.0) % 1.0
                    val bassPulse = sin(2.0 * PI * 55.0 * t) * exp(-beat * 5.0) * 0.45
                    val pad = (sin(2.0 * PI * 110.0 * t) + sin(2.0 * PI * 164.81 * t) * 0.6) * 0.18
                    val arpFreq = if (((t * 2.0).toInt() % 4) < 2) 220.0 else 261.63
                    val arp = sin(2.0 * PI * arpFreq * t) * exp(-beat * 9.0) * 0.12
                    baseBuffer[i] = ((bassPulse + pad + arp) * 14000.0).toInt().coerceIn(-32767, 32767).toShort()
                }

                track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(loopSamples * 2)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
                track.play()

                val chunk = ShortArray(sampleRate / 4)
                var idx = 0
                while (isActive) {
                    val gain = if (isMuted) 0f else (masterVolume * bgmVolume)
                    for (j in chunk.indices) {
                        chunk[j] = (baseBuffer[(idx + j) % loopSamples] * gain).toInt().coerceIn(-32767, 32767).toShort()
                    }
                    idx = (idx + chunk.size) % loopSamples
                    track.write(chunk, 0, chunk.size)
                }
            } catch (_: Exception) {
                // Ignore audio hardware unavailable in test/headless environments
            } finally {
                try {
                    track?.stop()
                    track?.release()
                } catch (_: Exception) {}
            }
        }
    }

    fun stopBackgroundMusic() {
        bgmJob?.cancel()
        bgmJob = null
    }

    private fun playPcm(buffer: ShortArray) {
        val gain = masterVolume * sfxVolume
        if (isMuted || gain <= 0.01f) return
        val scaledBuffer = if (gain >= 0.99f) buffer else ShortArray(buffer.size) { i ->
            (buffer[i] * gain).toInt().coerceIn(-32767, 32767).toShort()
        }
        scope.launch {
            try {
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(scaledBuffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(scaledBuffer, 0, scaledBuffer.size)
                track.play()
                // Auto-release after playing without blocking Dispatchers.Default thread
                val durationMs = (scaledBuffer.size * 1000L) / sampleRate
                kotlinx.coroutines.delay(durationMs + 30)
                track.stop()
                track.release()
            } catch (_: Exception) {
                // Ignore audio hardware busy exceptions
            }
        }
    }

    fun playRadioChirp() {
        val durationMs = 110
        val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val tone = sin(2.0 * PI * 1450.0 * t) * 0.6
            val env = exp(-t * 16.0)
            buffer[i] = (tone * env * 24000.0).toInt().coerceIn(-32767, 32767).toShort()
        }
        playPcm(buffer)
    }

    fun playGunfire(weaponName: String, isSilenced: Boolean = false) {
        vibrateGunRecoil()
        val durationMs = if (weaponName.contains("AWM") || weaponName.contains("Kar98k")) 250 else 90
        val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
        val buffer = ShortArray(numSamples)

        val baseFreq = when {
            weaponName.contains("AWM") -> 110.0
            weaponName.contains("Kar98k") -> 140.0
            weaponName.contains("AKM") -> 170.0
            weaponName.contains("M416") -> 240.0
            weaponName.contains("UMP") -> 300.0
            weaponName.contains("S12K") -> 95.0
            else -> 220.0
        }

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val decay = exp(-t * 22.0)
            val noise = (Random.nextDouble() * 2.0 - 1.0) * if (isSilenced) 0.15 else 0.45
            val tone = sin(2.0 * PI * baseFreq * t) * 0.55
            val sampleVal = (decay * (tone + noise) * 32000.0).toInt().coerceIn(-32767, 32767)
            buffer[i] = sampleVal.toShort()
        }
        playPcm(buffer)
    }

    fun playHeadshotDing() {
        val durationMs = 180
        val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val decay = exp(-t * 12.0)
            val tone = sin(2.0 * PI * 1850.0 * t) * 0.7 + sin(2.0 * PI * 2400.0 * t) * 0.3
            buffer[i] = (decay * tone * 32000.0).toInt().coerceIn(-32767, 32767).toShort()
        }
        playPcm(buffer)
    }

    fun playReload() {
        val durationMs = 300
        val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            // Double click: at t = 0.05s and t = 0.20s
            val click1 = exp(-((t - 0.05) * 60.0).let { it * it }) * sin(2.0 * PI * 800.0 * t)
            val click2 = exp(-((t - 0.20) * 60.0).let { it * it }) * sin(2.0 * PI * 1200.0 * t)
            buffer[i] = ((click1 + click2) * 28000.0).toInt().coerceIn(-32767, 32767).toShort()
        }
        playPcm(buffer)
    }

    fun playFootstep() {
        val durationMs = 45
        val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val decay = exp(-t * 60.0)
            val noise = (Random.nextDouble() * 2.0 - 1.0) * decay
            buffer[i] = (noise * 14000.0).toInt().coerceIn(-32767, 32767).toShort()
        }
        playPcm(buffer)
    }

    fun playExplosion() {
        vibrateExplosion()
        val durationMs = 500
        val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val decay = exp(-t * 5.5)
            val subBass = sin(2.0 * PI * (65.0 - t * 40.0) * t) * 0.6
            val noise = (Random.nextDouble() * 2.0 - 1.0) * 0.4
            buffer[i] = (decay * (subBass + noise) * 32000.0).toInt().coerceIn(-32767, 32767).toShort()
        }
        playPcm(buffer)
    }

    fun playVictoryFanfare() {
        val durationMs = 800
        val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val chord = sin(2.0 * PI * 523.25 * t) * 0.33 +
                    sin(2.0 * PI * 659.25 * t) * 0.33 +
                    sin(2.0 * PI * 783.99 * t) * 0.33
            val env = exp(-t * 2.0)
            buffer[i] = (chord * env * 30000.0).toInt().coerceIn(-32767, 32767).toShort()
        }
        playPcm(buffer)
    }

    fun playVehicleHorn() {
        val durationMs = 350
        val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val hornTone = sin(2.0 * PI * 420.0 * t) * 0.5 + sin(2.0 * PI * 490.0 * t) * 0.5
            val env = if (t < 0.05) t / 0.05 else if (t > 0.3) (0.35 - t) / 0.05 else 1.0
            buffer[i] = (hornTone * env * 28000.0).toInt().coerceIn(-32767, 32767).toShort()
        }
        playPcm(buffer)
    }

    fun playVehicleEngineRev() {
        val durationMs = 200
        val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val freq = 80.0 + t * 90.0
            val sub = sin(2.0 * PI * freq * t) * 0.7
            val grit = (Random.nextDouble() * 2.0 - 1.0) * 0.3
            buffer[i] = ((sub + grit) * 22000.0).toInt().coerceIn(-32767, 32767).toShort()
        }
        playPcm(buffer)
    }

    fun playNitroBoost() {
        val durationMs = 400
        val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val whoosh = (Random.nextDouble() * 2.0 - 1.0) * exp(-t * 2.5) * 0.8
            val flame = sin(2.0 * PI * (300.0 - t * 150.0) * t) * 0.4
            buffer[i] = ((whoosh + flame) * 30000.0).toInt().coerceIn(-32767, 32767).toShort()
        }
        playPcm(buffer)
    }

    fun playUiClick() {
        val durationMs = 45
        val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val tone = sin(2.0 * PI * 980.0 * t) * exp(-t * 55.0)
            buffer[i] = (tone * 18000.0).toInt().coerceIn(-32767, 32767).toShort()
        }
        playPcm(buffer)
    }

    fun playJump() {
        val durationMs = 110
        val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val freq = 140.0 + t * 520.0
            val tone = sin(2.0 * PI * freq * t) * exp(-t * 16.0)
            buffer[i] = (tone * 19000.0).toInt().coerceIn(-32767, 32767).toShort()
        }
        playPcm(buffer)
    }

    fun playShieldDeploy() {
        val durationMs = 260
        val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val tone = sin(2.0 * PI * (320.0 + t * 900.0) * t) * 0.65 + sin(2.0 * PI * 640.0 * t) * 0.35
            buffer[i] = (tone * exp(-t * 6.0) * 26000.0).toInt().coerceIn(-32767, 32767).toShort()
        }
        playPcm(buffer)
    }

    fun playShieldHit() {
        val durationMs = 95
        val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val shimmer = sin(2.0 * PI * 1320.0 * t) * exp(-t * 24.0)
            buffer[i] = (shimmer * 22000.0).toInt().coerceIn(-32767, 32767).toShort()
        }
        playPcm(buffer)
    }

    private fun vibrateGunRecoil() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(25)
            }
        } catch (_: Exception) {}
    }

    private fun vibrateExplosion() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 100, 50, 150), -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(200)
            }
        } catch (_: Exception) {}
    }
}
