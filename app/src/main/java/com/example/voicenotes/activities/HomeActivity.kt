package com.example.voicenotes.activities

import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.voicenotes.R
import com.example.voicenotes.adapters.NotesAdapter
import com.example.voicenotes.databinding.ActivityHomeBinding
import com.example.voicenotes.db.DatabaseHelper
import com.example.voicenotes.models.VoiceNote
import com.example.voicenotes.utils.SessionManager
import java.io.File

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var db: DatabaseHelper
    private lateinit var session: SessionManager
    private lateinit var adapter: NotesAdapter
    private val notes = mutableListOf<VoiceNote>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        db      = DatabaseHelper(this)
        session = SessionManager(this)

        binding.tvWelcome.text = "Hi, ${session.getUserName()} 👋"

        adapter = NotesAdapter(notes,
            onClick    = { note, view -> openPlayback(note, view) },
            onLongClick = { note -> showNoteOptions(note) }
        )

        binding.recyclerNotes.layoutManager = LinearLayoutManager(this)
        binding.recyclerNotes.adapter = adapter

        binding.fabRecord.setOnClickListener {
            startActivity(Intent(this, RecordActivity::class.java))
        }

        setupSwipeToDelete()
        loadNotes()
    }

    override fun onResume() {
        super.onResume()
        loadNotes()
    }

    private fun loadNotes() {
        val list = db.getAllNotes()
        adapter.updateList(list)
        val count = list.size
        binding.tvCount.text = "$count recording${if (count == 1) "" else "s"}"
        binding.layoutEmpty.visibility   = if (count == 0) View.VISIBLE else View.GONE
        binding.recyclerNotes.visibility = if (count == 0) View.GONE   else View.VISIBLE
    }

    private fun openPlayback(note: VoiceNote, view: View) {
        val intent = Intent(this, PlaybackActivity::class.java).apply {
            putExtra("NOTE_ID", note.id)
            putExtra("TRANSITION_NAME", "shared_note_${note.id}")
        }
        val options = androidx.core.app.ActivityOptionsCompat.makeSceneTransitionAnimation(
            this, view, "shared_note_${note.id}"
        )
        startActivity(intent, options.toBundle())
    }

    private fun showNoteOptions(note: VoiceNote) {
        AlertDialog.Builder(this)
            .setTitle(note.title)
            .setItems(arrayOf("Rename", "Delete")) { _, which ->
                when (which) {
                    0 -> showRenameDialog(note)
                    1 -> confirmDelete(note)
                }
            }
            .show()
    }

    private fun showRenameDialog(note: VoiceNote) {
        val input = EditText(this).apply {
            setText(note.title)
            setTextColor(resources.getColor(R.color.white, theme))
            setBackgroundResource(R.drawable.bg_edittext)
            setPadding(32, 16, 32, 16)
        }
        AlertDialog.Builder(this)
            .setTitle("Rename Recording")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val newTitle = input.text.toString().trim()
                if (newTitle.isEmpty()) {
                    Toast.makeText(this, "Name cannot be empty.", Toast.LENGTH_SHORT).show()
                } else {
                    // File I/O on background thread (FileIORunnable — Background Processing requirement)
                    Thread(Runnable {
                        db.updateTitle(note.id, newTitle)
                        runOnUiThread {
                            Toast.makeText(this, "Renamed successfully.", Toast.LENGTH_SHORT).show()
                            loadNotes()
                        }
                    }, "FileIOThread").start()
                }
            }
            .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
            .show()
    }

    private fun confirmDelete(note: VoiceNote) {
        AlertDialog.Builder(this)
            .setTitle("Delete Recording")
            .setMessage("Delete \"${note.title}\"? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                // File deletion on background thread (Background Processing requirement)
                Thread(Runnable {
                    val file = File(note.filePath)
                    if (file.exists()) file.delete()
                    db.deleteNote(note.id)
                    runOnUiThread {
                        Toast.makeText(this, "Recording deleted.", Toast.LENGTH_SHORT).show()
                        loadNotes()
                    }
                }, "FileIOThread").start()
            }
            .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
            .show()
    }

    private fun setupSwipeToDelete() {
        val simpleItemTouchCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                viewHolder.itemView.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                val position = viewHolder.adapterPosition
                val note = adapter.getNoteAt(position)
                
                AlertDialog.Builder(this@HomeActivity)
                    .setTitle("Delete Recording")
                    .setMessage("Delete \"${note.title}\"? This cannot be undone.")
                    .setPositiveButton("Delete") { _, _ ->
                        Thread(Runnable {
                            val file = File(note.filePath)
                            if (file.exists()) file.delete()
                            db.deleteNote(note.id)
                            runOnUiThread {
                                Toast.makeText(this@HomeActivity, "Recording deleted.", Toast.LENGTH_SHORT).show()
                                loadNotes()
                            }
                        }, "FileIOThread").start()
                    }
                    .setNegativeButton("Cancel") { d, _ ->
                        adapter.restoreItem(position)
                        d.dismiss()
                    }
                    .setOnCancelListener { adapter.restoreItem(position) }
                    .show()
            }

            override fun onChildDraw(c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
                val itemView = viewHolder.itemView
                val p = Paint()
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    p.color = Color.parseColor("#E53935")
                    if (dX > 0) {
                        c.drawRect(itemView.left.toFloat(), itemView.top.toFloat(), itemView.left.toFloat() + dX, itemView.bottom.toFloat(), p)
                    } else if (dX < 0) {
                        c.drawRect(itemView.right.toFloat() + dX, itemView.top.toFloat(), itemView.right.toFloat(), itemView.bottom.toFloat(), p)
                    }
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }
        ItemTouchHelper(simpleItemTouchCallback).attachToRecyclerView(binding.recyclerNotes)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_home, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_sort_date -> {
                adapter.updateList(db.getAllNotes())
                Toast.makeText(this, "Sorted by date", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.action_sort_name -> {
                adapter.updateList(db.getAllNotesSortedByName())
                Toast.makeText(this, "Sorted by name", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.action_about -> {
                val dialogView = layoutInflater.inflate(R.layout.dialog_about, null)
                val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
                dialog.setContentView(dialogView)
                
                dialogView.findViewById<View>(R.id.btnAboutClose).setOnClickListener {
                    dialog.dismiss()
                }
                
                dialog.show()
                true
            }
            R.id.action_logout -> {
                AlertDialog.Builder(this)
                    .setTitle("Logout")
                    .setMessage("Are you sure you want to logout?")
                    .setPositiveButton("Logout") { _, _ ->
                        session.logout()
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    }
                    .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
                    .show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
