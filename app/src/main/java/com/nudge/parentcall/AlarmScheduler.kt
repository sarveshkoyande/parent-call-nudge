package com.nudge.parentcall

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.nudge.parentcall.Prefs.loadAlarms
import java.util.Calendar

/** Schedules / cancels voice alarms with the system AlarmManager. */
object AlarmScheduler {

    fun scheduleAll(ctx: Context) {
        ctx.loadAlarms().filter { it.enabled }.forEach { schedule(ctx, it) }
    }

    fun schedule(ctx: Context, alarm: Alarm) {
        val trigger = nextTrigger(alarm) ?: return
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val fire = firePendingIntent(ctx, alarm.id)
        val show = showPendingIntent(ctx)
        am.setAlarmClock(AlarmManager.AlarmClockInfo(trigger, show), fire)
    }

    fun cancel(ctx: Context, id: Int) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(firePendingIntent(ctx, id))
    }

    /** Next epoch-millis this alarm should fire, or null if it can't. */
    fun nextTrigger(alarm: Alarm): Long? {
        val now = Calendar.getInstance()
        for (addDays in 0..7) {
            val c = Calendar.getInstance()
            c.add(Calendar.DAY_OF_YEAR, addDays)
            c.set(Calendar.HOUR_OF_DAY, alarm.hour)
            c.set(Calendar.MINUTE, alarm.minute)
            c.set(Calendar.SECOND, 0)
            c.set(Calendar.MILLISECOND, 0)

            val dowMatches = alarm.days.isEmpty() || alarm.days.contains(c.get(Calendar.DAY_OF_WEEK))
            if (dowMatches && c.timeInMillis > now.timeInMillis) return c.timeInMillis
        }
        return null
    }

    private fun firePendingIntent(ctx: Context, id: Int): PendingIntent {
        val i = Intent(ctx, AlarmReceiver::class.java).apply {
            action = "com.nudge.parentcall.FIRE"
            putExtra("id", id)
        }
        return PendingIntent.getBroadcast(
            ctx, id, i,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun showPendingIntent(ctx: Context): PendingIntent {
        val i = Intent(ctx, AlarmActivity::class.java)
        return PendingIntent.getActivity(
            ctx, 0, i,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}
