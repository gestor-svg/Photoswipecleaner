@file:OptIn(ExperimentalMaterial3Api::class)

package com.swipecleaner.app.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.swipecleaner.app.data.UpdateCheckResult
import com.swipecleaner.app.domain.BucketFolder
import com.swipecleaner.app.ui.UpdateBanner
import java.util.Locale

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
        TopAppBar(
            title = { Text("Elige una carpeta") },
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
                                    "Descárgala aquí: https://github.com/gestor-svg/PhotoSwipeCleaner/releases/latest"
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
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(s.folders) { folder ->
                                    FolderRow(folder = folder, onClick = { onFolderSelected(folder) })
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
private fun FolderRow(folder: BucketFolder, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(folder.name, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(2.dp))
                Text(
                    "${folder.photoCount} fotos · ${formatSize(folder.totalSizeBytes)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

private fun formatSize(bytes: Long): String {
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1024) String.format(Locale.getDefault(), "%.1f GB", mb / 1024)
    else String.format(Locale.getDefault(), "%.1f MB", mb)
}
