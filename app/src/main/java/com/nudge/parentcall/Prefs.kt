package com.nudge.parentcall

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

/** Single place for all saved state. Backed by SharedPreferences. */
object Prefs {
    private const val FILE = "nudge_prefs"

    private fun p(ctx: Context) = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    /** Hour of day (0-23) after which nudging starts, e.g. 19 = 7pm. */
    var Context.afterHour: Int
        get() = p(this).getInt("afterHour", 19)
        set(v) { p(this).edit().putInt("afterHour", v).apply() }

    var Context.armed: Boolean
        get() = p(this).getBoolean("armed", false)
        set(v) { p(this).edit().putBoolean("armed", v).apply() }

    /** Index of the next person to call in the ordered list. */
    var Context.nextIndex: Int
        get() = p(this).getInt("nextIndex", 0)
        set(v) { p(this).edit().putInt("nextIndex", v).apply() }

    /** The day [nextIndex] belongs to, so progress resets each new day. */
    var Context.progressDay: Int
        get() = p(this).getInt("progressDay", -1)
        set(v) { p(this).edit().putInt("progressDay", v).apply() }

    // ---- the ordered contact list, stored as JSON ----

    fun Context.loadTargets(): MutableList<CallTarget> {
        val raw = p(this).getString("targets", "[]") ?: "[]"
        val arr = JSONArray(raw)
        val list = mutableListOf<CallTarget>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(CallTarget(o.optString("name"), o.optString("number"), o.optBoolean("wa")))
        }
        return list
    }

    fun Context.saveTargets(list: List<CallTarget>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject().put("name", it.name).put("number", it.number).put("wa", it.viaWhatsApp))
        }
        p(this).edit().putString("targets", arr.toString()).apply()
    }

    // ---- time / day helpers ----

    fun Context.isAfterHours(): Boolean {
        val now = Calendar.getInstance()
        return now.get(Calendar.HOUR_OF_DAY) >= afterHour
    }

    fun todayStamp(): Int {
        val c = Calendar.getInstance()
        return c.get(Calendar.YEAR) * 1000 + c.get(Calendar.DAY_OF_YEAR)
    }

    /** On a new day, start the list over from the first contact. */
    fun Context.ensureToday() {
        if (progressDay != todayStamp()) {
            nextIndex = 0
            progressDay = todayStamp()
        }
    }
}
