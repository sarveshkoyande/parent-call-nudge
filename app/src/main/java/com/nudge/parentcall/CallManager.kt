package com.nudge.parentcall

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.nudge.parentcall.Prefs.completedDay
import com.nudge.parentcall.Prefs.grandparentNumber
import com.nudge.parentcall.Prefs.nextTarget
import com.nudge.parentcall.Prefs.parentNumber

/**
 * Places the outgoing call directly — no prompt, no countdown, no skip button.
 * The user's only way out is the phone's own "end call" button.
 * Advances the chain: parent -> grand -> done-for-today.
 */
object CallManager {

    fun dialNext(ctx: Context) {
        val target = ctx.nextTarget
        val number = when (target) {
            "parent" -> ctx.parentNumber
            "grand"  -> ctx.grandparentNumber
            else     -> return
        }
        if (number.isBlank()) {
            // No grandparent set? then parent alone finishes the day.
            if (target == "grand") ctx.completedDay = Prefs.todayStamp()
            return
        }

        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.CALL_PHONE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(ctx, "Open the app once to grant Phone permission.",
                Toast.LENGTH_LONG).show()
            return
        }

        val call = Intent(Intent.ACTION_CALL, Uri.parse("tel:$number"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(call)

        // Advance the chain. We count "dialed" as done for this person.
        when (target) {
            "parent" -> ctx.nextTarget = "grand"
            "grand"  -> {
                ctx.nextTarget = "done"
                ctx.completedDay = Prefs.todayStamp()
            }
        }
    }
}
