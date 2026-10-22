package com.example.player

import android.app.PendingIntent
import android.content.Intent
import kotlin.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.MainActivity

/**
 * خدمة التشغيل في الخلفية (NovaPlaybackService) المعتمدة على Jetpack Media3 MediaSessionService.
 * تضمن:
 * 1. استمرار تشغيل مقاطع الفيديو أو الصوت عند قفل الشاشة أو مغادرة التطبيق.
 * 2. إظهار إشعار تحكم بالوسائط (MediaStyle Notification) تفاعلي في لوحة الإشعارات وشاشة القفل.
 * 3. التعامل التلقائي مع التركيز الصوتي (Audio Focus) وإيقاف التشغيل عند فصل سماعات الرأس (Becoming Noisy).
 */
@OptIn(UnstableApi::class)
class NovaPlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    companion object {
        @Volatile
        var sharedPlayer: ExoPlayer? = null
            private set
    }

    override fun onCreate() {
        super.onCreate()

        // تهيئة المشغل المركزي للخدمة الخلفية
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true // التعامل التلقائي مع Audio Focus
            )
            .setHandleAudioBecomingNoisy(true) // الإيقاف التلقائي عند نزع السماعات
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()

        sharedPlayer = player

        // نية العودة للتطبيق عند النقر على إشعار التحكم
        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivityPendingIntent)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        sharedPlayer = null
        super.onDestroy()
    }
}
