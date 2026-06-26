package com.nudge.parentcall

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.nudge.parentcall.Prefs.ensureToday
import com.nudge.parentcall.Prefs.loadTargets
import com.nudge.parentcall.Prefs.nextIndex

/**
 * Places the next call directly — no prompt, no countdown, no skip button.
 * Walks an ordered list of contacts; each can be a normal cellular call OR a
 * WhatsApp voice call. The user's only way out is the phone's "end call" button.
 */
object CallManager {

    // WhatsApp's hidden "voice call this contact" action.
    private const val WA_VOIP = "vnd.android.cursor.item/vnd.com.whatsapp.voip.call"
    private const val WA_PKG = "com.whatsapp"

    fun dialNext(ctx: Context) {
        ctx.ensureToday()                 // reset to first contact on a new day
        val targets = ctx.loadTargets()
        if (targets.isEmpty()) return

        val idx = ctx.nextIndex
        if (idx >= targets.size) return   // everyone called today — done

        val t = targets[idx]
        val placed = if (t.viaWhatsApp) whatsAppCall(ctx, t.number) else cellularCall(ctx, t.number)

        // Only advance if the call actually went out. If a WhatsApp call couldn't
        // be set up, we leave the index so it retries (after the contact is fixed).
        if (placed) ctx.nextIndex = idx + 1
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

    /**
     * Starts a WhatsApp voice call. Needs READ_CONTACTS, and the number must be
     * saved as a contact that has WhatsApp. We look up that contact's hidden
     * WhatsApp "voice call" data row and fire it.
     */
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
                ctx,
                "Save $number as a contact with WhatsApp to call via WhatsApp.",
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

    /** Find the contact for [number], then its WhatsApp voice-call data row id. */
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
