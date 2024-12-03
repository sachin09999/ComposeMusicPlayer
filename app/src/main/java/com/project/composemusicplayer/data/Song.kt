package com.project.composemusicplayer.data

import android.net.Uri

data class Song (
    val id: Long,
    val title: String,
    val artist: String,
    val albumArt: Uri?,
    val duration: Long = 0,
    val filePath: String
)