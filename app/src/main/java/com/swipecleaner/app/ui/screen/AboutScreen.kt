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

    var isUnlocked by remember { mutableStateOf(swipeLimitManager.isUnlocked()) }
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
                        "Descárgala aquí: https://github.com/gestor-svg/PhotoSwipeCleaner/releases/latest"
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
                if (isUnlocked) {
                    Text("✅ Swipes ilimitados activos en este dispositivo", style = MaterialTheme.typography.bodyMedium)
                } else {
                    Text(
                        "¿Tienes un código de desbloqueo? Actívalo aquí.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = unlockCode,
                        onValueChange = {
                            unlockCode = it
                            unlockMessage = null
                        },
                        label = { Text("Código") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = {
                        if (swipeLimitManager.tryUnlock(unlockCode)) {
                            isUnlocked = true
                            unlockMessage = "¡Desbloqueado! Swipes ilimitados activos."
                        } else {
                            unlockMessage = "Código incorrecto."
                        }
                        unlockCode = ""
                    }) {
                        Text("Desbloquear")
                    }
                    unlockMessage?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall)
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
                    "Aquí vivirán las opciones de donación y funciones extra de la app conforme se vayan agregando.",
                    style = MaterialTheme.typography.bodySmall
                )
                // TODO: sección de Donar / tiers cuando se implemente Fase 5.
                // TODO: acceso a bóveda / funciones premium cuando se implemente Fase 4/8/9/10.
            }
        }
    }
}
