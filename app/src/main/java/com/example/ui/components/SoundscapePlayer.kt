package com.example.ui.components

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.*
import kotlin.math.sin

enum class SoundscapeMode(val id: String, val title: String, val icon: String, val description: String) {
    NONE("none", "Off", "\uD83D\uDD07", "Ambient audio muted"),
    LOFI("lofi", "Lo-Fi Chill", "\uD83C\uDFB5", "Warm gentle 7th chords & vinyl cadence"),
    RAIN("rain", "Cafe Rain", "\uD83C\uDF27\uFE0F", "Soothing low-pass atmospheric rain"),
    SYNTHWAVE("synthwave", "Synthwave", "\u2728", "Dreamy retro neon bassline & pads")
}

class SoundscapePlayer {
    private var audioTrack: AudioTrack? = null
    private var playbackJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    var activeMode: SoundscapeMode = SoundscapeMode.NONE
        private set

    fun setMode(mode: SoundscapeMode) {
        stop()
        if (mode == SoundscapeMode.NONE) {
            activeMode = SoundscapeMode.NONE
            return
        }
        activeMode = mode
        startSoundscape(mode)
    }

    private fun startSoundscape(mode: SoundscapeMode) {
        playbackJob = scope.launch {
            try {
                val sampleRate = 44100
                val bufferSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                ).coerceAtLeast(sampleRate)

                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
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
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                audioTrack = track
                track.play()

                val buffer = ShortArray(1024)
                var phase1 = 0.0
                var phase2 = 0.0
                var phase3 = 0.0

                while (isActive) {
                    for (i in buffer.indices) {
                        val sample: Double = when (mode) {
                            SoundscapeMode.LOFI -> {
                                // Mellow Cmaj7 chord (130.81Hz, 164.81Hz, 196Hz, 246.94Hz)
                                phase1 += 2.0 * Math.PI * 130.81 / sampleRate
                                phase2 += 2.0 * Math.PI * 164.81 / sampleRate
                                phase3 += 2.0 * Math.PI * 196.00 / sampleRate
                                val s = (sin(phase1) * 0.4 + sin(phase2) * 0.35 + sin(phase3) * 0.25)
                                s * 0.12
                            }
                            SoundscapeMode.RAIN -> {
                                // Gentle filtered white noise simulating cafe rain
                                val noise = (Math.random() * 2.0 - 1.0) * 0.08
                                noise
                            }
                            SoundscapeMode.SYNTHWAVE -> {
                                // Deep retro bassline (55Hz and 110Hz harmonics)
                                phase1 += 2.0 * Math.PI * 55.0 / sampleRate
                                phase2 += 2.0 * Math.PI * 110.0 / sampleRate
                                val s = (sin(phase1) * 0.6 + sin(phase2) * 0.4)
                                s * 0.15
                            }
                            SoundscapeMode.NONE -> 0.0
                        }
                        buffer[i] = (sample * Short.MAX_VALUE).toInt().toShort()
                    }
                    track.write(buffer, 0, buffer.size)
                }
            } catch (e: Exception) {
                Log.w("SoundscapePlayer", "Playback interrupted: ${e.message}")
            }
        }
    }

    fun stop() {
        playbackJob?.cancel()
        playbackJob = null
        try {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null
        activeMode = SoundscapeMode.NONE
    }
}
