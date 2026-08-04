package com.swipecleaner.app.data

import android.content.Context
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.time.YearMonth
import java.util.Date
import java.util.Locale

private const val PREFS_NAME = "swipe_limit_prefs"
private const val KEY_DATE = "date"
private const val KEY_COUNT = "count"
private const val KEY_MASTER_UNLOCKED = "master_unlocked"
private const val KEY_TIER1_EXPIRY = "tier1_expiry_millis"

const val DAILY_SWIPE_LIMIT = 30
private const val TIER1_DURATION_DAYS = 90L
private const val TIER1_DURATION_MILLIS = TIER1_DURATION_DAYS * 24 * 60 * 60 * 1000

/**
 * Hash SHA256 del código maestro — el código en texto plano nunca vive en
 * el código fuente ni en el APK, solo este hash. Desbloquea swipes
 * ilimitados de forma PERMANENTE en el dispositivo donde se canjee.
 */
private const val MASTER_CODE_HASH = "94fbc13a5b22b6fd28d7687ef05810269d158007f06c4ce08768d72101d61b7d"

/**
 * Secreto usado para calcular el código mensual de Tier 1 (donación real).
 * Distinto del código maestro a propósito. La fórmula es
 * SHA256("millonario-YYYY-MM") tomando los primeros 10 caracteres en
 * mayúsculas — se genera fuera de la app (nunca en una página pública) y se
 * entrega manualmente por WhatsApp tras confirmar el pago en Mercado Pago.
 */
private const val TIER1_SECRET = "millonario"

/**
 * Contador global (no por carpeta) de swipes diarios, para el límite
 * gratuito de 30/día — y desbloqueo permanente (código maestro) o temporal
 * de 90 días (código mensual de Tier 1). Todo persistido en
 * SharedPreferences, honor system, consistente con el resto del proyecto.
 */
class SwipeLimitManager(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private fun today(): String = dateFormat.format(Date())

    fun getTodayCount(): Int {
        val storedDate = prefs.getString(KEY_DATE, null)
        return if (storedDate == today()) prefs.getInt(KEY_COUNT, 0) else 0
    }

    fun isMasterUnlocked(): Boolean = prefs.getBoolean(KEY_MASTER_UNLOCKED, false)

    fun isTier1Active(): Boolean = prefs.getLong(KEY_TIER1_EXPIRY, 0L) > System.currentTimeMillis()

    /** Días restantes de Tier 1 (0 si no está activo). Redondea hacia arriba. */
    fun tier1DaysRemaining(): Int {
        val diff = prefs.getLong(KEY_TIER1_EXPIRY, 0L) - System.currentTimeMillis()
        if (diff <= 0) return 0
        return (diff / (24 * 60 * 60 * 1000)).toInt() + 1
    }

    /** Swipes restantes hoy. Ilimitado si hay código maestro o Tier 1 activo. */
    fun remaining(): Int {
        if (isMasterUnlocked() || isTier1Active()) return Int.MAX_VALUE
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

    /**
     * Intenta canjear un código: primero contra el maestro (permanente),
     * luego contra el código de Tier 1 del mes actual o el anterior (margen
     * de gracia). Al validar Tier 1, la fecha de vencimiento se REEMPLAZA por
     * 90 días desde hoy (no se acumula con lo que quedaba).
     */
    fun tryUnlock(code: String): UnlockResult {
        val trimmed = code.trim()
        if (trimmed.isEmpty()) return UnlockResult.Invalid

        if (sha256(trimmed) == MASTER_CODE_HASH) {
            prefs.edit().putBoolean(KEY_MASTER_UNLOCKED, true).apply()
            return UnlockResult.MasterUnlocked
        }

        val input = trimmed.uppercase()
        if (input == tier1CodeFor(0L) || input == tier1CodeFor(-1L)) {
            val newExpiry = System.currentTimeMillis() + TIER1_DURATION_MILLIS
            prefs.edit().putLong(KEY_TIER1_EXPIRY, newExpiry).apply()
            return UnlockResult.Tier1Unlocked(TIER1_DURATION_DAYS.toInt())
        }

        return UnlockResult.Invalid
    }

    private fun tier1CodeFor(monthOffset: Long): String {
        val ym = YearMonth.now().plusMonths(monthOffset)
        val key = "%04d-%02d".format(ym.year, ym.monthValue)
        return sha256("$TIER1_SECRET-$key").substring(0, 10).uppercase()
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

sealed interface UnlockResult {
    data object MasterUnlocked : UnlockResult
    data class Tier1Unlocked(val days: Int) : UnlockResult
    data object Invalid : UnlockResult
}
