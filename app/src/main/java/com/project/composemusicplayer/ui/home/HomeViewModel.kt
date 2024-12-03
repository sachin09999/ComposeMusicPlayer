package com.project.composemusicplayer.ui.home

import android.app.Application
import android.content.ComponentName
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.IBinder
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.composemusicplayer.data.PlaybackState
import com.project.composemusicplayer.data.PlayerEvent
import com.project.composemusicplayer.data.Song
import com.project.composemusicplayer.player.MusicPlayerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {


    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()
    private var musicService: MusicPlayerService? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicPlayerService.MusicBinder
            musicService = binder.getService()
            // Add this here
            musicService?.setProgressUpdateListener { progress, position ->
                _playbackState.value = _playbackState.value.copy(
                    progress = progress
                )
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            musicService = null
        }
    }

    init {
        loadSongs()
        bindMusicService()
    }

    private fun bindMusicService() {
        Intent(getApplication(), MusicPlayerService::class.java).also { intent ->
            getApplication<Application>().bindService(
                intent,
                serviceConnection,
                Context.BIND_AUTO_CREATE
            )
        }
    }



    private fun loadSongs() {
        viewModelScope.launch {
            val songList = mutableListOf<Song>()
            val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.DATA
            )

            getApplication<Application>().contentResolver.query(
                collection,
                projection,
                null,
                null,
                "${MediaStore.Audio.Media.TITLE} ASC"
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val filePathColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val title = cursor.getString(titleColumn)
                    val artist = cursor.getString(artistColumn)
                    val duration = cursor.getLong(durationColumn)
                    val albumId = cursor.getLong(albumIdColumn)
                    val filePath = cursor.getString(filePathColumn)

                    val albumArtUri = ContentUris.withAppendedId(
                        Uri.parse("content://media/external/audio/albumart"),
                        albumId
                    )

                    val song = Song(
                        id = id,
                        title = title,
                        artist = artist,
                        duration = duration,
                        albumArt = albumArtUri,
                        filePath = filePath
                    )
                    songList.add(song)
                }
            }
            _songs.value = songList
        }
    }

    fun onPlayerEvent(event: PlayerEvent) {
        when (event) {
            is PlayerEvent.Play -> {
                playbackState.value.currentSong?.let { song ->
                    musicService?.resumePlayback()
                }
                _playbackState.value = _playbackState.value.copy(isPlaying = true)
            }
            is PlayerEvent.Pause -> {
                musicService?.pausePlayback()
                _playbackState.value = _playbackState.value.copy(isPlaying = false)
            }
            is PlayerEvent.Stop -> {
                musicService?.pausePlayback()
                _playbackState.value = _playbackState.value.copy(
                    isPlaying = false,
                    progress = 0f,
                    currentPosition = 0L
                )
            }
            is PlayerEvent.SelectSong -> {
                musicService?.playSong(event.song)
                _playbackState.value = _playbackState.value.copy(
                    currentSong = event.song,
                    isPlaying = true,
                    progress = 0f,
                    currentPosition = 0L,
                    currentSongDuration = event.song.duration
                )
            }
            is PlayerEvent.SeekTo -> {
                musicService?.seekTo(event.position)
            }
            else -> handleOtherEvents(event)
        }
    }


    private fun handleOtherEvents(event: PlayerEvent) {
        when (event) {
            is PlayerEvent.Next -> handleNextSong()
            is PlayerEvent.Previous -> handlePreviousSong()
            is PlayerEvent.UpdateProgress -> {
                _playbackState.value = _playbackState.value.copy(progress = event.progress)
            }
            else -> {} // Handle other events if needed
        }
    }

    private fun handleNextSong() {
        val currentSong = _playbackState.value.currentSong
        val currentList = _songs.value
        if (currentSong != null && currentList.isNotEmpty()) {
            val currentIndex = currentList.indexOfFirst { it.id == currentSong.id }
            if (currentIndex != -1 && currentIndex < currentList.size - 1) {
                val nextSong = currentList[currentIndex + 1]
                onPlayerEvent(PlayerEvent.SelectSong(nextSong))
            }
        }
    }

    private fun handlePreviousSong() {
        val currentSong = _playbackState.value.currentSong
        val currentList = _songs.value
        if (currentSong != null && currentList.isNotEmpty()) {
            val currentIndex = currentList.indexOfFirst { it.id == currentSong.id }
            if (currentIndex > 0) {
                val previousSong = currentList[currentIndex - 1]
                onPlayerEvent(PlayerEvent.SelectSong(previousSong))
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        getApplication<Application>().unbindService(serviceConnection)
    }
}

