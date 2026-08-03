package com.swipecleaner.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.swipecleaner.app.data.UpdateCheckResult

/**
 * Banner discreto (no bloquea el uso de la app) que se muestra en la
 * pantalla de selección de carpetas cuando hay una versión más nueva
 * publicada en GitHub Releases.
 */
@Composable
fun UpdateBanner(result: UpdateCheckResult.UpdateAvailable, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Nueva versión disponible", style = MaterialTheme.typography.bodyMedium)
                Text("v${result.latestVersion}", style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(result.downloadUrl))
                context.startActivity(intent)
            }) {
                Text("Descargar")
            }
        }
    }
}
