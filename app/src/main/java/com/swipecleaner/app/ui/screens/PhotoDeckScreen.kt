@file:OptIn(ExperimentalMaterial3Api::class)

package com.swipecleaner.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import com.swipecleaner.app.data.SwipeLimitManager
import com.swipecleaner.app.domain.BucketFolder
import com.swipecleaner.app.domain.PhotoItem
import com.swipecleaner.app.domain.SwipeDirection
import com.swipecleaner.app.ui.theme.GradientButton
import com.swipecleaner.app.ui.theme.HumorPhrases
import com.swipecleaner.app.ui.theme.PsColor
import com.swipecleaner.app.ui.theme.PsRadius
import com.swipecleaner.app.ui.theme.PsTextStyle
import com.swipecleaner.app.ui.trash.rememberTrashOrchestrator
import kotlinx.coroutines.delay
import java.util.Locale

private data class ResultSnackbarData(val count: Int, val freedBytes: Long)

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
    val context = LocalContext.current

    // Snapshot del lote que se está confirmando — se captura al momento de
    // confirmar porque el estado (freedBytes/history) se limpia apenas
    // termina el envío, y el snackbar necesita esos números después.
    var pendingConfirmSnapshot by remember { mutableStateOf<ResultSnackbarData?>(null) }
    var resultSnackbar by remember { mutableStateOf<ResultSnackbarData?>(null) }

    val trashAction = rememberTrashOrchestrator(
        onFinished = { deletedCount ->
            viewModel.onTrashConfirmed(deletedCount)
            pendingConfirmSnapshot?.let { snap ->
                resultSnackbar = ResultSnackbarData(deletedCount, snap.freedBytes)
            }
            pendingConfirmSnapshot = null
        }
    )

    var pendingCorruptedBatch by remember { mutableStateOf<List<PhotoItem>>(emptyList()) }
    val corruptedTrashAction = rememberTrashOrchestrator(
        onFinished = { deletedCount ->
            viewModel.onCorruptedTrashConfirmed(pendingCorruptedBatch, deletedCount)
            pendingCorruptedBatch = emptyList()
        }
    )

    resultSnackbar?.let { data ->
        LaunchedEffect(data) {
            delay(3000)
            resultSnackbar = null
        }
    }

    // Bottom sheet de confirmación (2b) — reemplaza el envío directo de antes.
    var showConfirmSheet by remember { mutableStateOf(false) }

    fun requestConfirm() {
        if (loaded != null && loaded.trashCandidates.isNotEmpty()) {
            showConfirmSheet = true
        }
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
                    loaded?.let {
                        pendingConfirmSnapshot = ResultSnackbarData(it.trashCandidates.size, it.freedBytes)
                        trashAction(it.trashCandidates)
                    }
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

    if (showConfirmSheet && loaded != null) {
        ModalBottomSheet(
            onDismissRequest = { showConfirmSheet = false },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(topStart = PsRadius.BottomSheetTop, topEnd = PsRadius.BottomSheetTop)
        ) {
            ConfirmDeleteSheetContent(
                count = loaded.trashCandidates.size,
                freedBytes = loaded.freedBytes,
                onCancel = { showConfirmSheet = false },
                onConfirm = {
                    pendingConfirmSnapshot = ResultSnackbarData(loaded.trashCandidates.size, loaded.freedBytes)
                    trashAction(loaded.trashCandidates)
                    showConfirmSheet = false
                }
            )
        }
    }

    var manualSwipeCounter by remember { mutableStateOf(0) }
    var manualSwipe by remember { mutableStateOf<ManualSwipeRequest?>(null) }

    var corruptedBannerDismissed by remember(loaded?.corruptedPhotos) { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(topBar = {
            TopAppBar(
                title = { Text(folder.name) },
                navigationIcon = {
                    TextButton(onClick = { requestExit() }) { Text("← Carpetas") }
                },
                actions = {
                    TextButton(
                        onClick = { requestConfirm() },
                        enabled = loaded?.trashCandidates?.isNotEmpty() == true
                    ) { Text("Confirmar") }
                }
            )
        }) { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {

                if (loaded != null && loaded.currentIndex < loaded.photos.size) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "${loaded.currentIndex + 1} / ${loaded.photos.size} · ${folder.name}",
                            style = PsTextStyle.DeckHeader,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "${formatSize(loaded.freedBytes)} marcados para papelera" +
                                if (loaded.confirmedCount > 0) " · ${loaded.confirmedCount} ya enviadas" else "",
                            style = PsTextStyle.Caption,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
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
                                    onSendMarked = {
                                        pendingConfirmSnapshot = ResultSnackbarData(s.trashCandidates.size, s.freedBytes)
                                        trashAction(s.trashCandidates)
                                    },
                                    onBack = onBack
                                )
                            } else if (index >= photos.size) {
                                if (photos.isEmpty()) {
                                    Text("Esta carpeta no tiene fotos")
                                } else {
                                    DeckSummary(
                                        state = s,
                                        onConfirm = { requestConfirm() }
                                    )
                                }
                            } else {
                                val top = photos[index]
                                val next = photos.getOrNull(index + 1)

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.78f)
                                        .aspectRatio(0.9f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (next != null) {
                                        Card(
                                            shape = RoundedCornerShape(PsRadius.PhotoCard),
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .graphicsLayer {
                                                    rotationZ = -3f
                                                    scaleX = 0.96f
                                                    scaleY = 0.96f
                                                    translationY = 10.dp.toPx()
                                                    alpha = 0.55f
                                                }
                                        ) {
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
                                        val rightAlpha = if (progressPx > 0) (progressPx / 120f).coerceIn(0f, 1f) else 0f
                                        val leftAlpha = if (progressPx < 0) (-progressPx / 120f).coerceIn(0f, 1f) else 0f
                                        val dragAlpha = (kotlin.math.abs(progressPx) / 130f).coerceIn(0f, 1f)
                                        val ringColor = when {
                                            progressPx > 0 -> PsColor.Green
                                            progressPx < 0 -> PsColor.Orange
                                            else -> Color.Transparent
                                        }
                                        val ringWidth = lerp(0.dp, 3.dp, dragAlpha)

                                        Card(
                                            shape = RoundedCornerShape(PsRadius.PhotoCard),
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .border(ringWidth, ringColor.copy(alpha = dragAlpha), RoundedCornerShape(PsRadius.PhotoCard))
                                        ) {
                                            Box {
                                                PhotoCardImage(uri = top.uri, contentDescription = top.displayName)
                                                SwipeBadge(
                                                    symbol = "✕",
                                                    color = PsColor.Orange,
                                                    alpha = leftAlpha,
                                                    modifier = Modifier.align(Alignment.TopStart).padding(14.dp)
                                                )
                                                SwipeBadge(
                                                    symbol = "✓",
                                                    color = PsColor.Green,
                                                    alpha = rightAlpha,
                                                    modifier = Modifier.align(Alignment.TopEnd).padding(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (loaded != null && loaded.remainingSwipesToday > 0 && loaded.currentIndex < loaded.photos.size) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            UndoCircleButton(
                                enabled = loaded.history.isNotEmpty(),
                                onClick = { viewModel.undo() }
                            )
                            ActionPillButton(
                                symbol = "✕",
                                label = "Borrar",
                                tint = PsColor.Orange,
                                onClick = {
                                    manualSwipeCounter++
                                    manualSwipe = ManualSwipeRequest(SwipeDecision.LEFT, manualSwipeCounter)
                                }
                            )
                            ActionPillButton(
                                symbol = "✓",
                                label = "Conservar",
                                tint = PsColor.Green,
                                onClick = {
                                    manualSwipeCounter++
                                    manualSwipe = ManualSwipeRequest(SwipeDecision.RIGHT, manualSwipeCounter)
                                }
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Deshacer última acción",
                            style = PsTextStyle.Caption,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.56f)
                        )
                    }
                }
            }
        }

        resultSnackbar?.let { data ->
            ResultSnackbar(
                data = data,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun ConfirmDeleteSheetContent(count: Int, freedBytes: Long, onCancel: () -> Unit, onConfirm: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 28.dp, top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "¿Borrar $count foto${if (count == 1) "" else "s"}?",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Se liberarán ${formatSize(freedBytes)} de espacio. Esta acción no se puede deshacer.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) { Text("Cancelar") }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                    .background(PsColor.GradDelete)
                    .clickable(onClick = onConfirm),
                contentAlignment = Alignment.Center
            ) {
                Text("Confirmar", color = Color.White, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun ResultSnackbar(data: ResultSnackbarData, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, PsColor.Green.copy(alpha = 0.3f)),
        shadowElevation = 12.dp,
        modifier = modifier.padding(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(32.dp).background(PsColor.Green, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("✓", color = Color.White, style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("¡Liberaste ${formatSize(data.freedBytes)}!", style = MaterialTheme.typography.labelLarge)
                Text(
                    "${data.count} foto${if (data.count == 1) "" else "s"} menos en tu galería",
                    style = PsTextStyle.Caption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SwipeBadge(symbol: String, color: Color, alpha: Float, modifier: Modifier = Modifier) {
    if (alpha <= 0f) return
    Box(
        modifier = modifier
            .size(38.dp)
            .background(color.copy(alpha = alpha), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(symbol, color = Color.White.copy(alpha = alpha), style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun UndoCircleButton(enabled: Boolean, onClick: () -> Unit) {
    val borderColor = if (enabled) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    val contentColor = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
    Box(
        modifier = Modifier
            .size(52.dp)
            .border(BorderStroke(1.dp, borderColor), CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text("↩", color = contentColor, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun ActionPillButton(symbol: String, label: String, tint: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = 10.dp,
        modifier = Modifier.size(width = 88.dp, height = 64.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(symbol, color = tint, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(2.dp))
            Text(label, color = MaterialTheme.colorScheme.onSurface, style = PsTextStyle.Caption)
        }
    }
}

/**
 * Pantalla de límite diario alcanzado (2d), con frase de humor y conteo
 * regresivo en vivo contra la hora exacta de reset (23:59:59 de hoy).
 */
@Composable
private fun LimitReachedView(
    state: PhotoDeckState.Loaded,
    onSendMarked: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val swipeLimitManager = remember { SwipeLimitManager(context) }
    var phrase by remember { mutableStateOf(HumorPhrases.random()) }
    var remainingMillis by remember {
        mutableStateOf((swipeLimitManager.resetAtMillis() - System.currentTimeMillis()).coerceAtLeast(0))
    }

    LaunchedEffect(Unit) {
        while (true) {
            remainingMillis = (swipeLimitManager.resetAtMillis() - System.currentTimeMillis()).coerceAtLeast(0)
            delay(60_000)
        }
    }
    val totalMinutes = (remainingMillis / 60_000).toInt()
    val hh = totalMinutes / 60
    val mm = totalMinutes % 60

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(24.dp).fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(Brush.linearGradient(listOf(PsColor.Yellow, PsColor.Orange)), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("⏰", style = MaterialTheme.typography.titleLarge)
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "Se acabaron tus swipes de hoy",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
        Text(
            phrase,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 280.dp)
        )
        TextButton(onClick = { phrase = HumorPhrases.random(excluding = phrase) }) {
            Text("↻ otra frase")
        }

        if (state.trashCandidates.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Button(onClick = onSendMarked) { Text("Enviar lo marcado a la papelera") }
        }

        Spacer(Modifier.height(16.dp))
        GradientButton(
            text = "Donar y desbloquear 90 días",
            gradient = PsColor.GradDonate,
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://gestor-svg.github.io/Photoswipecleaner/donar.html"))
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth(0.9f)
        )
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(0.9f).height(52.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("⏱ Esperar ${hh}h ${mm}m")
        }
    }
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
