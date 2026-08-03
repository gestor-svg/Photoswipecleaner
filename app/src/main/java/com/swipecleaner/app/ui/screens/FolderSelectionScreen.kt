@file:OptIn(ExperimentalMaterial3Api::class)
package com.swipecleaner.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.swipecleaner.app.domain.BucketFolder
import java.util.Locale

@Composable
fun FolderSelectionScreen(
    onFolderSelected: (BucketFolder) -> Unit,
    viewModel: FolderListViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(topBar = {
        TopAppBar(title = { Text("Elige una carpeta") })
    }) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
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
