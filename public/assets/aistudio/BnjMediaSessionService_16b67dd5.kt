package com.example.player.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.MainActivity
import com.example.R
import com.example.equalizer.BnjAudioDspProcessor

/**
 * AndroidX Media3 Foreground MediaSessionService providing background music/video audio playback,
 * lock screen controls, rich media notifications, and Bluetooth AVRCP headset controls.
 *
 * Owns and shares the single active ExoPlayer instance with BnjPlaybackController so that
 * Media3's internal MediaNotificationManager automatically promotes the service to foreground
 * with a MediaStyle notification when playback begins, avoiding ForegroundServiceDidNotStartInTimeException.
 */
class BnjMediaSessionService : MediaSessionService() {

    var mediaSession: MediaSession? = null
        private set

    companion object {
        const val CHANNEL_ID = "bnj_media_playback_channel"

        @OptIn(UnstableApi::class)
        val sharedDspProcessor: BnjAudioDspProcessor = BnjAudioDspProcessor()

        @Volatile
        private var sharedPlayer: ExoPlayer? = null

        @OptIn(UnstableApi::class)
        @Synchronized
        fun getOrCreatePlayer(context: Context): ExoPlayer {
            val appContext = context.applicationContext
            val existing = sharedPlayer
            if (existing != null) {
                return existing
            }
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build()

            val renderersFactory = object : DefaultRenderersFactory(appContext) {
                override fun buildAudioSink(
                    context: Context,
                    enableFloatOutput: Boolean,
                    enableAudioTrackPlaybackParams: Boolean
                ): AudioSink {
                    return DefaultAudioSink.Builder(context)
                        .setEnableFloatOutput(enableFloatOutput)
                        .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                        .setAudioProcessors(arrayOf(sharedDspProcessor))
                        .build()
                }
            }
                .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
                .setEnableDecoderFallback(true)

            return ExoPlayer.Builder(appContext, renderersFactory)
                .setAudioAttributes(audioAttributes, true)
                .setHandleAudioBecomingNoisy(true)
                .build()
                .also { created ->
                    sharedPlayer = created
                }
        }
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val notificationProvider = DefaultMediaNotificationProvider.Builder(this)
            .setChannelId(CHANNEL_ID)
            .setChannelName(R.string.app_name)
            .build()
        setMediaNotificationProvider(notificationProvider)

        val player = getOrCreatePlayer(applicationContext)

        val sessionActivityIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivityIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "JKL PLAY Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background music playback and media session controls"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val activePlayer = mediaSession?.player
        if (activePlayer == null || !activePlayer.playWhenReady || activePlayer.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
