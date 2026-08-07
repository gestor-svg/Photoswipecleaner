package com.swipecleaner.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.swipecleaner.app.data.UpdateCheckResult
import com.swipecleaner.app.ui.theme.PsColor

/**
 * Banner de actualización disponible. A propósito NO es discreto — todo el
 * bloque es tocable, con degradado de marca e ícono grande, para que
 * destaque sobre la lista de carpetas en vez de perderse como una línea
 * de texto más.
 */
@Composable
fun UpdateBanner(result: UpdateCheckResult.UpdateAvailable, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(PsColor.GradBrand)
            .clickable {
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://gestor-svg.github.io/Photoswipecleaner/descarga.html")
                )
                context.startActivity(intent)
            }
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("⬇️", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Nueva versión disponible",
                style = MaterialTheme.typography.titleSmall,
                color = Color.White
            )
            Text(
                "v${result.latestVersion} · Toca para descargar",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.85f)
            )
        }
        Text("→", style = MaterialTheme.typography.titleLarge, color = Color.White)
    }
}
