package com.swipecleaner.app.data

import android.app.RecoverableSecurityException
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface DeleteOutcome {
    data object Deleted : DeleteOutcome
    data class RequiresPermission(val intentSender: IntentSender) : DeleteOutcome
    data class Failed(val message: String) : DeleteOutcome
}

class TrashManager(private val context: Context) {

    /**
     * API 30+: construye la solicitud para mover un LOTE completo de fotos
     * a la papelera nativa del sistema. El usuario confirma una sola vez
     * para todo el lote (no una vez por foto).
     */
    fun createTrashIntentSender(uris: List<Uri>): IntentSender {
        val pendingIntent = MediaStore.createTrashRequest(context.contentResolver, uris, true)
        return pendingIntent.intentSender
    }

    /**
     * Fallback para API < 30, donde no existe papelera nativa unificada:
     * - API 29: el borrado puede lanzar RecoverableSecurityException, que
     *   trae un IntentSender para pedir el permiso puntual de esa foto.
     * - API 26-28: borrado directo (requiere WRITE_EXTERNAL_STORAGE legacy).
     */
    suspend fun deleteLegacy(uri: Uri): DeleteOutcome = withContext(Dispatchers.IO) {
        try {
            val rows = context.contentResolver.delete(uri, null, null)
            if (rows > 0) DeleteOutcome.Deleted else DeleteOutcome.Failed("No se pudo borrar")
        } catch (e: SecurityException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && e is RecoverableSecurityException) {
                DeleteOutcome.RequiresPermission(e.userAction.actionIntent.intentSender)
            } else {
                DeleteOutcome.Failed(e.message ?: "Permiso denegado")
            }
        }
    }
}
