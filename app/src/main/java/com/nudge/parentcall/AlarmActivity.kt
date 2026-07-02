package com.nudge.parentcall

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.materialswitch.MaterialSwitch
import com.nudge.parentcall.Prefs.loadAlarms
import com.nudge.parentcall.Prefs.saveAlarms
import java.util.Calendar

class AlarmActivity : AppCompatActivity() {

    private lateinit var container: LinearLayout
    private lateinit var emptyHint: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarm)

        container = findViewById(R.id.alarmsContainer)
        emptyHint = findViewById(R.id.emptyHint)

        findViewById<Button>(R.id.addAlarm).setOnClickListener {
            startActivity(Intent(this, AlarmEditActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        container.removeAllViews()
        val alarms = loadAlarms()
        emptyHint.visibility = if (alarms.isEmpty()) View.VISIBLE else View.GONE

        alarms.forEach { a ->
            val row = layoutInflater.inflate(R.layout.alarm_row, container, false)
            row.findViewById<TextView>(R.id.alarmTime).text =
                String.format("%02d:%02d", a.hour, a.minute)
            row.findViewById<TextView>(R.id.alarmSub).text = subtitle(a)

            val sw = row.findViewById<MaterialSwitch>(R.id.alarmEnabled)
            sw.isChecked = a.enabled
            sw.setOnCheckedChangeListener { _, checked ->
                val updated = loadAlarms().map { if (it.id == a.id) it.copy(enabled = checked) else it }
                saveAlarms(updated)
                if (checked) AlarmScheduler.schedule(this, a.copy(enabled = true))
                else AlarmScheduler.cancel(this, a.id)
            }

            row.setOnClickListener {
                startActivity(Intent(this, AlarmEditActivity::class.java).putExtra("id", a.id))
            }
            row.findViewById<Button>(R.id.alarmDelete).setOnClickListener {
                AlarmScheduler.cancel(this, a.id)
                saveAlarms(loadAlarms().filter { it.id != a.id })
                render()
            }
            container.addView(row)
        }
    }

    private fun subtitle(a: Alarm): String {
        val voice = if (a.audioPath.isNotBlank()) "🎙 voice" else "default tone"
        val days = if (a.days.isEmpty()) "Once" else a.days.sorted().joinToString(" ") { dayLabel(it) }
        val lbl = if (a.label.isBlank()) "" else " · ${a.label}"
        return "$days · $voice$lbl"
    }

    private fun dayLabel(calDay: Int): String = when (calDay) {
        Calendar.SUNDAY -> "Sun"
        Calendar.MONDAY -> "Mon"
        Calendar.TUESDAY -> "Tue"
        Calendar.WEDNESDAY -> "Wed"
        Calendar.THURSDAY -> "Thu"
        Calendar.FRIDAY -> "Fri"
        Calendar.SATURDAY -> "Sat"
        else -> "?"
    }
}
