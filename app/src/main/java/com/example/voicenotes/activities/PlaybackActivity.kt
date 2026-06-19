package com.example.voicenotes.activities

import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.voicenotes.databinding.ActivityPlaybackBinding
import com.example.voicenotes.db.DatabaseHelper
import com.example.voicenotes.models.VoiceNote
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PlaybackActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlaybackBinding
    private lateinit var db: DatabaseHelper
    private var note: VoiceNote? = null
    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false
    private val handler = Handler(Looper.getMainLooper())

    private val seekUpdateRunnable = object : Runnable {
        override fun run() {
            mediaPlayer?.let { mp ->
                if (mp.isPlaying) {
                    binding.seekBar.progress = mp.currentPosition
                    binding.tvCurrentTime.text = formatMs(mp.currentPosition)
                    handler.postDelayed(this, 500)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlaybackBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val transitionName = intent.getStringExtra("TRANSITION_NAME")
        if (transitionName != null) {
            binding.mainContainer.transitionName = transitionName
        }

        db = DatabaseHelper(this)

        val noteId = intent.getLongExtra("NOTE_ID", -1L)
        note = db.getNoteById(noteId)

        if (note == null) {
            Toast.makeText(this, "Recording not found.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupUI()
        setupMediaPlayer()
        setupControls()
    }

    private fun setupUI() {
        val n = note!!
        binding.etTitle.setText(n.title)
        binding.tvDate.text = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            .format(Date(n.timestamp))
        binding.tvTotalTime.text = formatMs(n.duration * 1000)
    }

    private fun setupMediaPlayer() {
        val filePath = note!!.filePath
        if (!File(filePath).exists()) {
            Toast.makeText(this, "Audio file not found.", Toast.LENGTH_LONG).show()
            return
        }

        // Pre-load media player on a background thread (Background Processing requirement)
        Thread(Runnable {
            try {
                val mp = MediaPlayer().apply {
                    setDataSource(filePath)
                    prepare()
                }
                runOnUiThread {
                    mediaPlayer = mp
                    binding.seekBar.max = mp.duration
                    binding.tvTotalTime.text = formatMs(mp.duration)

                    mp.setOnCompletionListener {
                        isPlaying = false
                        binding.ivPlayPauseIcon.setImageResource(android.R.drawable.ic_media_play)
                        handler.removeCallbacks(seekUpdateRunnable)
                        binding.seekBar.progress = 0
                        binding.tvCurrentTime.text = "0:00"
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Cannot load audio: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }, "FileIOThread").start()
    }

    private fun setupControls() {
        binding.btnPlayPause.setOnClickListener {
            val mp = mediaPlayer ?: run {
                Toast.makeText(this, "Audio not ready yet.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (isPlaying) {
                mp.pause()
                isPlaying = false
                binding.ivPlayPauseIcon.setImageResource(android.R.drawable.ic_media_play)
                handler.removeCallbacks(seekUpdateRunnable)
            } else {
                mp.start()
                isPlaying = true
                binding.ivPlayPauseIcon.setImageResource(android.R.drawable.ic_media_pause)
                handler.post(seekUpdateRunnable)
            }
        }

        binding.btnStop.setOnClickListener {
            mediaPlayer?.apply {
                if (isPlaying) pause()
                seekTo(0)
            }
            isPlaying = false
            binding.ivPlayPauseIcon.setImageResource(android.R.drawable.ic_media_play)
            handler.removeCallbacks(seekUpdateRunnable)
            binding.seekBar.progress = 0
            binding.tvCurrentTime.text = "0:00"
        }

        binding.btnSaveTitle.setOnClickListener {
            val newTitle = binding.etTitle.text.toString().trim()
            if (newTitle.isEmpty()) {
                Toast.makeText(this, "Title cannot be empty.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            note?.let {
                // Update title on background thread (Background Processing requirement)
                Thread(Runnable {
                    db.updateTitle(it.id, newTitle)
                    runOnUiThread {
                        Toast.makeText(this, "Title saved.", Toast.LENGTH_SHORT).show()
                    }
                }, "FileIOThread").start()
            }
        }

        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaPlayer?.seekTo(progress)
                    binding.tvCurrentTime.text = formatMs(progress)
                }
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })

        binding.btnBack.setOnClickListener { finish() }
    }

    private fun formatMs(ms: Int): String {
        val totalSeconds = ms / 1000
        val m = totalSeconds / 60
        val s = totalSeconds % 60
        return "%d:%02d".format(m, s)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        mediaPlayer?.apply { if (isPlaying) stop(); release() }
        mediaPlayer = null
    }
}
