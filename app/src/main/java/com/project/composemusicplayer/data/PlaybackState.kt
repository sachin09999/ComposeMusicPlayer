package com.project.composemusicplayer.data

data class PlaybackState (
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val progress: Float = 0f,
    val playlist: List<Song> = emptyList(),
    val currentSongDuration: Long = 0L,
    val currentPosition: Long = 0L
)