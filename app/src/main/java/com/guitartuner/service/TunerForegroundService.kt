package com.guitartuner.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.guitartuner.MainActivity
import com.guitartuner.R

/**
 * Keeps the microphone alive when the screen is locked.
 *
 * Android 10+ restricts background microphone access to apps with a running
 * foreground service that carries the FOREGROUND_SERVICE_TYPE_MICROPHONE flag.
 * This service provides that cover, showing a persistent (non-dismissible)
 * notification while the tuner is active so the system never kills the mic.
 *
 * The actual AudioRecord lives in AudioProcessor / TunerViewModel; this service
 * merely holds the OS-level permission token.
 *
 * Usage:
 *   start: startForegroundService(Intent(ctx, TunerForegroundService::class.java))
 *   stop:  stopService(Intent(ctx, TunerForegroundService::class.java))
 */
class TunerForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "tuner_mic_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP = "com.guitartuner.STOP_TUNER"

        fun startIntent(context: Context) = Intent(context, TunerForegroundService::class.java)
        fun stopIntent(context: Context) = Intent(context, TunerForegroundService::class.java).apply {
            action = ACTION_STOP
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, buildNotification())
        return START_STICKY
    }

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this, 1,
            stopIntent(this),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Guitar Tuner")
            .setContentText("Listening…")
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(openIntent)
            .addAction(0, "Stop", stopIntent)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Tuner microphone",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Active while the tuner is listening"
            setShowBadge(false)
        }
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)
    }
}
