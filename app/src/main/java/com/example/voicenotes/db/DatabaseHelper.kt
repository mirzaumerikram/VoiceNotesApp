package com.example.voicenotes.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.voicenotes.models.VoiceNote

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    companion object {
        private const val DB_NAME = "voicenotes.db"
        private const val DB_VERSION = 1
        private const val TABLE = "voice_notes"
        private const val COL_ID = "id"
        private const val COL_TITLE = "title"
        private const val COL_FILE_PATH = "file_path"
        private const val COL_DURATION = "duration"
        private const val COL_TIMESTAMP = "timestamp"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE $TABLE (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_TITLE TEXT NOT NULL,
                $COL_FILE_PATH TEXT NOT NULL,
                $COL_DURATION INTEGER DEFAULT 0,
                $COL_TIMESTAMP INTEGER NOT NULL
            )"""
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE")
        onCreate(db)
    }

    fun insertNote(note: VoiceNote): Long {
        val cv = ContentValues().apply {
            put(COL_TITLE, note.title)
            put(COL_FILE_PATH, note.filePath)
            put(COL_DURATION, note.duration)
            put(COL_TIMESTAMP, note.timestamp)
        }
        return writableDatabase.insert(TABLE, null, cv)
    }

    fun getAllNotes(): MutableList<VoiceNote> {
        val list = mutableListOf<VoiceNote>()
        val cursor = readableDatabase.rawQuery(
            "SELECT * FROM $TABLE ORDER BY $COL_TIMESTAMP DESC", null
        )
        cursor.use { while (it.moveToNext()) list.add(it.toNote()) }
        return list
    }

    fun getAllNotesSortedByName(): MutableList<VoiceNote> {
        val list = mutableListOf<VoiceNote>()
        val cursor = readableDatabase.rawQuery(
            "SELECT * FROM $TABLE ORDER BY $COL_TITLE ASC", null
        )
        cursor.use { while (it.moveToNext()) list.add(it.toNote()) }
        return list
    }

    fun updateTitle(id: Long, newTitle: String): Int {
        val cv = ContentValues().apply { put(COL_TITLE, newTitle) }
        return writableDatabase.update(TABLE, cv, "$COL_ID=?", arrayOf(id.toString()))
    }

    fun deleteNote(id: Long): Int =
        writableDatabase.delete(TABLE, "$COL_ID=?", arrayOf(id.toString()))

    fun getNoteById(id: Long): VoiceNote? {
        val cursor = readableDatabase.rawQuery(
            "SELECT * FROM $TABLE WHERE $COL_ID=?", arrayOf(id.toString())
        )
        return cursor.use { if (it.moveToFirst()) it.toNote() else null }
    }

    private fun Cursor.toNote() = VoiceNote(
        id        = getLong(getColumnIndexOrThrow(COL_ID)),
        title     = getString(getColumnIndexOrThrow(COL_TITLE)),
        filePath  = getString(getColumnIndexOrThrow(COL_FILE_PATH)),
        duration  = getInt(getColumnIndexOrThrow(COL_DURATION)),
        timestamp = getLong(getColumnIndexOrThrow(COL_TIMESTAMP))
    )
}
