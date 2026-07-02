package com.nudge.parentcall

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

/**
 * The app is designed with a light green/purple palette. Force light mode so it
 * looks the same whether or not the phone is in system dark mode (otherwise the
 * dark surfaces clash with the light-oriented colors).
 */
class NudgeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }
}
