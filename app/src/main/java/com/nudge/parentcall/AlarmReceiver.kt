package com.nudge.parentcall

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.nudge.parentcall.Prefs.loadAlarms
import com.nudge.parentcall.Prefs.saveAlarms

/** Fires when an alarm's time arrives: launches the ringing screen + reschedules. */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra("id", -1)
        if (id < 0) return
        val alarm = context.loadAlarms().firstOrNull { it.id == id } ?: return
        if (!alarm.enabled) return

        showRinging(context, alarm)

        // Recurring -> schedule the next occurrence. One-shot -> disable it.
        if (alarm.days.isNotEmpty()) {
            AlarmScheduler.schedule(context, alarm)
        } else {
            val list = context.loadAlarms().map {
                if (it.id == id) it.copy(enabled = false) else it
            }
            context.saveAlarms(list)
        }
    }

    private fun showRinging(ctx: Context, alarm: Alarm) {
        val channelId = "voice_alarm"
        val nm = ctx.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                channelId, "Voice alarm", NotificationManager.IMPORTANCE_HIGH
            )
            ch.setSound(null, null)   // the ring activity plays the audio, not the notification
            nm.createNotificationChannel(ch)
        }

        val full = Intent(ctx, AlarmRingActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            putExtra("id", alarm.id)
        }
        val fullPi = PendingIntent.getActivity(
            ctx, alarm.id, full,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notif = android.app.Notification.Builder(ctx, channelId)
            .setContentTitle(alarm.label.ifBlank { "Alarm" })
            .setContentText("Tap to open")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setCategory(android.app.Notification.CATEGORY_ALARM)
            .setFullScreenIntent(fullPi, true)
            .setAutoCancel(true)
            .setOngoing(true)
            .build()

        nm.notify(alarm.id, notif)

        // Also try to launch directly (works when we hold overlay permission).
        try { ctx.startActivity(full) } catch (_: Exception) {}
    }
}
