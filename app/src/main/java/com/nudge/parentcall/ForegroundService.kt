package com.nudge.parentcall

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder

/**
 * Tiny always-on service. Its only job is to stay alive so the UnlockReceiver
 * (registered here at runtime) actually fires when the screen is unlocked.
 * ACTION_USER_PRESENT can't be caught from the manifest on Android 8+, so it
 * must be registered from a running process like this one.
 */
class ForegroundService : Service() {

    private val receiver = UnlockReceiver()
    private val channelId = "nudge_running"

    override fun onCreate() {
        super.onCreate()
        startInForeground()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_USER_PRESENT) // phone unlocked
            addAction(Intent.ACTION_SCREEN_ON)
        }
        registerReceiver(receiver, filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY // restart me if the system kills me
    }

    override fun onDestroy() {
        try { unregisterReceiver(receiver) } catch (_: Exception) {}
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startInForeground() {
        val nm = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(channelId, "Nudge active", NotificationManager.IMPORTANCE_MIN)
            )
        }
        val n: Notification = Notification.Builder(this, channelId)
            .setContentTitle("Call nudge is armed")
            .setContentText("Will remind you to call family after work.")
            .setSmallIcon(android.R.drawable.sym_action_call)
            .setOngoing(true)
            .build()
        startForeground(1, n)
    }
}
