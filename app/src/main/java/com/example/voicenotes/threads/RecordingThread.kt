package com.example.voicenotes.threads

import android.media.MediaRecorder
import android.os.Build
import android.util.Log

import android.content.Context

/**
 * Thread subclass (Week 14 — Thread Class requirement).
 * Wraps MediaRecorder start/stop off the main UI thread to avoid
 * blocking the main thread during audio I/O operations.
 */
class RecordingThread(
    private val context: Context,
    private val outputPath: String,
    private val onStarted: () -> Unit,
    private val onError: (String) -> Unit
) : Thread("RecordingThread") {

    private var recorder: MediaRecorder? = null

    @Volatile
    var isRecording = false
        private set

    override fun run() {
        try {
            recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder!!.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(outputPath)
                prepare()
                start()
            }

            isRecording = true
            Log.d("RecordingThread", "Recording started: $outputPath")
            onStarted()

        } catch (e: Exception) {
            Log.e("RecordingThread", "Error starting recorder: ${e.message}")
            onError(e.message ?: "Unknown recording error")
        }
    }

    fun stopRecording() {
        try {
            recorder?.apply {
                stop()
                release()
            }
            recorder = null
            isRecording = false
            Log.d("RecordingThread", "Recording stopped")
        } catch (e: Exception) {
            Log.e("RecordingThread", "Error stopping recorder: ${e.message}")
        }
    }

    fun getMaxAmplitude(): Int = try {
        recorder?.maxAmplitude ?: 0
    } catch (e: Exception) {
        0
    }
}
