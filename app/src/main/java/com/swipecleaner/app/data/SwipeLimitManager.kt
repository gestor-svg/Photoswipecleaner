package com.swipecleaner.app.data

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val PREFS_NAME = "swipe_limit_prefs"
private const val KEY_DATE = "date"
private const val KEY_COUNT = "count"

const val DAILY_SWIPE_LIMIT = 30

/**
 * Contador global (no por carpeta) de swipes diarios, para el límite
 * gratuito de 30/día. Persistido en SharedPreferences — es honor system,
 * consistente con el resto del esquema del proyecto (se puede resetear
 * borrando datos de la app, y está bien así: mismo criterio ya aceptado
 * con el sistema de referidos).
 */
class SwipeLimitManager(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private fun today(): String = dateFormat.format(Date())

    /** Swipes ya usados hoy. Si cambió el día desde el último guardado, resetea a 0. */
    fun getTodayCount(): Int {
        val storedDate = prefs.getString(KEY_DATE, null)
        return if (storedDate == today()) prefs.getInt(KEY_COUNT, 0) else 0
    }

    fun remaining(): Int = (DAILY_SWIPE_LIMIT - getTodayCount()).coerceAtLeast(0)

    /** Registra un swipe nuevo (cualquier dirección). Devuelve el conteo actualizado del día. */
    fun registerSwipe(): Int {
        val updated = getTodayCount() + 1
        prefs.edit()
            .putString(KEY_DATE, today())
            .putInt(KEY_COUNT, updated)
            .apply()
        return updated
    }
}
