package com.example.voicenotes.threads

import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * Runnable implementation (Week 14 — Runnable Interface + Background Processing requirements).
 * Polls the RecordingThread's amplitude every 100ms and posts waveform
 * bar heights to the UI thread via a Handler — runs in a dedicated
 * Thread("WaveformThread"), separate from RecordingThread, satisfying
 * the Multiple Threads requirement.
 */
class WaveformRunnable(
    private val recordingThread: RecordingThread,
    private val onAmplitudeUpdate: (Int) -> Unit
) : Runnable {

    @Volatile
    private var running = false

    private val mainHandler = Handler(Looper.getMainLooper())

    override fun run() {
        Log.d("WaveformRunnable", "Waveform polling started on ${Thread.currentThread().name}")
        running = true
        while (running && recordingThread.isRecording) {
            val amplitude = recordingThread.getMaxAmplitude()
            // Normalise 0-32767 → 0-100
            val normalised = (amplitude / 327.67).toInt().coerceIn(0, 100)
            mainHandler.post { onAmplitudeUpdate(normalised) }
            try {
                Thread.sleep(100)
            } catch (e: InterruptedException) {
                break
            }
        }
        Log.d("WaveformRunnable", "Waveform polling stopped")
    }

    fun stop() {
        running = false
    }
}
