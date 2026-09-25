package com.kinassist.app.core.webrtc

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.kinassist.app.MainActivity
import com.kinassist.app.R

class ScreenCaptureService : Service() {

    companion object {
        const val ACTION_START = "com.kinassist.app.START_SCREEN_CAPTURE"
        const val ACTION_STOP = "com.kinassist.app.STOP_SCREEN_CAPTURE"
        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "kinassist_screen_share"

        var mediaProjection: MediaProjection? = null
            private set
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
                val resultData = intent.getParcelableExtra<Intent>(EXTRA_RESULT_DATA)

                if (resultCode == Activity.RESULT_OK && resultData != null) {
                    startForeground(NOTIFICATION_ID, buildNotification())
                    val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                    mediaProjection = projectionManager.getMediaProjection(resultCode, resultData)

                    // Connect to WebRTC Video and Audio pipeline
                    val webrtcManager = WebRtcManager.getInstance(applicationContext)
                    webrtcManager.startScreenCapture(resultData)
                    webrtcManager.startAudio()
                } else {
                    stopSelf()
                }
            }
            ACTION_STOP -> {
                mediaProjection?.stop()
                mediaProjection = null
                WebRtcManager.getInstance(applicationContext).close()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Screen Share Active",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifies you when your screen is being shared with family"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("KinAssist: Screen Sharing Active")
            .setContentText("Rahul is helping you right now. Tap to return.")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        mediaProjection?.stop()
        mediaProjection = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
