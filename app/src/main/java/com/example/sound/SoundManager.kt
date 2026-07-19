package com.example.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Vibrator
import android.os.VibratorManager
import android.os.Build
import android.os.VibrationEffect
import kotlinx.coroutines.*
import kotlin.math.sin
import kotlin.math.PI
import java.util.Random

class SoundManager(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var isMuted = false
    private var isVibrateEnabled = true
    private var volumeFactor = 0.7f // 0.3 for Kecil, 0.7 for Normal, 1.0 for Besar
    
    private var musicJob: Job? = null
    private val sharedPrefs = context.getSharedPreferences("BurungTerbangPrefs", Context.MODE_PRIVATE)

    init {
        // Load settings from SharedPreferences
        isMuted = !sharedPrefs.getBoolean("bgm_on", true)
        isVibrateEnabled = sharedPrefs.getBoolean("vibrate_on", true)
        val volStr = sharedPrefs.getString("volume", "Normal") ?: "Normal"
        volumeFactor = when (volStr) {
            "Kecil" -> 0.3f
            "Normal" -> 0.7f
            "Besar" -> 1.0f
            else -> 0.7f
        }
    }

    fun updateSettings(muted: Boolean, vibrate: Boolean, volume: String) {
        this.isMuted = muted
        this.isVibrateEnabled = vibrate
        this.volumeFactor = when (volume) {
            "Kecil" -> 0.3f
            "Normal" -> 0.7f
            "Besar" -> 1.0f
            else -> 0.7f
        }
        
        if (isMuted) {
            stopMusic()
        } else {
            startMusic()
        }
    }

    /**
     * Synthesizes a tone with a simple ADSR/exponential envelope.
     * Types:
     * - "sine": Smooth tone
     * - "triangle": Flute/woodwind tone, nice retro feel
     * - "pluck": String/metallic chime (fast decay)
     * - "noise": Exploding crash noise
     */
    fun playTone(freqStart: Double, freqEnd: Double, durationMs: Int, type: String = "sine") {
        if (volumeFactor <= 0.05f) return

        scope.launch {
            try {
                val sampleRate = 22050
                val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
                if (numSamples <= 0) return@launch
                
                val buffer = ShortArray(numSamples)
                val random = Random()
                
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    val progress = i.toDouble() / numSamples
                    val freq = freqStart + (freqEnd - freqStart) * progress
                    
                    val envelope = when (type) {
                        "pluck" -> {
                            // Quick pluck decay
                            Math.exp(-6.0 * progress)
                        }
                        "noise" -> {
                            // Explosion decay
                            1.0 - progress
                        }
                        else -> {
                            // Standard fade out
                            1.0 - progress
                        }
                    }
                    
                    val value = when (type) {
                        "noise" -> {
                            // Random sample + bandpass/lowpass filter behavior
                            val rawNoise = random.nextDouble() * 2.0 - 1.0
                            // Low-frequency filter by mixing previous samples
                            rawNoise * envelope
                        }
                        "triangle" -> {
                            val angle = t * freq * 2.0 * PI
                            val tri = if ((angle % (2.0 * PI)) < PI) {
                                (angle % PI) / PI * 2.0 - 1.0
                            } else {
                                1.0 - ((angle % PI) / PI * 2.0)
                            }
                            tri * envelope
                        }
                        else -> { // sine
                            val angle = t * freq * 2.0 * PI
                            sin(angle) * envelope
                        }
                    }
                    
                    buffer[i] = (value * Short.MAX_VALUE * volumeFactor).toInt().coerceIn(-Short.MAX_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }

                val audioTrack = AudioTrack.Builder()
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
                    .setBufferSizeInBytes(numSamples * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(buffer, 0, buffer.size)
                audioTrack.play()
                delay(durationMs + 100L)
                audioTrack.stop()
                audioTrack.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun playFlap() {
        playTone(320.0, 650.0, 100, "triangle")
        vibrate(40)
    }

    fun playScore() {
        scope.launch {
            // Gamelan Saron pentatonic two-tone chime
            playTone(540.0, 540.0, 120, "pluck") // High Barang note
            delay(80)
            playTone(680.0, 680.0, 220, "pluck") // High Nem note
        }
    }

    fun playCrash() {
        playTone(180.0, 40.0, 400, "noise")
        vibrate(200)
    }

    fun playWin() {
        scope.launch {
            // Rapid ascending major gamelan scale
            val slendroWin = doubleArrayOf(270.0, 300.0, 340.0, 400.0, 450.0, 540.0, 600.0, 680.0)
            for (freq in slendroWin) {
                playTone(freq, freq, 250, "pluck")
                delay(100)
            }
        }
    }

    fun playFail() {
        playTone(220.0, 70.0, 600, "sine")
        vibrate(300)
    }

    // Gamelan Slendro (pentatonic scale used in Javanese Wayang plays)
    fun startMusic() {
        if (isMuted) return
        if (musicJob?.isActive == true) return
        
        musicJob = scope.launch {
            // Frequencies for Slendro scale: Barang, Gulu, Dhada, Lima, Nem
            val scale = doubleArrayOf(270.0, 300.0, 340.0, 400.0, 450.0, 540.0)
            
            // Loop pattern (Saron metalphone accompaniment style)
            val melody = intArrayOf(
                0, 1, 2, 4, 3, 2, 1, 0,
                2, 3, 4, 5, 4, 3, 2, 1,
                0, 2, 1, 3, 2, 4, 3, 5,
                4, 2, 3, 1, 2, 0, 1, 0
            )
            
            var index = 0
            while (isActive) {
                if (isMuted) {
                    delay(1000)
                    continue
                }
                
                val noteIdx = melody[index % melody.size]
                val freq = scale[noteIdx]
                
                // High-pitched bells with gentle decay
                playTone(freq, freq, 450, "pluck")
                
                // Steady gamelan tempo: 450ms per beat
                delay(450)
                index++
            }
        }
    }

    fun stopMusic() {
        musicJob?.cancel()
        musicJob = null
    }

    @Suppress("DEPRECATION")
    private fun vibrate(ms: Long) {
        if (!isVibrateEnabled) return
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    vibrator.vibrate(ms)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun release() {
        stopMusic()
        scope.cancel()
    }
}
