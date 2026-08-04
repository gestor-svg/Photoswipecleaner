@file:OptIn(ExperimentalMaterial3Api::class)

package com.swipecleaner.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import com.swipecleaner.app.domain.BucketFolder
import com.swipecleaner.app.domain.PhotoItem
import com.swipecleaner.app.domain.SwipeDirection
import com.swipecleaner.app.ui.trash.rememberTrashOrchestrator
import java.util.Locale

@Composable
fun PhotoDeckScreen(
    folder: BucketFolder,
    onBack: () -> Unit,
    viewModel: PhotoDeckViewModel = viewModel()
) {
    LaunchedEffect(folder.bucketId) {
        viewModel.loadPhotos(folder.bucketId)
    }

    val state by viewModel.state.collectAsState()
    val loaded = state as? PhotoDeckState.Loaded

    val trashAction = rememberTrashOrchestrator(
        onFinished = { deletedCount -> viewModel.onTrashConfirmed(deletedCount) }
    )

    var pendingCorruptedBatch by remember { mutableStateOf<List<PhotoItem>>(emptyList()) }
    val corruptedTrashAction = rememberTrashOrchestrator(
        onFinished = { deletedCount ->
            viewModel.onCorruptedTrashConfirmed(pendingCorruptedBatch, deletedCount)
            pendingCorruptedBatch = emptyList()
        }
    )

    // Mensaje de celebración al confirmar ("¡Felicidades! Vas a liberar X").
    var celebrateMessage by remember { mutableStateOf<String?>(null) }

    fun confirmWithCelebration(photos: List<PhotoItem>, freedBytes: Long) {
        if (photos.isEmpty()) return
        celebrateMessage = "¡Felicidades! Vas a liberar ${formatSize(freedBytes)} 🎉"
        trashAction(photos)
    }

    var showExitDialog by remember { mutableStateOf(false) }

    fun requestExit() {
        if (loaded != null && loaded.trashCandidates.isNotEmpty()) {
            showExitDialog = true
        } else {
            onBack()
        }
    }

    BackHandler(onBack = { requestExit() })

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Tienes fotos marcadas sin enviar") },
            text = { Text("¿Qué quieres hacer con las fotos que marcaste para borrar en esta carpeta?") },
            confirmButton = {
                TextButton(onClick = {
                    showExitDialog = false
                    loaded?.let { trashAction(it.trashCandidates) }
                    onBack()
                }) { Text("Enviar y salir") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        showExitDialog = false
                        onBack()
                    }) { Text("Salir sin cambios") }
                    TextButton(onClick = { showExitDialog = false }) { Text("Cancelar") }
                }
            }
        )
    }

    celebrateMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { celebrateMessage = null },
            confirmButton = {
                TextButton(onClick = { celebrateMessage = null }) { Text("¡Genial!") }
            },
            text = { Text(msg) }
        )
    }

    // Disparador de swipe manual desde los botones ✕/✓. Cada click suma un
    // token nuevo para que nunca se pierda un click (ver ManualSwipeRequest).
    var manualSwipeCounter by remember { mutableStateOf(0) }
    var manualSwipe by remember { mutableStateOf<ManualSwipeRequest?>(null) }

    var corruptedBannerDismissed by remember(loaded?.corruptedPhotos) { mutableStateOf(false) }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(folder.name) },
            navigationIcon = {
                TextButton(onClick = { requestExit() }) { Text("← Carpetas") }
            },
            actions = {
                TextButton(
                    onClick = { viewModel.undo() },
                    enabled = loaded?.history?.isNotEmpty() == true
                ) { Text("Deshacer") }
                TextButton(
                    onClick = { loaded?.let { confirmWithCelebration(it.trashCandidates, it.freedBytes) } },
                    enabled = loaded?.trashCandidates?.isNotEmpty() == true
                ) { Text("Confirmar") }
            }
        )
    }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            if (loaded != null) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Revisadas ${loaded.currentIndex.coerceAtMost(loaded.photos.size)} de ${loaded.photos.size} · " +
                            "${formatSize(loaded.freedBytes)} marcados para papelera" +
                            if (loaded.confirmedCount > 0) " · ${loaded.confirmedCount} ya enviadas" else "",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            val corrupted = loaded?.corruptedPhotos.orEmpty()
            if (corrupted.isNotEmpty() && !corruptedBannerDismissed) {
                CorruptedFilesBanner(
                    count = corrupted.size,
                    onSend = {
                        pendingCorruptedBatch = corrupted
                        corruptedTrashAction(corrupted)
                    },
                    onDismiss = { corruptedBannerDismissed = true }
                )
            }

            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                when (val s = state) {
                    is PhotoDeckState.Loading -> CircularProgressIndicator()

                    is PhotoDeckState.Error -> Text("Error: ${s.message}")

                    is PhotoDeckState.Loaded -> {
                        val photos = s.photos
                        val index = s.currentIndex

                        if (s.remainingSwipesToday <= 0) {
                            LimitReachedView(
                                state = s,
                                onConfirm = { trashAction(s.trashCandidates) }
                            )
                        } else if (index >= photos.size) {
                            if (photos.isEmpty()) {
                                Text("Esta carpeta no tiene fotos")
                            } else {
                                DeckSummary(
                                    state = s,
                                    onConfirm = { confirmWithCelebration(s.trashCandidates, s.freedBytes) }
                                )
                            }
                        } else {
                            val top = photos[index]
                            val next = photos.getOrNull(index + 1)

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .aspectRatio(0.75f),
                                contentAlignment = Alignment.Center
                            ) {
                                if (next != null) {
                                    Card(modifier = Modifier.fillMaxSize()) {
                                        PhotoCardImage(uri = next.uri, contentDescription = null)
                                    }
                                }

                                SwipeableCard(
                                    manualTrigger = manualSwipe,
                                    onSwiped = { decision ->
                                        val direction = if (decision == SwipeDecision.LEFT)
                                            SwipeDirection.LEFT else SwipeDirection.RIGHT
                                        viewModel.onSwipe(direction)
                                    }
                                ) { progressPx ->
                                    Card(modifier = Modifier.fillMaxSize()) {
                                        Box {
                                            PhotoCardImage(uri = top.uri, contentDescription = top.displayName)
                                            val leftAlpha = (-progressPx / 300f).coerceIn(0f, 1f)
                                            val rightAlpha = (progressPx / 300f).coerceIn(0f, 1f)
                                            Text(
                                                "PAPELERA",
                                                color = Color(0xFFE2685F).copy(alpha = leftAlpha),
                                                modifier = Modifier.align(Alignment.TopStart).padding(16.dp)
                                            )
                                            Text(
                                                "CONSERVAR",
                                                color = Color(0xFF7FAE6A).copy(alpha = rightAlpha),
                                                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Botones manuales ✕/✓ — ocultos si ya se llegó al límite diario.
            if (loaded != null && loaded.remainingSwipesToday > 0 && loaded.currentIndex < loaded.photos.size) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    OutlinedButton(onClick = {
                        manualSwipeCounter++
                        manualSwipe = ManualSwipeRequest(SwipeDecision.LEFT, manualSwipeCounter)
                    }) {
                        Text("✕  Borrar")
                    }
                    OutlinedButton(onClick = {
                        manualSwipeCounter++
                        manualSwipe = ManualSwipeRequest(SwipeDecision.RIGHT, manualSwipeCounter)
                    }) {
                        Text("✓  Conservar")
                    }
                }
            }
        }
    }
}

/**
 * Pantalla que reemplaza el deck cuando se agotó el cupo diario de 30
 * swipes. Muestra lo liberado hoy, lo que falta en esta carpeta, y un botón
 * de donar como placeholder (aún no existe el sistema de tiers/pago real).
 */
@Composable
private fun LimitReachedView(state: PhotoDeckState.Loaded, onConfirm: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(24.dp)
    ) {
        Text("Por hoy ya revisaste tus 30 fotos gratis 🙌", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "Llevas liberado ${formatSize(state.freedBytes + state.confirmedBytes)} en total hoy.",
            style = MaterialTheme.typography.bodyMedium
        )
        if (state.currentIndex < state.photos.size) {
            Spacer(Modifier.height(4.dp))
            Text(
                "Te faltan ${state.photos.size - state.currentIndex} fotos por revisar en esta carpeta.",
                style = MaterialTheme.typography.bodySmall
            )
        }
        Spacer(Modifier.height(16.dp))
        if (state.trashCandidates.isNotEmpty()) {
            Button(onClick = onConfirm) { Text("Enviar lo marcado a la papelera") }
            Spacer(Modifier.height(12.dp))
        }
        Text(
            "Si quieres seguir liberando espacio hoy mismo, es muy fácil: ayúdanos donando 💛",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = {
            val intent = android.content.Intent(
                android.content.Intent.ACTION_VIEW,
                android.net.Uri.parse("https://gestor-svg.github.io/Photoswipecleaner/donar.html")
            )
            context.startActivity(intent)
        }) {
            Text("Donar")
        }

@Composable
private fun CorruptedFilesBanner(count: Int, onSend: () -> Unit, onDismiss: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Text(
                "$count archivo${if (count == 1) "" else "s"} dañado${if (count == 1) "" else "s"} encontrado${if (count == 1) "" else "s"}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                "No se pueden leer, por lo que no se pueden reparar ni recuperar.",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(8.dp))
            Row {
                Button(onClick = onSend) { Text("Enviar a la papelera") }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = onDismiss) { Text("Ignorar") }
            }
        }
    }
}

@Composable
private fun PhotoCardImage(uri: android.net.Uri, contentDescription: String?) {
    val painter = rememberAsyncImagePainter(uri)
    val painterState = painter.state

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painter,
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        if (painterState is AsyncImagePainter.State.Error) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("⚠️", style = MaterialTheme.typography.displayMedium)
                Spacer(Modifier.height(8.dp))
                Text("Imagen dañada", style = MaterialTheme.typography.bodyMedium)
                Text("Desliza para continuar", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun formatSize(bytes: Long): String {
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1024) String.format(Locale.getDefault(), "%.1f GB", mb / 1024)
    else String.format(Locale.getDefault(), "%.1f MB", mb)
}

@Composable
private fun DeckSummary(state: PhotoDeckState.Loaded, onConfirm: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (state.confirmedCount > 0) {
            Text("Listo ✅", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text("${state.confirmedCount} fotos enviadas a la papelera del sistema en total")
            Spacer(Modifier.height(4.dp))
            Text("Se eliminarán automáticamente en 30 días", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(16.dp))
        }

        if (state.trashCandidates.isEmpty()) {
            if (state.confirmedCount == 0) {
                Text("Carpeta revisada ✅")
                Spacer(Modifier.height(4.dp))
                Text("No marcaste ninguna foto para borrar", style = MaterialTheme.typography.bodySmall)
            } else {
                Text("Carpeta terminada", style = MaterialTheme.typography.bodySmall)
            }
        } else {
            Text("Carpeta revisada", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                "${state.trashCandidates.size} fotos marcadas · ${formatSize(state.freedBytes)}",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onConfirm) { Text("Enviar a la papelera") }
        }
    }
}
