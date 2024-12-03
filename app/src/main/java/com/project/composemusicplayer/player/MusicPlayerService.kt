package com.project.composemusicplayer.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.project.composemusicplayer.MainActivity
import com.project.composemusicplayer.R
import com.project.composemusicplayer.data.Song
import java.io.IOException
import kotlin.math.max

class MusicPlayerService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private lateinit var audioManager: AudioManager
    private var currentSong: Song? = null
    private val musicBinder = MusicBinder()
    private var progressUpdateListener: ((Float, Long) -> Unit)? = null
    private val handler = Handler(Looper.getMainLooper())
    private val progressUpdateRunnable = object : Runnable {
        override fun run() {
            updateProgress()
            handler.postDelayed(this, 1000)
        }
    }

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "MusicPlayerChannel"
        private const val NOTIFICATION_ID = 1
    }

    inner class MusicBinder : Binder() {
        fun getService(): MusicPlayerService = this@MusicPlayerService
    }

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        setupMediaPlayer()
        createNotificationChannel()
    }

    private fun setupMediaPlayer() {
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            setOnCompletionListener {
                stopProgressUpdates()
            }
        }
    }

    fun playSong(song: Song) {
        try {
            currentSong = song
            mediaPlayer?.apply {
                reset()
                setDataSource(song.filePath)
                prepare()
                start()
                startProgressUpdates()
                startForeground(NOTIFICATION_ID, createNotification(song))
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun pausePlayback() {
        mediaPlayer?.pause()
        stopProgressUpdates()
        updateNotification()
    }

    fun resumePlayback() {
        mediaPlayer?.start()
        startProgressUpdates()
        updateNotification()
    }

    fun seekTo(position: Int) {
        mediaPlayer?.seekTo(position)
    }

    fun setOnProgressUpdateListener(listener: (Float, Long) -> Unit) {
        progressUpdateListener = listener
    }

    private fun startProgressUpdates() {
        handler.removeCallbacks(progressUpdateRunnable)
        handler.post(progressUpdateRunnable)
    }

    private fun stopProgressUpdates() {
        handler.removeCallbacks(progressUpdateRunnable)
    }

//    private fun updateProgress() {
//        mediaPlayer?.let { player ->
//            if (player.isPlaying) {
//                val duration = player.duration.toLong()
//                val position = player.currentPosition.toLong()
//                val progress = if (duration > 0) position.toFloat() / duration else 0f
//                progressUpdateListener?.invoke(progress, position)
//            }
//        }
//    }
    fun seekTo(position: Long) {
        mediaPlayer?.let {
            val seekPosition = (position.toFloat() / it.duration * it.duration).toInt()
            it.seekTo(seekPosition)
        }
    }

    private fun updateProgress() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                val progress = it.currentPosition.toFloat() / it.duration
                progressUpdateListener?.invoke(progress, it.currentPosition.toLong())
            }
        }
    }

    fun setProgressUpdateListener(listener: (Float, Long) -> Unit) {
        progressUpdateListener = listener
    }


    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Music Player",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Music Player Controls"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(song: Song): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(song.title)
            .setContentText(song.artist)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification() {
        currentSong?.let { song ->
            val notification = createNotification(song)
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.notify(NOTIFICATION_ID, notification)
        }
    }

    override fun onBind(intent: Intent): IBinder {
        return musicBinder
    }

    override fun onDestroy() {
        stopProgressUpdates()
        mediaPlayer?.release()
        mediaPlayer = null
        super.onDestroy()
    }
}