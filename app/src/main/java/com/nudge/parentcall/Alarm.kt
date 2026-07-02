package com.nudge.parentcall

/**
 * A voice alarm.
 * [days]: Calendar.DAY_OF_WEEK values (1=Sun … 7=Sat) it repeats on.
 *          Empty = one-shot (fires once at the next hour:minute, then disables).
 * [audioPath]: absolute path to the recorded voice memo, or "" to use the
 *              default alarm tone.
 */
data class Alarm(
    val id: Int,
    val hour: Int,
    val minute: Int,
    val label: String,
    val days: List<Int>,
    val audioPath: String,
    val enabled: Boolean
)
