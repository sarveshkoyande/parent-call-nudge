package com.nudge.parentcall

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.nudge.parentcall.Prefs.afterHour
import com.nudge.parentcall.Prefs.armed
import com.nudge.parentcall.Prefs.grandViaWhatsApp
import com.nudge.parentcall.Prefs.grandparentNumber
import com.nudge.parentcall.Prefs.nextTarget
import com.nudge.parentcall.Prefs.parentNumber
import com.nudge.parentcall.Prefs.parentViaWhatsApp

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val parent = findViewById<EditText>(R.id.parentNum)
        val grand = findViewById<EditText>(R.id.grandNum)
        val hour = findViewById<EditText>(R.id.hour)
        val parentWA = findViewById<CheckBox>(R.id.parentWA)
        val grandWA = findViewById<CheckBox>(R.id.grandWA)
        val status = findViewById<TextView>(R.id.status)

        parent.setText(parentNumber)
        grand.setText(grandparentNumber)
        hour.setText(afterHour.toString())
        parentWA.isChecked = parentViaWhatsApp
        grandWA.isChecked = grandViaWhatsApp

        requestPermissions()

        findViewById<Button>(R.id.arm).setOnClickListener {
            parentNumber = parent.text.toString().trim()
            grandparentNumber = grand.text.toString().trim()
            afterHour = hour.text.toString().toIntOrNull()?.coerceIn(0, 23) ?: 19
            parentViaWhatsApp = parentWA.isChecked
            grandViaWhatsApp = grandWA.isChecked

            if (parentNumber.isBlank()) {
                Toast.makeText(this, "Enter at least Parent's number.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Background auto-dial needs the "Display over other apps" right.
            // Send the user to grant it, then they tap Arm again.
            if (!canDrawOverlays()) {
                Toast.makeText(this,
                    "Grant \"Display over other apps\", then tap Arm again.",
                    Toast.LENGTH_LONG).show()
                requestOverlayPermission()
                return@setOnClickListener
            }

            armed = true
            nextTarget = "parent"
            startNudgeService()
            status.text = "ARMED ✓  After ${afterHour}:00, your next unlock auto-dials Parent, then Grandparent."
        }

        findViewById<Button>(R.id.disarm).setOnClickListener {
            armed = false
            stopService(Intent(this, ForegroundService::class.java))
            status.text = "Disarmed."
        }

        status.text = when {
            armed && !canDrawOverlays() ->
                "⚠ Armed, but \"Display over other apps\" is OFF — auto-dial may not fire. Re-arm to fix."
            armed -> "ARMED ✓  Auto-dials after ${afterHour}:00."
            else  -> "Not armed."
        }
    }

    private fun canDrawOverlays(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
        }
    }

    private fun startNudgeService() {
        val svc = Intent(this, ForegroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(svc)
        else startService(svc)
    }

    private fun requestPermissions() {
        val perms = mutableListOf(
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_CONTACTS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        ActivityCompat.requestPermissions(this, perms.toTypedArray(), 100)
    }
}
