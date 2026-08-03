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

@Composable
fun rememberTrashOrchestrator(onFinished: (deletedCount: Int) -> Unit): (List<PhotoItem>) -> Unit {
    val context = LocalContext.current
    val trashManager = remember { TrashManager(context) }
    val scope = rememberCoroutineScope()

    var pendingContinuation by remember { mutableStateOf<CancellableContinuation<Boolean>?>(null) }

    fun resumePending(resultCode: Int) {
        pendingContinuation?.resume(resultCode == Activity.RESULT_OK, null)
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
                val sender = trashManager.createTrashIntentSender(uris)
                val confirmed = awaitIntentSender(sender, batchLauncher)
                onFinished(if (confirmed) photos.size else 0)
            } else {
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
