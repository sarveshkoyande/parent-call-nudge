package com.nudge.parentcall

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView

/** Home screen — pick a section. */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        findViewById<MaterialCardView>(R.id.cardCall).setOnClickListener {
            startActivity(Intent(this, CallNudgeActivity::class.java))
        }
        findViewById<MaterialCardView>(R.id.cardAlarm).setOnClickListener {
            startActivity(Intent(this, AlarmActivity::class.java))
        }
    }
}
