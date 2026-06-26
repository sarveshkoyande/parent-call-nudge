package com.nudge.parentcall

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nudge.parentcall.Prefs.armed
import com.nudge.parentcall.Prefs.isAfterHours
import com.nudge.parentcall.Prefs.isDoneToday
import com.nudge.parentcall.Prefs.rolloverIfNewDay

/** Fires every time the phone is unlocked. Decides whether to throw up the Nudge. */
class UnlockReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_USER_PRESENT) return

        with(context) {
            rolloverIfNewDay()
            if (!armed) return
            if (!isAfterHours()) return     // still during/ before office hours
            if (isDoneToday()) return        // already called everyone today

            // No prompt — just dial. Only escape is the phone's end-call button.
            CallManager.dialNext(this)
        }
    }
}
