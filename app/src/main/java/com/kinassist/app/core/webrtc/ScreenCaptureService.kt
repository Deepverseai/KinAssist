package com.kinassist.app.core.webrtc

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
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
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            startForeground(NOTIFICATION_ID, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
                        } else {
                            startForeground(NOTIFICATION_ID, buildNotification())
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("ScreenCaptureService", "startForeground error: ${e.message}", e)
                    }

                    // Start WebRTC screen capture pipeline safely
                    try {
                        val webrtcManager = WebRtcManager.getInstance(applicationContext)
                        webrtcManager.startScreenCapture(resultData)
                    } catch (e: Throwable) {
                        android.util.Log.e("ScreenCaptureService", "startScreenCapture error: ${e.message}", e)
                    }

                    // Start Audio only if RECORD_AUDIO permission is granted
                    try {
                        val hasAudioPerm = androidx.core.content.ContextCompat.checkSelfPermission(
                            this,
                            android.Manifest.permission.RECORD_AUDIO
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                        if (hasAudioPerm) {
                            WebRtcManager.getInstance(applicationContext).startAudio()
                        }
                    } catch (e: Throwable) {
                        android.util.Log.e("ScreenCaptureService", "startAudio error: ${e.message}", e)
                    }
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
            .setContentText("Your caregiver is assisting you right now. Tap to return.")
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
