package com.swipecleaner.app.ui.trash

import android.app.Activity
import android.content.IntentSender
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.swipecleaner.app.data.DeleteOutcome
import com.swipecleaner.app.data.TrashManager
import com.swipecleaner.app.domain.PhotoItem
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Expone una función `(List<PhotoItem>) -> Unit` que envía un lote de fotos
 * a la papelera, resolviendo automáticamente qué flujo usar según la
 * versión de Android:
 *
 * - API 30+: una sola confirmación de sistema para todo el lote.
 * - API 29: confirmación individual por foto (RecoverableSecurityException).
 * - API 26-28: borrado directo.
 *
 * [onFinished] se llama con la cantidad de fotos efectivamente eliminadas/enviadas.
 */
@Composable
fun rememberTrashOrchestrator(onFinished: (deletedCount: Int) -> Unit): (List<PhotoItem>) -> Unit {
    val context = LocalContext.current
    val trashManager = remember { TrashManager(context) }
    val scope = rememberCoroutineScope()

    // Continuación pendiente compartida: cualquiera de los dos launchers
    // (lote o permiso individual) la resume con el resultado del sistema.
    var pendingContinuation by remember { mutableStateOf<CancellableContinuation<Boolean>?>(null) }

    fun resumePending(resultCode: Int) {
        pendingContinuation?.resume(resultCode == Activity.RESULT_OK) { _, _, _ -> }
        pendingContinuation = null
    }

    val batchLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result -> resumePending(result.resultCode) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result -> resumePending(result.resultCode) }

    suspend fun awaitIntentSender(
        sender: IntentSender,
        launcher: ActivityResultLauncher<IntentSenderRequest>
    ): Boolean = suspendCancellableCoroutine { cont ->
        pendingContinuation = cont
        launcher.launch(IntentSenderRequest.Builder(sender).build())
    }

    return trashAction@{ photos ->
        if (photos.isEmpty()) return@trashAction
        scope.launch {
            val uris = photos.map { it.uri }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Flujo moderno: un único diálogo de sistema para todo el lote
                val sender = trashManager.createTrashIntentSender(uris)
                val confirmed = awaitIntentSender(sender, batchLauncher)
                onFinished(if (confirmed) photos.size else 0)
            } else {
                // Flujo legacy: se procesa foto por foto
                var deletedCount = 0
                for (uri in uris) {
                    when (val outcome = trashManager.deleteLegacy(uri)) {
                        is DeleteOutcome.Deleted -> deletedCount++
                        is DeleteOutcome.RequiresPermission -> {
                            val granted = awaitIntentSender(outcome.intentSender, permissionLauncher)
                            if (granted && trashManager.deleteLegacy(uri) is DeleteOutcome.Deleted) {
                                deletedCount++
                            }
                        }
                        is DeleteOutcome.Failed -> Unit
                    }
                }
                onFinished(deletedCount)
            }
        }
    }
}
