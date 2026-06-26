package com.nudge.parentcall

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.Settings
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.nudge.parentcall.Prefs.afterHour
import com.nudge.parentcall.Prefs.armed
import com.nudge.parentcall.Prefs.loadTargets
import com.nudge.parentcall.Prefs.saveTargets

class MainActivity : AppCompatActivity() {

    private val targets = mutableListOf<CallTarget>()
    private lateinit var container: LinearLayout
    private lateinit var status: TextView

    // Launches the system contact picker and reads the chosen phone number.
    private val pickContact =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
            if (res.resultCode == RESULT_OK) {
                res.data?.data?.let { uri ->
                    readPickedContact(uri)?.let {
                        targets.add(it)
                        saveTargets(targets)
                        renderRows()
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        container = findViewById(R.id.contactsContainer)
        status = findViewById(R.id.status)
        val hour = findViewById<EditText>(R.id.hour)

        targets.clear()
        targets.addAll(loadTargets())
        hour.setText(afterHour.toString())
        renderRows()

        requestPermissions()

        findViewById<Button>(R.id.addContact).setOnClickListener {
            pickContact.launch(
                Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
            )
        }

        findViewById<Button>(R.id.arm).setOnClickListener {
            afterHour = hour.text.toString().toIntOrNull()?.coerceIn(0, 23) ?: 19
            saveTargets(targets)

            if (targets.isEmpty()) {
                Toast.makeText(this, "Add at least one contact.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!canDrawOverlays()) {
                Toast.makeText(this,
                    "Grant \"Display over other apps\", then tap Arm again.",
                    Toast.LENGTH_LONG).show()
                requestOverlayPermission()
                return@setOnClickListener
            }

            armed = true
            startNudgeService()
            updateStatus()
        }

        findViewById<Button>(R.id.disarm).setOnClickListener {
            armed = false
            stopService(Intent(this, ForegroundService::class.java))
            status.text = "Disarmed."
        }

        updateStatus()
    }

    /** Rebuild the contact rows from [targets]. */
    private fun renderRows() {
        container.removeAllViews()
        targets.forEachIndexed { i, t ->
            val row = layoutInflater.inflate(R.layout.contact_row, container, false)
            row.findViewById<TextView>(R.id.rowName).text = "${i + 1}. ${t.name}"
            row.findViewById<TextView>(R.id.rowNumber).text = t.number

            val wa = row.findViewById<CheckBox>(R.id.rowWA)
            wa.isChecked = t.viaWhatsApp
            wa.setOnCheckedChangeListener { _, checked ->
                targets[i] = t.copy(viaWhatsApp = checked)
                saveTargets(targets)
            }

            row.findViewById<Button>(R.id.rowRemove).setOnClickListener {
                targets.removeAt(i)
                saveTargets(targets)
                renderRows()
            }
            container.addView(row)
        }
    }

    /** Read name + number from a picked Phone contact uri. */
    private fun readPickedContact(uri: Uri): CallTarget? {
        contentResolver.query(
            uri,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
            ),
            null, null, null
        )?.use {
            if (it.moveToFirst()) {
                val number = it.getString(0)?.replace(" ", "") ?: return null
                val name = it.getString(1) ?: number
                return CallTarget(name, number, false)
            }
        }
        return null
    }

    private fun updateStatus() {
        status.text = when {
            armed && !canDrawOverlays() ->
                "⚠ Armed, but \"Display over other apps\" is OFF — auto-dial may not fire. Re-arm to fix."
            armed -> "ARMED ✓  After ${afterHour}:00, your next unlock calls them in order."
            else -> "Not armed."
        }
    }

    private fun canDrawOverlays(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
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
