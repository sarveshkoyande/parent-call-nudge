package com.nudge.parentcall

/** One person to call, in order. [viaWhatsApp] picks WhatsApp vs a normal call. */
data class CallTarget(
    val name: String,
    val number: String,
    val viaWhatsApp: Boolean
)
