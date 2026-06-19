package com.example.voicenotes.models

data class VoiceNote(
    val id: Long = 0,
    var title: String,
    val filePath: String,
    val duration: Int,
    val timestamp: Long = System.currentTimeMillis()
)
