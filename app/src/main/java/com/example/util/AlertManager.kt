package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.example.data.prefs.MerchantPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.sin

/**
 * Manages POS-style audio chimes and haptic feedback pulses on transaction inflow capture.
 */
object AlertManager {
    private const val TAG = "AlertManager"
    private var soundPool: SoundPool? = null
    private var chimeSoundId: Int = 0
    private var isSoundLoaded: Boolean = false
    private val scope = CoroutineScope(Dispatchers.IO)

    fun init(context: Context) {
        if (soundPool != null) return
        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val pool = SoundPool.Builder()
                .setMaxStreams(2)
                .setAudioAttributes(audioAttributes)
                .build()

            pool.setOnLoadCompleteListener { _, sampleId, status ->
                if (status == 0 && sampleId == chimeSoundId) {
                    isSoundLoaded = true
                    Log.d(TAG, "POS confirmation chime loaded into SoundPool")
                }
            }

            soundPool = pool

            // Create POS confirmation chime WAV in cache if not existing
            scope.launch {
                try {
                    val chimeFile = getOrCreatePosChimeWav(context)
                    if (chimeFile.exists()) {
                        chimeSoundId = pool.load(chimeFile.absolutePath, 1)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed loading chime file: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "SoundPool init exception: ${e.message}")
        }
    }

    /**
     * Plays alert according to user preferences (Haptic double-pulse + Audio chime).
     */
    fun playInflowAlert(context: Context) {
        val prefs = MerchantPreferences.getInstance(context)

        // 1. Haptic double pulse (0, 50, 60, 50)
        if (prefs.isHapticEnabled()) {
            triggerHapticPulse(context)
        }

        // 2. Audio Chime
        if (prefs.isAudioToneEnabled()) {
            playPosChime(context)
        }
    }

    /**
     * Triggers double-pulse vibration pattern: [0, 50ms pulse, 60ms pause, 50ms pulse]
     */
    fun triggerHapticPulse(context: Context) {
        try {
            val pattern = longArrayOf(0, 50, 60, 50)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vibrator = vibratorManager?.defaultVibrator ?: (context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, -1)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Vibration failed: ${e.message}")
        }
    }

    /**
     * Plays the high-frequency POS-style payment confirmation chime.
     */
    fun playPosChime(context: Context) {
        try {
            init(context)
            val pool = soundPool
            if (pool != null && isSoundLoaded && chimeSoundId != 0) {
                pool.play(chimeSoundId, 0.9f, 0.9f, 1, 0, 1.0f)
            } else {
                // Direct synthesize fallback
                scope.launch {
                    synthesizeAndPlayDirect()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Audio chime playback failed: ${e.message}")
        }
    }

    private fun synthesizeAndPlayDirect() {
        try {
            val sampleRate = 44100
            // Dual-tone: 1046 Hz (C6) for 80ms followed by 1318 Hz (E6) for 140ms
            val tone1Samples = (0.08 * sampleRate).toInt()
            val tone2Samples = (0.14 * sampleRate).toInt()
            val totalSamples = tone1Samples + tone2Samples
            val pcmData = ShortArray(totalSamples)

            for (i in 0 until tone1Samples) {
                val envelope = if (i < 200) i / 200.0 else (tone1Samples - i).toDouble() / tone1Samples
                val sample = sin(2.0 * Math.PI * 1046.5 * i / sampleRate) * envelope
                pcmData[i] = (sample * Short.MAX_VALUE * 0.7).toInt().toShort()
            }

            for (i in 0 until tone2Samples) {
                val envelope = if (i < 200) i / 200.0 else (tone2Samples - i).toDouble() / tone2Samples
                val sample = sin(2.0 * Math.PI * 1318.5 * i / sampleRate) * envelope
                pcmData[tone1Samples + i] = (sample * Short.MAX_VALUE * 0.7).toInt().toShort()
            }

            val minBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
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
                .setBufferSizeInBytes(maxOf(minBufferSize, totalSamples * 2))
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(pcmData, 0, totalSamples)
            track.play()
        } catch (e: Exception) {
            Log.w(TAG, "Direct AudioTrack synthesis failed: ${e.message}")
        }
    }

    private fun getOrCreatePosChimeWav(context: Context): File {
        val file = File(context.cacheDir, "piprapay_pos_chime.wav")
        if (file.exists() && file.length() > 0) return file

        val sampleRate = 44100
        val tone1Samples = (0.08 * sampleRate).toInt()
        val tone2Samples = (0.16 * sampleRate).toInt()
        val totalSamples = tone1Samples + tone2Samples
        val pcmData = ShortArray(totalSamples)

        for (i in 0 until tone1Samples) {
            val envelope = if (i < 200) i / 200.0 else (tone1Samples - i).toDouble() / tone1Samples
            val sample = sin(2.0 * Math.PI * 1046.5 * i / sampleRate) * envelope
            pcmData[i] = (sample * Short.MAX_VALUE * 0.8).toInt().toShort()
        }

        for (i in 0 until tone2Samples) {
            val envelope = if (i < 200) i / 200.0 else (tone2Samples - i).toDouble() / tone2Samples
            val sample = sin(2.0 * Math.PI * 1318.5 * i / sampleRate) * envelope
            pcmData[tone1Samples + i] = (sample * Short.MAX_VALUE * 0.8).toInt().toShort()
        }

        val byteStream = ByteArrayOutputStream()
        val numBytes = totalSamples * 2
        // Write WAV header
        byteStream.write("RIFF".toByteArray())
        byteStream.write(intToByteArray(36 + numBytes))
        byteStream.write("WAVE".toByteArray())
        byteStream.write("fmt ".toByteArray())
        byteStream.write(intToByteArray(16)) // Subchunk1Size
        byteStream.write(shortToByteArray(1)) // AudioFormat (PCM)
        byteStream.write(shortToByteArray(1)) // NumChannels (Mono)
        byteStream.write(intToByteArray(sampleRate))
        byteStream.write(intToByteArray(sampleRate * 2)) // ByteRate
        byteStream.write(shortToByteArray(2)) // BlockAlign
        byteStream.write(shortToByteArray(16)) // BitsPerSample
        byteStream.write("data".toByteArray())
        byteStream.write(intToByteArray(numBytes))

        for (s in pcmData) {
            byteStream.write(s.toInt() and 0xFF)
            byteStream.write((s.toInt() shr 8) and 0xFF)
        }

        FileOutputStream(file).use { it.write(byteStream.toByteArray()) }
        return file
    }

    private fun intToByteArray(value: Int): ByteArray = byteArrayOf(
        (value and 0xFF).toByte(),
        ((value shr 8) and 0xFF).toByte(),
        ((value shr 16) and 0xFF).toByte(),
        ((value shr 24) and 0xFF).toByte()
    )

    private fun shortToByteArray(value: Int): ByteArray = byteArrayOf(
        (value and 0xFF).toByte(),
        ((value shr 8) and 0xFF).toByte()
    )
}
