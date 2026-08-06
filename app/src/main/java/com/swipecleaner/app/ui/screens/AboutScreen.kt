@file:OptIn(ExperimentalMaterial3Api::class)

package com.swipecleaner.app.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.swipecleaner.app.data.SwipeLimitManager
import com.swipecleaner.app.data.UnlockResult

private data class PermissionInfo(val name: String, val reason: String)

private val permissionsList = listOf(
    PermissionInfo(
        "Fotos y videos del dispositivo",
        "Para mostrarte tus fotos en la pantalla de swipe. Nunca se suben a ningún servidor — todo el procesamiento ocurre en tu teléfono."
    ),
    PermissionInfo(
        "Acceso a Internet",
        "Solo para revisar si hay una versión más nueva de la app publicada en GitHub. No se envía ningún dato personal ni fotos."
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

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Acerca de") },
            navigationIcon = {
                TextButton(onClick = onBack) { Text("← Atrás") }
            }
        )
    }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                Text(
                    "PhotoSwipeCleaner",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Limpia tu galería deslizando: izquierda para borrar, derecha para conservar.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(24.dp))
            }

            item {
                Text("Privacidad", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Esta app no tiene rastreadores, no recolecta datos, y nunca sube tus fotos a ningún servidor. Todo el código es público y auditable.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(16.dp))
            }

            item {
                Text("Permisos que usa esta app", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
            }

            items(permissionsList) { permission ->
                Column(modifier = Modifier.padding(bottom = 12.dp)) {
                    Text(permission.name, fontWeight = FontWeight.Medium)
                    Text(permission.reason, style = MaterialTheme.typography.bodySmall)
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                Button(onClick = {
                    val message = "Prueba PhotoSwipeCleaner, limpia tu galería con swipes 🧹📱\n" +
                        "Descárgala aquí: https://gestor-svg.github.io/Photoswipecleaner/descarga.html"
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, message)
                    }
                    context.startActivity(Intent.createChooser(intent, "Compartir PhotoSwipeCleaner"))
                }) {
                    Text("Compartir esta app")
                }
                Spacer(Modifier.height(24.dp))
            }

            item {
                Text("Código de desbloqueo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))

                when {
                    isMasterUnlocked -> {
                        Text("✅ Swipes ilimitados activos en este dispositivo (permanente)", style = MaterialTheme.typography.bodyMedium)
                    }
                    tier1Active -> {
                        Text("✅ Tier 1 activo — te quedan $tier1Days días", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "¿Quieres renovar antes de que venza? Canjea un código nuevo cuando quieras, reemplaza la fecha por otros 90 días.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(Modifier.height(8.dp))
                        UnlockCodeField(
                            code = unlockCode,
                            onCodeChange = { unlockCode = it; unlockMessage = null },
                            message = unlockMessage,
                            onSubmit = {
                                when (val result = swipeLimitManager.tryUnlock(unlockCode)) {
                                    is UnlockResult.MasterUnlocked -> {
                                        isMasterUnlocked = true
                                        unlockMessage = "¡Desbloqueado permanentemente!"
                                    }
                                    is UnlockResult.Tier1Unlocked -> {
                                        tier1Active = true
                                        tier1Days = swipeLimitManager.tier1DaysRemaining()
                                        unlockMessage = "¡Renovado! ${result.days} días de Tier 1."
                                    }
                                    UnlockResult.Invalid -> unlockMessage = "Código incorrecto."
                                }
                                unlockCode = ""
                            }
                        )
                    }
                    else -> {
                        Text(
                            "¿Tienes un código de desbloqueo? Actívalo aquí.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(Modifier.height(8.dp))
                        UnlockCodeField(
                            code = unlockCode,
                            onCodeChange = { unlockCode = it; unlockMessage = null },
                            message = unlockMessage,
                            onSubmit = {
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
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
            }

            item {
                Text("Código fuente", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("github.com/gestor-svg/PhotoSwipeCleaner", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(24.dp))
            }

            item {
                Text("Próximamente", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Bóveda privada, video swipes, y más funciones conforme se vayan agregando.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
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
        label = { Text("Código") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(8.dp))
    Button(onClick = onSubmit) {
        Text("Desbloquear")
    }
    message?.let {
        Spacer(Modifier.height(4.dp))
        Text(it, style = MaterialTheme.typography.bodySmall)
    }
}
