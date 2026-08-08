package com.swipecleaner.app.data

import android.content.Context

private const val PREFS_NAME = "onboarding_prefs"
private const val KEY_TUTORIAL_SEEN = "tutorial_seen"

/**
 * Recuerda si el usuario ya vio el tutorial de bienvenida (swipe, botón
 * Confirmar, dónde activar el código de desbloqueo), para no mostrarlo
 * más de una vez.
 */
class OnboardingManager(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun hasSeenTutorial(): Boolean = prefs.getBoolean(KEY_TUTORIAL_SEEN, false)

    fun markTutorialSeen() {
        prefs.edit().putBoolean(KEY_TUTORIAL_SEEN, true).apply()
    }
}
