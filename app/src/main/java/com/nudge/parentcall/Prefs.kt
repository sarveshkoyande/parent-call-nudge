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

    // ---- the ordered contact list, stored as JSON ----

    fun Context.loadTargets(): MutableList<CallTarget> {
        val raw = p(this).getString("targets", "[]") ?: "[]"
        val arr = JSONArray(raw)
        val list = mutableListOf<CallTarget>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(
                CallTarget(
                    name = o.optString("name"),
                    number = o.optString("number"),
                    viaWhatsApp = o.optBoolean("wa"),
                    frequencyWeeks = o.optInt("freq", 0),
                    lastCalledEpochDay = o.optLong("last", -1L)
                )
            )
        }
        return list
    }

    fun Context.saveTargets(list: List<CallTarget>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(
                JSONObject()
                    .put("name", it.name)
                    .put("number", it.number)
                    .put("wa", it.viaWhatsApp)
                    .put("freq", it.frequencyWeeks)
                    .put("last", it.lastCalledEpochDay)
            )
        }
        p(this).edit().putString("targets", arr.toString()).apply()
    }

    // ---- call history (most recent first, capped at 50) ----

    fun Context.logCall(entry: String) {
        val arr = JSONArray(p(this).getString("history", "[]") ?: "[]")
        arr.put(entry)
        val start = maxOf(0, arr.length() - 50)
        val trimmed = JSONArray()
        for (i in start until arr.length()) trimmed.put(arr.getString(i))
        p(this).edit().putString("history", trimmed.toString()).apply()
    }

    fun Context.recentCalls(): List<String> {
        val arr = JSONArray(p(this).getString("history", "[]") ?: "[]")
        return (arr.length() - 1 downTo 0).map { arr.getString(it) }
    }

    // ---- time helper ----

    fun Context.isAfterHours(): Boolean {
        val now = Calendar.getInstance()
        return now.get(Calendar.HOUR_OF_DAY) >= afterHour
    }
}
