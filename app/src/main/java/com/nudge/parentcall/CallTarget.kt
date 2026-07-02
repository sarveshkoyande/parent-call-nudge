package com.nudge.parentcall

/**
 * One person to call, in order.
 * [viaWhatsApp] picks WhatsApp vs a normal call.
 * [frequencyWeeks]: 0 = every day; 1 = weekly; 2 = every 2 weeks; ... (weekly ones
 * only fire on Sundays).
 * [lastCalledEpochDay]: the day this person was last called (epoch day), or -1 if never.
 */
data class CallTarget(
    val name: String,
    val number: String,
    val viaWhatsApp: Boolean,
    val frequencyWeeks: Int = 0,
    val lastCalledEpochDay: Long = -1L
)
