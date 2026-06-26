package com.nudge.parentcall

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nudge.parentcall.Prefs.armed
import com.nudge.parentcall.Prefs.isAfterHours

/** Fires every time the phone is unlocked. Decides whether to place the next call. */
class UnlockReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_USER_PRESENT) return

        with(context) {
            if (!armed) return
            if (!isAfterHours()) return     // still during / before office hours

            // No prompt — just dial the next person. dialNext() handles the
            // new-day reset and stops once everyone's been called today.
            CallManager.dialNext(this)
        }
    }
}
