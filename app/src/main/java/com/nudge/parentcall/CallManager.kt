package com.nudge.parentcall

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.nudge.parentcall.Prefs.logCall
import com.nudge.parentcall.Prefs.loadTargets
import com.nudge.parentcall.Prefs.saveTargets
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Places the next *due* call directly — no prompt, no countdown, no skip button.
 * Each contact has its own frequency (daily / every N weeks on Sunday), tracked
 * by the day it was last called. The user's only way out is the "end call" button.
 */
object CallManager {

    private const val WA_VOIP = "vnd.android.cursor.item/vnd.com.whatsapp.voip.call"
    private const val WA_PKG = "com.whatsapp"
    private val stamp = DateTimeFormatter.ofPattern("MMM d, HH:mm")

    fun dialNext(ctx: Context) {
        val targets = ctx.loadTargets()
        if (targets.isEmpty()) return

        val today = LocalDate.now()
        val todayEpoch = today.toEpochDay()
        val isSunday = today.dayOfWeek == DayOfWeek.SUNDAY

        // First person in list order who is due right now.
        val idx = targets.indexOfFirst { isDue(it, todayEpoch, isSunday) }
        if (idx < 0) return

        val t = targets[idx]
        val placed = if (t.viaWhatsApp) whatsAppCall(ctx, t.number) else cellularCall(ctx, t.number)
        if (!placed) return

        // Mark called today and log it.
        targets[idx] = t.copy(lastCalledEpochDay = todayEpoch)
        ctx.saveTargets(targets)
        val how = if (t.viaWhatsApp) "WhatsApp" else "Call"
        ctx.logCall("${LocalDateTime.now().format(stamp)} — ${t.name} ($how)")
    }

    /** Is this contact due to be called today? */
    private fun isDue(t: CallTarget, todayEpoch: Long, isSunday: Boolean): Boolean {
        if (t.lastCalledEpochDay == todayEpoch) return false   // already called today
        return if (t.frequencyWeeks <= 0) {
            true                                                // daily, not yet today
        } else {
            if (!isSunday) return false                         // weekly ones only on Sunday
            if (t.lastCalledEpochDay < 0L) true                 // first time -> this Sunday
            else (todayEpoch - t.lastCalledEpochDay) >= t.frequencyWeeks * 7L
        }
    }

    private fun cellularCall(ctx: Context, number: String): Boolean {
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.CALL_PHONE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(ctx, "Open the app once to grant Phone permission.",
                Toast.LENGTH_LONG).show()
            return false
        }
        val call = Intent(Intent.ACTION_CALL, Uri.parse("tel:$number"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(call)
        return true
    }

    private fun whatsAppCall(ctx: Context, number: String): Boolean {
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(ctx, "Open the app once to grant Contacts permission.",
                Toast.LENGTH_LONG).show()
            return false
        }

        val dataId = findWhatsAppCallRow(ctx, number)
        if (dataId == null) {
            Toast.makeText(
                ctx, "Save $number as a contact with WhatsApp to call via WhatsApp.",
                Toast.LENGTH_LONG
            ).show()
            return false
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.parse("content://com.android.contacts/data/$dataId"), WA_VOIP)
            setPackage(WA_PKG)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            ctx.startActivity(intent)
            true
        } catch (e: Exception) {
            Toast.makeText(ctx, "Couldn't open WhatsApp call.", Toast.LENGTH_LONG).show()
            false
        }
    }

    private fun findWhatsAppCallRow(ctx: Context, number: String): Long? {
        val cr = ctx.contentResolver
        val lookup = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number)
        )
        var contactId: Long = -1
        cr.query(lookup, arrayOf(ContactsContract.PhoneLookup.CONTACT_ID), null, null, null)
            ?.use { if (it.moveToFirst()) contactId = it.getLong(0) }
        if (contactId < 0) return null

        var dataId: Long? = null
        cr.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(ContactsContract.Data._ID),
            "${ContactsContract.Data.CONTACT_ID}=? AND ${ContactsContract.Data.MIMETYPE}=?",
            arrayOf(contactId.toString(), WA_VOIP),
            null
        )?.use { if (it.moveToFirst()) dataId = it.getLong(0) }
        return dataId
    }
}
