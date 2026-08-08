@file:OptIn(ExperimentalMaterial3Api::class)

package com.swipecleaner.app.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.swipecleaner.app.ui.theme.GradientButton
import com.swipecleaner.app.ui.theme.PsColor
import com.swipecleaner.app.ui.theme.PsRadius

private const val TOTAL_STEPS = 3

/**
 * Tutorial de bienvenida, se muestra solo la primera vez que se abre la
 * app. Todos los gráficos están hechos en código (cajas, colores, emojis)
 * a propósito, no son capturas de pantalla reales — así no hay que
 * mantenerlas actualizadas cada vez que cambia el diseño de la app.
 */
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    var step by remember { mutableStateOf(0) }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onFinish) { Text("Saltar") }
            }

            Spacer(Modifier.weight(1f))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Crossfade(targetState = step, label = "onboarding_step") { current ->
                    when (current) {
                        0 -> SwipeStepIllustration()
                        1 -> ConfirmStepIllustration()
                        else -> UnlockCodeStepIllustration()
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Spacer(Modifier.weight(1f))
                repeat(TOTAL_STEPS) { i ->
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                if (i == step) PsColor.Blue else MaterialTheme.colorScheme.outline,
                                CircleShape
                            )
                    )
                }
                Spacer(Modifier.weight(1f))
            }

            Spacer(Modifier.weight(1f))

            GradientButton(
                text = if (step < TOTAL_STEPS - 1) "Siguiente" else "¡Entendido, empezar!",
                gradient = PsColor.GradBrand,
                onClick = {
                    if (step < TOTAL_STEPS - 1) step++ else onFinish()
                }
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SwipeStepIllustration() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.width(180.dp).height(220.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(PsRadius.PhotoCard),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("🖼️", style = MaterialTheme.typography.displayLarge)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "✕",
                    color = PsColor.Orange,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(start = 8.dp)
                )
                Text(
                    "✓",
                    color = PsColor.Green,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }
        Spacer(Modifier.height(28.dp))
        Text("Desliza para decidir", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(
            "Desliza la foto a la izquierda para marcarla como borrar, o a la derecha para conservarla. Si prefieres no deslizar, puedes usar los botones ✕ y ✓ debajo de la tarjeta.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 280.dp)
        )
    }
}

@Composable
private fun ConfirmStepIllustration() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .width(140.dp)
                .height(56.dp)
                .background(PsColor.GradDelete, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("Confirmar", color = Color.White, style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.height(28.dp))
        Text("Nada se borra sin avisar", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(
            "Las fotos que marcas para borrar no se eliminan solas. Toca \"Confirmar\" cuando quieras enviarlas — te preguntamos antes, y quedan recuperables 30 días en la papelera del sistema.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 280.dp)
        )
    }
}

@Composable
private fun UnlockCodeStepIllustration() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(88.dp).background(PsColor.GradBrand, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("🔑", style = MaterialTheme.typography.displaySmall)
        }
        Spacer(Modifier.height(28.dp))
        Text("¿Tienes un código?", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(
            "Actívalo en ⋮ → Acerca de → Código de desbloqueo. Ahí mismo puedes donar para conseguir swipes ilimitados.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 280.dp)
        )
    }
}
