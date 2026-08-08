package com.swipecleaner.app.data

import android.content.Context
import java.security.MessageDigest
import java.time.YearMonth

private const val PREFS_NAME = "swipe_limit_prefs"
private const val KEY_HOUR_BUCKET = "hour_bucket"
private const val KEY_COUNT = "count"
private const val KEY_MASTER_UNLOCKED = "master_unlocked"
private const val KEY_TIER1_EXPIRY = "tier1_expiry_millis"

/** Límite gratuito: 30 swipes por hora (antes era por día). */
const val DAILY_SWIPE_LIMIT = 30
private const val MS_PER_HOUR = 60L * 60L * 1000L

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
 * Contador global (no por carpeta) de swipes por hora, para el límite
 * gratuito de 30/hora — y desbloqueo permanente (código maestro) o temporal
 * de 90 días (código mensual de Tier 1). Todo persistido en
 * SharedPreferences, honor system, consistente con el resto del proyecto.
 *
 * Cambio de esta sesión: antes el cupo era 30/día (reset a medianoche),
 * ahora es 30/hora (reset cada vez que cambia la hora en punto) — motivo:
 * el cupo diario se sentía demasiado restrictivo, se agotaba rápido y no
 * dejaba seguir usando la app en el mismo día.
 */
class SwipeLimitManager(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Identificador único de la hora actual (cambia cada hora en punto). */
    private fun currentHourBucket(): Long = System.currentTimeMillis() / MS_PER_HOUR

    fun getCurrentWindowCount(): Int {
        val storedBucket = prefs.getLong(KEY_HOUR_BUCKET, -1L)
        return if (storedBucket == currentHourBucket()) prefs.getInt(KEY_COUNT, 0) else 0
    }

    /**
     * Timestamp (millis) del inicio de la siguiente hora — momento exacto
     * en el que el cupo vuelve a estar disponible. Se usa solo para el
     * conteo regresivo visual de la pantalla de límite alcanzado.
     */
    fun resetAtMillis(): Long {
        val currentHourStart = (System.currentTimeMillis() / MS_PER_HOUR) * MS_PER_HOUR
        return currentHourStart + MS_PER_HOUR
    }

    /** Swipes restantes esta hora. Ilimitado si hay código maestro o Tier 1 activo. */
    fun remaining(): Int {
        if (isMasterUnlocked() || isTier1Active()) return Int.MAX_VALUE
        return (DAILY_SWIPE_LIMIT - getCurrentWindowCount()).coerceAtLeast(0)
    }

    fun registerSwipe(): Int {
        val updated = getCurrentWindowCount() + 1
        prefs.edit()
            .putLong(KEY_HOUR_BUCKET, currentHourBucket())
            .putInt(KEY_COUNT, updated)
            .apply()
        return updated
    }

    fun isMasterUnlocked(): Boolean = prefs.getBoolean(KEY_MASTER_UNLOCKED, false)

    fun isTier1Active(): Boolean = prefs.getLong(KEY_TIER1_EXPIRY, 0L) > System.currentTimeMillis()

    /** Días restantes de Tier 1 (0 si no está activo). Redondea hacia arriba. */
    fun tier1DaysRemaining(): Int {
        val diff = prefs.getLong(KEY_TIER1_EXPIRY, 0L) - System.currentTimeMillis()
        if (diff <= 0) return 0
        return (diff / (24 * 60 * 60 * 1000)).toInt() + 1
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
