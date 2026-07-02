package com.nudge.parentcall

import android.app.NotificationManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.nudge.parentcall.Prefs.loadAlarms
import java.io.File

/** Full-screen alarm: loops the recorded voice until "Dismiss". Voice only. */
class AlarmRingActivity : AppCompatActivity() {

    private var player: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Show over the lock screen and wake the display.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContentView(R.layout.activity_alarm_ring)

        val id = intent.getIntExtra("id", -1)
        val alarm = loadAlarms().firstOrNull { it.id == id }
        findViewById<TextView>(R.id.ringLabel).text =
            alarm?.label?.ifBlank { "Alarm" } ?: "Alarm"

        // Clear the notification that launched us.
        getSystemService(NotificationManager::class.java).cancel(id)

        maxAlarmVolume()
        startLoopingAudio(alarm?.audioPath)

        findViewById<Button>(R.id.dismiss).setOnClickListener { stopAndFinish() }
    }

    private fun maxAlarmVolume() {
        val am = getSystemService(AUDIO_SERVICE) as AudioManager
        am.setStreamVolume(
            AudioManager.STREAM_ALARM,
            am.getStreamMaxVolume(AudioManager.STREAM_ALARM),
            0
        )
    }

    private fun startLoopingAudio(path: String?) {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()

        player = MediaPlayer().apply {
            setAudioAttributes(attrs)
            isLooping = true
            try {
                if (!path.isNullOrBlank() && File(path).exists()) {
                    setDataSource(path)
                } else {
                    // No recording — fall back to the default alarm tone.
                    val tone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    setDataSource(this@AlarmRingActivity, tone)
                }
                prepare()
                start()
            } catch (e: Exception) {
                release()
                player = null
            }
        }
    }

    private fun stopAndFinish() {
        player?.let { try { it.stop() } catch (_: Exception) {}; it.release() }
        player = null
        finish()
    }

    override fun onDestroy() {
        player?.release()
        player = null
        super.onDestroy()
    }

    // Don't let Back silently kill the alarm without dismissing.
    override fun onBackPressed() { stopAndFinish() }
}
