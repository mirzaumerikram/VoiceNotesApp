package com.example.voicenotes.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.voicenotes.databinding.ItemVoiceNoteBinding
import com.example.voicenotes.models.VoiceNote
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotesAdapter(
    private val notes: MutableList<VoiceNote>,
    private val onClick: (VoiceNote, android.view.View) -> Unit,
    private val onLongClick: (VoiceNote) -> Unit
) : RecyclerView.Adapter<NotesAdapter.NoteViewHolder>() {

    inner class NoteViewHolder(val binding: ItemVoiceNoteBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val binding = ItemVoiceNoteBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return NoteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = notes[position]
        with(holder.binding) {
            tvNoteTitle.text = note.title
            tvDuration.text  = formatDuration(note.duration)
            tvDate.text      = formatDate(note.timestamp)

            root.transitionName = "shared_note_${note.id}"
            root.setOnClickListener     { onClick(note, root) }
            root.setOnLongClickListener { onLongClick(note); true }
        }
    }

    override fun getItemCount() = notes.size

    fun updateList(newList: List<VoiceNote>) {
        notes.clear()
        notes.addAll(newList)
        notifyDataSetChanged()
    }

    fun getNoteAt(position: Int): VoiceNote = notes[position]

    fun restoreItem(position: Int) {
        notifyItemChanged(position)
    }

    private fun formatDuration(seconds: Int): String {
        val m = seconds / 60
        val s = seconds % 60
        return "%d:%02d".format(m, s)
    }

    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
