@file:OptIn(ExperimentalMaterial3Api::class)

package com.swipecleaner.app.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.lifecycle.viewmodel.compose.viewModel
import com.swipecleaner.app.data.UpdateCheckResult
import com.swipecleaner.app.domain.BucketFolder
import com.swipecleaner.app.ui.UpdateBanner
import com.swipecleaner.app.ui.theme.PsColor
import com.swipecleaner.app.ui.theme.PsRadius
import com.swipecleaner.app.ui.theme.PsTextStyle
import java.util.Locale

// Colores de acento que rotan entre carpetas, en el mismo orden que el resto de la marca.
private val accentCycle = listOf(PsColor.Blue, PsColor.Green, PsColor.Orange, PsColor.Yellow)

@Composable
fun FolderSelectionScreen(
    onFolderSelected: (BucketFolder) -> Unit,
    onAboutClick: () -> Unit,
    viewModel: FolderListViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val updateState by viewModel.updateState.collectAsState()
    val context = LocalContext.current

    var menuExpanded by remember { mutableStateOf(false) }

    Scaffold(topBar = {
        CenterAlignedTopAppBar(
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "PHOTOSWIPECLEANER",
                        style = PsTextStyle.OverlineLabel.copy(letterSpacing = 0.14.em),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Tus carpetas",
                        style = PsTextStyle.ScreenTitle,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            },
            actions = {
                Box {
                    TextButton(onClick = { menuExpanded = true }) {
                        Text("⋮", style = MaterialTheme.typography.titleLarge)
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Acerca de") },
                            onClick = {
                                menuExpanded = false
                                onAboutClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Donar 💛") },
                            onClick = {
                                menuExpanded = false
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    android.net.Uri.parse("https://gestor-svg.github.io/Photoswipecleaner/donar.html")
                                )
                                context.startActivity(intent)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Compartir esta app") },
                            onClick = {
                                menuExpanded = false
                                val message = "Prueba PhotoSwipeCleaner, limpia tu galería con swipes 🧹📱\n" +
                                    "Descárgala aquí: https://gestor-svg.github.io/Photoswipecleaner/descarga.html"
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, message)
                                }
                                context.startActivity(Intent.createChooser(intent, "Compartir PhotoSwipeCleaner"))
                            }
                        )
                    }
                }
            }
        )
    }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val available = updateState as? UpdateCheckResult.UpdateAvailable
            if (available != null) {
                UpdateBanner(result = available)
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (val s = state) {
                    is FolderListState.Loading -> {
                        CircularProgressIndicator(Modifier.align(Alignment.Center))
                    }

                    is FolderListState.Error -> {
                        Column(
                            modifier = Modifier.align(Alignment.Center).padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("No se pudo leer la galería")
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = { viewModel.loadFolders() }) { Text("Reintentar") }
                        }
                    }

                    is FolderListState.Loaded -> {
                        if (s.folders.isEmpty()) {
                            Text(
                                "No se encontraron fotos en el dispositivo",
                                modifier = Modifier.align(Alignment.Center)
                            )
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                itemsIndexed(s.folders) { index, folder ->
                                    FolderCard(
                                        folder = folder,
                                        accent = accentCycle[index % accentCycle.size],
                                        onClick = { onFolderSelected(folder) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderCard(folder: BucketFolder, accent: Color, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(PsRadius.Card),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // Barra de acento superior — hereda las esquinas redondeadas del Card
            // porque Card recorta su contenido al mismo shape por default.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(accent)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    folder.name,
                    style = PsTextStyle.CardTitle,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatChip(
                        text = "${folder.photoCount} fotos",
                        containerColor = accent.copy(alpha = 0.12f),
                        borderColor = accent.copy(alpha = 0.33f),
                        contentColor = accent
                    )
                    StatChip(
                        text = formatSize(folder.totalSizeBytes),
                        containerColor = Color.Transparent,
                        borderColor = MaterialTheme.colorScheme.outline,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun StatChip(text: String, containerColor: Color, borderColor: Color, contentColor: Color) {
    Box(
        modifier = Modifier
            .background(containerColor, CircleShape)
            .border(1.dp, borderColor, CircleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text, style = PsTextStyle.Caption, color = contentColor)
    }
}

private fun formatSize(bytes: Long): String {
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1024) String.format(Locale.getDefault(), "%.1f GB", mb / 1024)
    else String.format(Locale.getDefault(), "%.1f MB", mb)
}
