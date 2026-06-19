package com.example.voicenotes.activities

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.voicenotes.R
import com.example.voicenotes.databinding.ActivityRecordBinding
import com.example.voicenotes.db.DatabaseHelper
import com.example.voicenotes.models.VoiceNote
import com.example.voicenotes.threads.RecordingThread
import com.example.voicenotes.threads.WaveformRunnable
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRecordBinding
    private lateinit var db: DatabaseHelper

    private var recordingThread: RecordingThread? = null
    private var waveformRunnable: WaveformRunnable? = null
    private var waveformThread: Thread? = null
    private var isRecording = false

    private val handler = Handler(Looper.getMainLooper())
    private var elapsedSeconds = 0
    private lateinit var timerRunnable: Runnable

    private lateinit var waveformBars: List<View>

    companion object {
        private const val REQ_AUDIO = 200
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = DatabaseHelper(this)

        waveformBars = listOf(
            binding.bar1,  binding.bar2,  binding.bar3,  binding.bar4,
            binding.bar5,  binding.bar6,  binding.bar7,  binding.bar8,
            binding.bar9,  binding.bar10, binding.bar11, binding.bar12
        )

        timerRunnable = object : Runnable {
            override fun run() {
                if (isRecording) {
                    elapsedSeconds++
                    val m = elapsedSeconds / 60
                    val s = elapsedSeconds % 60
                    binding.tvTimer.text = "%d:%02d".format(m, s)
                    handler.postDelayed(this, 1000)
                }
            }
        }

        binding.btnMic.setOnClickListener {
            if (!isRecording) {
                checkPermissionAndRecord()
            } else {
                stopRecording()
            }
        }

        binding.btnBack.setOnClickListener {
            if (isRecording) {
                AlertDialog.Builder(this)
                    .setTitle("Stop Recording?")
                    .setMessage("Recording in progress. Stop and discard?")
                    .setPositiveButton("Discard") { _, _ -> discardAndFinish() }
                    .setNegativeButton("Keep Recording") { d, _ -> d.dismiss() }
                    .show()
            } else {
                finish()
            }
        }
    }

    private fun checkPermissionAndRecord() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), REQ_AUDIO)
        } else {
            startRecording()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_AUDIO) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startRecording()
            } else {
                Toast.makeText(this, "Microphone permission is required to record.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun startRecording() {
        val recDir = File(filesDir, "recordings")
        if (!recDir.exists()) recDir.mkdirs()

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val outputPath = File(recDir, "VN_$timestamp.m4a").absolutePath

        recordingThread = RecordingThread(
            context = this,
            outputPath = outputPath,
            onStarted  = {
                runOnUiThread {
                    isRecording = true
                    elapsedSeconds = 0
                    binding.tvStatus.text = getString(R.string.recording)
                    binding.tvRecordLabel.text = getString(R.string.stop_recording)
                    val pulse = AnimationUtils.loadAnimation(this, R.anim.pulse)
                    binding.btnMic.startAnimation(pulse)
                    handler.postDelayed(timerRunnable, 1000)
                }
            },
            onError = { msg ->
                runOnUiThread {
                    Toast.makeText(this, "Recording error: $msg", Toast.LENGTH_LONG).show()
                }
            }
        )
        recordingThread!!.start()

        // Start waveform polling on a SEPARATE thread (Multiple Threads requirement)
        waveformRunnable = WaveformRunnable(recordingThread!!) { amplitude ->
            updateWaveform(amplitude)
        }
        waveformThread = Thread(waveformRunnable, "WaveformThread")
        waveformThread!!.start()
    }

    private fun stopRecording() {
        isRecording = false
        handler.removeCallbacks(timerRunnable)
        binding.btnMic.clearAnimation()
        binding.tvStatus.text = "Saving recording…"
        binding.tvRecordLabel.text = getString(R.string.start_recording)

        waveformRunnable?.stop()

        val duration = elapsedSeconds
        val path = recordingThread?.let {
            // Stop MediaRecorder off UI thread
            val stopThread = Thread(Runnable {
                it.stopRecording()
            }, "FileIOThread")
            stopThread.start()
            stopThread.join()
            // Return the output path by reading it from the thread field
            null
        }

        // Save to DB
        val recDir = File(filesDir, "recordings")
        val files = recDir.listFiles()?.sortedByDescending { it.lastModified() }
        val savedFile = files?.firstOrNull()

        if (savedFile != null && savedFile.exists() && savedFile.length() > 0) {
            val timestamp = System.currentTimeMillis()
            val sdf = SimpleDateFormat("HH:mm dd MMM", Locale.getDefault())
            val title = "Recording ${sdf.format(Date(timestamp))}"

            val note = VoiceNote(
                title     = title,
                filePath  = savedFile.absolutePath,
                duration  = duration,
                timestamp = timestamp
            )
            db.insertNote(note)
            Toast.makeText(this, "Recording saved!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Recording too short or failed.", Toast.LENGTH_SHORT).show()
        }

        resetWaveform()
        binding.tvTimer.text = "0:00"
        binding.tvStatus.text = getString(R.string.tap_to_record)
        finish()
    }

    private fun discardAndFinish() {
        isRecording = false
        waveformRunnable?.stop()
        recordingThread?.stopRecording()
        handler.removeCallbacks(timerRunnable)
        binding.btnMic.clearAnimation()
        finish()
    }

    private fun updateWaveform(amplitude: Int) {
        val maxHeightDp = 72f
        val minHeightDp = 8f
        val scale = resources.displayMetrics.density

        waveformBars.forEachIndexed { index, bar ->
            val variation = ((Math.random() * 40) - 20).toInt()
            val barAmplitude = (amplitude + variation).coerceIn(0, 100)
            val heightDp = minHeightDp + (maxHeightDp - minHeightDp) * barAmplitude / 100f
            val heightPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, heightDp, resources.displayMetrics).toInt()
            bar.layoutParams = bar.layoutParams.also { it.height = heightPx }
            bar.requestLayout()
        }
    }

    private fun resetWaveform() {
        val minHeightPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 10f, resources.displayMetrics
        ).toInt()
        waveformBars.forEach { bar ->
            bar.layoutParams = bar.layoutParams.also { it.height = minHeightPx }
            bar.requestLayout()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        waveformRunnable?.stop()
        if (isRecording) recordingThread?.stopRecording()
    }
}
