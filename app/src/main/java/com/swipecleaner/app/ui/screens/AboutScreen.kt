@file:OptIn(ExperimentalMaterial3Api::class)

package com.swipecleaner.app.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.swipecleaner.app.BuildConfig
import com.swipecleaner.app.data.SwipeLimitManager
import com.swipecleaner.app.data.UnlockResult
import com.swipecleaner.app.ui.theme.GradientButton
import com.swipecleaner.app.ui.theme.HumorPhrases
import com.swipecleaner.app.ui.theme.PsColor
import com.swipecleaner.app.ui.theme.PsRadius
import com.swipecleaner.app.ui.theme.PsTextStyle

private data class PermissionInfo(val icon: String, val name: String, val reason: String)

private val permissionsList = listOf(
    PermissionInfo(
        "🖼️",
        "Acceso a fotos y videos",
        "Necesario para mostrarte tu galería y aplicar los cambios que decides al deslizar."
    ),
    PermissionInfo(
        "🌐",
        "Acceso a internet",
        "Solo para validar donaciones y códigos de desbloqueo. No enviamos tus fotos ni tu actividad a ningún servidor."
    )
)

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val swipeLimitManager = remember { SwipeLimitManager(context) }

    var isMasterUnlocked by remember { mutableStateOf(swipeLimitManager.isMasterUnlocked()) }
    var tier1Active by remember { mutableStateOf(swipeLimitManager.isTier1Active()) }
    var tier1Days by remember { mutableStateOf(swipeLimitManager.tier1DaysRemaining()) }
    var unlockCode by remember { mutableStateOf("") }
    var unlockMessage by remember { mutableStateOf<String?>(null) }
    var phrase by remember { mutableStateOf(HumorPhrases.random()) }

    fun handleUnlock() {
        when (val result = swipeLimitManager.tryUnlock(unlockCode)) {
            is UnlockResult.MasterUnlocked -> {
                isMasterUnlocked = true
                unlockMessage = "¡Desbloqueado permanentemente!"
            }
            is UnlockResult.Tier1Unlocked -> {
                tier1Active = true
                tier1Days = swipeLimitManager.tier1DaysRemaining()
                unlockMessage = "¡Activado! ${result.days} días de Tier 1."
            }
            UnlockResult.Invalid -> unlockMessage = "Código incorrecto."
        }
        unlockCode = ""
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Acerca de") },
            navigationIcon = {
                TextButton(onClick = onBack) { Text("← Atrás") }
            }
        )
    }) { padding ->
        LazyColumnWrapper(padding) {

            // Logo + nombre + subtítulo
            Box(
                modifier = Modifier.size(56.dp).background(PsColor.GradBrand, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("🧹", style = MaterialTheme.typography.titleLarge)
            }
            Spacer(Modifier.height(12.dp))
            Text("PhotoSwipeCleaner", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(4.dp))
            Text(
                "Limpieza de galería, sin trucos.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(28.dp))

            // Permisos
            Text("Permisos", style = MaterialTheme.typography.titleSmall, modifier = Modifier.align(Alignment.Start))
            Spacer(Modifier.height(10.dp))
            permissionsList.forEach { permission ->
                PermissionCard(permission)
                Spacer(Modifier.height(10.dp))
            }

            Divider()
            Spacer(Modifier.height(24.dp))

            // Humor + donar
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
            Spacer(Modifier.height(8.dp))
            GradientButton(
                text = "Donar y desbloquear 90 días",
                gradient = PsColor.GradDonate,
                onClick = {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        android.net.Uri.parse("https://gestor-svg.github.io/Photoswipecleaner/donar.html")
                    )
                    context.startActivity(intent)
                }
            )
            Spacer(Modifier.height(24.dp))

            Divider()
            Spacer(Modifier.height(24.dp))

            // Código de desbloqueo
            Text("Código de desbloqueo", style = MaterialTheme.typography.titleSmall, modifier = Modifier.align(Alignment.Start))
            Spacer(Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(PsRadius.Card),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    when {
                        isMasterUnlocked -> {
                            UnlockedBadge("Código aplicado — acceso ilimitado permanente ✨")
                        }
                        tier1Active -> {
                            UnlockedBadge("Código aplicado — te quedan $tier1Days días de acceso ilimitado")
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "¿Quieres renovar antes de que venza?",
                                style = PsTextStyle.Caption,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            UnlockCodeField(
                                code = unlockCode,
                                onCodeChange = { unlockCode = it; unlockMessage = null },
                                message = unlockMessage,
                                onSubmit = ::handleUnlock
                            )
                        }
                        else -> {
                            UnlockCodeField(
                                code = unlockCode,
                                onCodeChange = { unlockCode = it; unlockMessage = null },
                                message = unlockMessage,
                                onSubmit = ::handleUnlock
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))

            Divider()
            Spacer(Modifier.height(24.dp))

            // Compartir
            OutlinedButton(
                onClick = {
                    val message = "Prueba PhotoSwipeCleaner, limpia tu galería con swipes 🧹📱\n" +
                        "Descárgala aquí: https://gestor-svg.github.io/Photoswipecleaner/descarga.html"
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, message)
                    }
                    context.startActivity(Intent.createChooser(intent, "Compartir PhotoSwipeCleaner"))
                },
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth(0.8f).height(52.dp)
            ) {
                Text("🔗  Compartir")
            }

            Spacer(Modifier.height(28.dp))
            Text(
                "v${BuildConfig.VERSION_NAME}",
                style = PsTextStyle.Caption,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

/**
 * Columna centrada con scroll, reemplaza el LazyColumn anterior — la
 * pantalla ya no necesita listas dinámicas, todo es contenido fijo.
 */
@Composable
private fun LazyColumnWrapper(padding: PaddingValues, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content
    )
}

@Composable
private fun PermissionCard(permission: PermissionInfo) {
    Surface(
        shape = RoundedCornerShape(PsRadius.Card),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(permission.icon, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(permission.name, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(2.dp))
                Text(
                    permission.reason,
                    style = PsTextStyle.Caption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun UnlockedBadge(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(24.dp).background(PsColor.Green, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("✓", color = androidx.compose.ui.graphics.Color.White, style = PsTextStyle.Caption)
        }
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun UnlockCodeField(
    code: String,
    onCodeChange: (String) -> Unit,
    message: String?,
    onSubmit: () -> Unit
) {
    OutlinedTextField(
        value = code,
        onValueChange = onCodeChange,
        label = { Text("Ej. AMIGO90") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(8.dp))
    Button(onClick = onSubmit, modifier = Modifier.fillMaxWidth()) {
        Text("Desbloquear")
    }
    message?.let {
        Spacer(Modifier.height(4.dp))
        Text(it, style = PsTextStyle.Caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
