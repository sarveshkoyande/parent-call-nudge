package com.nudge.parentcall

import android.content.Context
import java.util.Calendar

/** Single place for all saved state. Backed by SharedPreferences. */
object Prefs {
    private const val FILE = "nudge_prefs"

    private fun p(ctx: Context) = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    var Context.parentNumber: String
        get() = p(this).getString("parent", "") ?: ""
        set(v) { p(this).edit().putString("parent", v).apply() }

    var Context.grandparentNumber: String
        get() = p(this).getString("grand", "") ?: ""
        set(v) { p(this).edit().putString("grand", v).apply() }

    /** Hour of day (0-23) after which nudging starts, e.g. 18 = 6pm. */
    var Context.afterHour: Int
        get() = p(this).getInt("afterHour", 19)   // 19 = 7pm, office ends
        set(v) { p(this).edit().putInt("afterHour", v).apply() }

    var Context.armed: Boolean
        get() = p(this).getBoolean("armed", false)
        set(v) { p(this).edit().putBoolean("armed", v).apply() }

    /** "parent" or "grand" — who the next nudge should call. */
    var Context.nextTarget: String
        get() = p(this).getString("nextTarget", "parent") ?: "parent"
        set(v) { p(this).edit().putString("nextTarget", v).apply() }

    var Context.streak: Int
        get() = p(this).getInt("streak", 0)
        set(v) { p(this).edit().putInt("streak", v).apply() }

    /** Day-of-year stamp of the last day both calls were completed. */
    var Context.completedDay: Int
        get() = p(this).getInt("completedDay", -1)
        set(v) { p(this).edit().putInt("completedDay", v).apply() }

    fun Context.isAfterHours(): Boolean {
        val now = Calendar.getInstance()
        return now.get(Calendar.HOUR_OF_DAY) >= afterHour
    }

    fun todayStamp(): Int {
        val c = Calendar.getInstance()
        return c.get(Calendar.YEAR) * 1000 + c.get(Calendar.DAY_OF_YEAR)
    }

    fun Context.isDoneToday(): Boolean = completedDay == todayStamp()

    /** Reset the day's progress so a new day starts at Parent again. */
    fun Context.rolloverIfNewDay() {
        if (completedDay != todayStamp() && nextTarget == "done") {
            nextTarget = "parent"
        }
    }
}
