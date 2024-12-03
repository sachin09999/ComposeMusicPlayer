package com.project.composemusicplayer.data

sealed class PlayerEvent {
    object Play : PlayerEvent()
    object Pause : PlayerEvent()
    object Next : PlayerEvent()
    object Previous : PlayerEvent()
    object Stop : PlayerEvent()
    data class UpdateProgress(val progress: Float) : PlayerEvent()
    data class SeekTo(val position: Long) : PlayerEvent()
    data class SelectSong(val song: Song) : PlayerEvent()
}