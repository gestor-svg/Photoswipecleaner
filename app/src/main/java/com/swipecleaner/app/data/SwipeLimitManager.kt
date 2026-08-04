package com.swipecleaner.app.data

import android.content.Context
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val PREFS_NAME = "swipe_limit_prefs"
private const val KEY_DATE = "date"
private const val KEY_COUNT = "count"
private const val KEY_UNLOCKED = "master_unlocked"

const val DAILY_SWIPE_LIMIT = 30

/**
 * Hash SHA256 del código maestro — el código en texto plano nunca vive en
 * el código fuente ni en el APK, solo este hash. Desbloquea swipes
 * ilimitados de forma permanente en el dispositivo donde se canjee.
 * Independiente del futuro sistema real de tiers/pago (Fase 5, pendiente).
 */
private const val MASTER_CODE_HASH = "94fbc13a5b22b6fd28d7687ef05810269d158007f06c4ce08768d72101d61b7d"

/**
 * Contador global (no por carpeta) de swipes diarios, para el límite
 * gratuito de 30/día. Persistido en SharedPreferences — es honor system,
 * consistente con el resto del esquema del proyecto.
 */
class SwipeLimitManager(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private fun today(): String = dateFormat.format(Date())

    fun getTodayCount(): Int {
        val storedDate = prefs.getString(KEY_DATE, null)
        return if (storedDate == today()) prefs.getInt(KEY_COUNT, 0) else 0
    }

    /** Swipes restantes hoy. Si el dispositivo está desbloqueado, siempre es "ilimitado". */
    fun remaining(): Int {
        if (isUnlocked()) return Int.MAX_VALUE
        return (DAILY_SWIPE_LIMIT - getTodayCount()).coerceAtLeast(0)
    }

    fun registerSwipe(): Int {
        val updated = getTodayCount() + 1
        prefs.edit()
            .putString(KEY_DATE, today())
            .putInt(KEY_COUNT, updated)
            .apply()
        return updated
    }

    fun isUnlocked(): Boolean = prefs.getBoolean(KEY_UNLOCKED, false)

    /** Compara el hash del código ingresado contra el hash maestro. Si coincide, desbloquea permanentemente. */
    fun tryUnlock(code: String): Boolean {
        if (sha256(code) == MASTER_CODE_HASH) {
            prefs.edit().putBoolean(KEY_UNLOCKED, true).apply()
            return true
        }
        return false
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
