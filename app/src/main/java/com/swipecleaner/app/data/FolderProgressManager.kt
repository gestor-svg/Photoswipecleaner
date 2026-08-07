package com.swipecleaner.app.data

import android.content.Context

private const val PREFS_NAME = "folder_progress_prefs"

/**
 * Recuerda, por carpeta, qué fotos ya se marcaron como "conservar" (✓) —
 * las marcadas para borrar no hace falta guardarlas, MediaStore ya las
 * excluye solo en cuanto quedan en la papelera. Persistido en
 * SharedPreferences, sobrevive cerrar la app por completo.
 *
 * Se guardan IDs específicos, no un índice de posición, porque un índice
 * se rompe en cuanto cambia el contenido de la carpeta entre visitas
 * (fotos nuevas, borradas desde otra app, etc.) — los IDs son robustos a eso.
 */
class FolderProgressManager(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun key(bucketId: String) = "kept_$bucketId"

    fun getKeptIds(bucketId: String): Set<Long> {
        return prefs.getStringSet(key(bucketId), emptySet())
            ?.mapNotNull { it.toLongOrNull() }
            ?.toSet() ?: emptySet()
    }

    fun addKeptId(bucketId: String, photoId: Long) {
        val current = prefs.getStringSet(key(bucketId), emptySet())?.toMutableSet() ?: mutableSetOf()
        current.add(photoId.toString())
        prefs.edit().putStringSet(key(bucketId), current).apply()
    }

    fun removeKeptId(bucketId: String, photoId: Long) {
        val current = prefs.getStringSet(key(bucketId), emptySet())?.toMutableSet() ?: return
        current.remove(photoId.toString())
        prefs.edit().putStringSet(key(bucketId), current).apply()
    }

    /** "Ver desde el principio" — olvida el progreso guardado de esta carpeta. */
    fun resetFolder(bucketId: String) {
        prefs.edit().remove(key(bucketId)).apply()
    }
}
