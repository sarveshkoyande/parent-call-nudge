package com.nudge.parentcall

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.TimePicker
import android.widget.Toast
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.nudge.parentcall.Prefs.loadAlarms
import com.nudge.parentcall.Prefs.nextAlarmId
import com.nudge.parentcall.Prefs.saveAlarms
import java.io.File
import java.util.Calendar

class AlarmEditActivity : AppCompatActivity() {

    private var alarmId = -1
    private var audioPath = ""
    private var recorder: MediaRecorder? = null
    private var isRecording = false
    private var preview: MediaPlayer? = null

    private val dayToggles = mutableListOf<ToggleButton>()
    private val dayLetters = listOf("S", "M", "T", "W", "T", "F", "S") // Sun..Sat

    private lateinit var timePicker: TimePicker
    private lateinit var recordBtn: Button
    private lateinit var recordStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarm_edit)

        timePicker = findViewById(R.id.timePicker)
        recordBtn = findViewById(R.id.record)
        recordStatus = findViewById(R.id.recordStatus)
        val label = findViewById<EditText>(R.id.label)
        val daysContainer = findViewById<LinearLayout>(R.id.daysContainer)

        buildDayToggles(daysContainer)

        val existing = intent.getIntExtra("id", -1).takeIf { it >= 0 }
            ?.let { id -> loadAlarms().firstOrNull { it.id == id } }

        alarmId = existing?.id ?: nextAlarmId()
        audioPath = existing?.audioPath ?: File(filesDir, "alarm_$alarmId.m4a").absolutePath

        if (existing != null) {
            setTime(existing.hour, existing.minute)
            label.setText(existing.label)
            existing.days.forEach { cal -> dayToggles[cal - 1].isChecked = true }
        }
        refreshRecordUi()

        recordBtn.setOnClickListener { if (isRecording) stopRecording() else startRecording() }
        findViewById<Button>(R.id.play).setOnClickListener { playBack() }

        findViewById<Button>(R.id.save).setOnClickListener {
            label.clearFocus()
            saveAlarm(label.text.toString().trim())
        }
    }

    private fun buildDayToggles(container: LinearLayout) {
        for (i in 0 until 7) {
            val tb = ToggleButton(this).apply {
                textOn = dayLetters[i]
                textOff = dayLetters[i]
                text = dayLetters[i]
                val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                lp.marginEnd = 4
                layoutParams = lp
                minWidth = 0
                minimumWidth = 0
            }
            dayToggles.add(tb)
            container.addView(tb)
        }
    }

    private fun setTime(hour: Int, minute: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            timePicker.hour = hour
            timePicker.minute = minute
        } else {
            @Suppress("DEPRECATION") timePicker.currentHour = hour
            @Suppress("DEPRECATION") timePicker.currentMinute = minute
        }
    }

    private fun getHour(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) timePicker.hour
        else @Suppress("DEPRECATION") timePicker.currentHour

    private fun getMinute(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) timePicker.minute
        else @Suppress("DEPRECATION") timePicker.currentMinute

    // ---- recording ----

    private fun startRecording() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 200)
            return
        }
        try {
            recorder = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                MediaRecorder(this) else @Suppress("DEPRECATION") MediaRecorder()).apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(audioPath)
                prepare()
                start()
            }
            isRecording = true
            recordBtn.text = "■ Stop"
            recordStatus.text = "Recording… speak your reason."
        } catch (e: Exception) {
            Toast.makeText(this, "Couldn't start recording.", Toast.LENGTH_SHORT).show()
            recorder?.release(); recorder = null
        }
    }

    private fun stopRecording() {
        try { recorder?.stop() } catch (_: Exception) {}
        recorder?.release()
        recorder = null
        isRecording = false
        recordBtn.text = "● Re-record"
        refreshRecordUi()
    }

    private fun playBack() {
        val f = File(audioPath)
        if (!f.exists()) {
            Toast.makeText(this, "Nothing recorded yet.", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            preview?.release()
            preview = MediaPlayer().apply {
                setDataSource(audioPath); prepare(); start()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Couldn't play recording.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun refreshRecordUi() {
        recordStatus.text = if (File(audioPath).exists())
            "Recording saved ✓  It will loop when the alarm fires."
        else
            "No recording yet — the default alarm tone will be used."
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 200 && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            startRecording()
        }
    }

    // ---- save ----

    private fun saveAlarm(label: String) {
        val days = dayToggles.mapIndexedNotNull { i, tb -> if (tb.isChecked) i + 1 else null }
        val hasAudio = File(audioPath).exists()
        val alarm = Alarm(
            id = alarmId,
            hour = getHour(),
            minute = getMinute(),
            label = label,
            days = days,
            audioPath = if (hasAudio) audioPath else "",
            enabled = true
        )
        val list = loadAlarms().filter { it.id != alarmId }.toMutableList()
        list.add(alarm)
        saveAlarms(list)

        AlarmScheduler.cancel(this, alarmId)
        AlarmScheduler.schedule(this, alarm)

        val next = AlarmScheduler.nextTrigger(alarm)
        val msg = if (next != null) "Alarm saved." else "Saved, but that time has no upcoming day."
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onDestroy() {
        recorder?.release(); recorder = null
        preview?.release(); preview = null
        super.onDestroy()
    }
}
