@file:OptIn(ExperimentalMaterial3Api::class)
            
package com.swipecleaner.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.swipecleaner.app.domain.BucketFolder
import com.swipecleaner.app.domain.SwipeDirection
import com.swipecleaner.app.ui.trash.rememberTrashOrchestrator
import java.util.Locale

/**
 * Punto de entrada del deck: carga las fotos de la carpeta y muestra
 * la tarjeta superior con su thumbnail. La lógica de gestos (swipe)
 * se añade sobre este mismo Composable en el paso 6.
 */
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

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(folder.name) },
            navigationIcon = {
                TextButton(onClick = onBack) { Text("← Carpetas") }
            },
            actions = {
                TextButton(
                    onClick = { viewModel.undo() },
                    enabled = loaded?.history?.isNotEmpty() == true
                ) { Text("Deshacer") }
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
                        "${formatSize(loaded.freedBytes)} marcados para papelera",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
            when (val s = state) {
                is PhotoDeckState.Loading -> CircularProgressIndicator()

                is PhotoDeckState.Error -> Text("Error: ${s.message}")

                is PhotoDeckState.Loaded -> {
                    val photos = s.photos
                    val index = s.currentIndex

                    if (index >= photos.size) {
                        if (photos.isEmpty()) {
                            Text("Esta carpeta no tiene fotos")
                        } else {
                            DeckSummary(
                                state = s,
                                onConfirm = { trashAction(s.trashCandidates) }
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
                            // Carta de fondo (siguiente foto), sin gesto
                            if (next != null) {
                                Card(modifier = Modifier.fillMaxSize()) {
                                    Image(
                                        painter = rememberAsyncImagePainter(next.uri),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }

                            // Carta superior: arrastrable
                            SwipeableCard(
                                onSwiped = { decision ->
                                    val direction = if (decision == SwipeDecision.LEFT)
                                        SwipeDirection.LEFT else SwipeDirection.RIGHT
                                    viewModel.onSwipe(direction)
                                }
                            ) { progressPx ->
                                Card(modifier = Modifier.fillMaxSize()) {
                                    Box {
                                        Image(
                                            painter = rememberAsyncImagePainter(top.uri),
                                            contentDescription = top.displayName,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                        val leftAlpha = (-progressPx / 300f).coerceIn(0f, 1f)
                                        val rightAlpha = (progressPx / 300f).coerceIn(0f, 1f)
                                        Text(
                                            "PAPELERA",
                                            color = androidx.compose.ui.graphics.Color(0xFFE2685F).copy(alpha = leftAlpha),
                                            modifier = Modifier
                                                .align(Alignment.TopStart)
                                                .padding(16.dp)
                                        )
                                        Text(
                                            "CONSERVAR",
                                            color = androidx.compose.ui.graphics.Color(0xFF7FAE6A).copy(alpha = rightAlpha),
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            } // fin Box
        } // fin Column
    }
}

private fun formatSize(bytes: Long): String {
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1024) String.format(Locale.getDefault(), "%.1f GB", mb / 1024)
    else String.format(Locale.getDefault(), "%.1f MB", mb)
}

/**
 * Pantalla al terminar el deck: si ya hubo confirmación previa muestra el
 * resultado; si no, muestra el resumen de candidatos y el botón que dispara
 * el diálogo de sistema para enviarlos a la papelera.
 */
@Composable
private fun DeckSummary(state: PhotoDeckState.Loaded, onConfirm: () -> Unit) {
    val confirmedCount = state.lastConfirmedCount

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (confirmedCount != null) {
            Text("Listo ✅", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text("$confirmedCount fotos enviadas a la papelera del sistema")
            Spacer(Modifier.height(4.dp))
            Text(
                "Se eliminarán automáticamente en 30 días",
                style = MaterialTheme.typography.bodySmall
            )
        } else if (state.trashCandidates.isEmpty()) {
            Text("Carpeta revisada ✅")
            Spacer(Modifier.height(4.dp))
            Text("No marcaste ninguna foto para borrar", style = MaterialTheme.typography.bodySmall)
        } else {
            Text("Carpeta revisada", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                "${state.trashCandidates.size} fotos marcadas · ${formatSize(state.freedBytes)}",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onConfirm) {
                Text("Enviar a la papelera")
            }
        }
    }
}
