package com.swipecleaner.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class SwipeDecision { LEFT, RIGHT }

/**
 * Cada click en los botones manuales genera una solicitud con un `token`
 * único (incremental), no solo la dirección. Así dos clicks seguidos con la
 * misma dirección (ej. ✕ luego ✕) siempre son valores distintos y siempre
 * disparan una nueva animación — corrige el bug donde el botón dejaba de
 * responder tras varios clicks rápidos (el token viejo podía quedar
 * "atorado" porque dos clicks iguales no generaban un cambio detectable).
 */
data class ManualSwipeRequest(val decision: SwipeDecision, val token: Int)

private const val SWIPE_THRESHOLD_DP = 120
private const val EXIT_DISTANCE_DP = 500

@Composable
fun SwipeableCard(
    manualTrigger: ManualSwipeRequest? = null,
    onSwiped: (SwipeDecision) -> Unit,
    content: @Composable (progressPx: Float) -> Unit
) {
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val thresholdPx = with(density) { SWIPE_THRESHOLD_DP.dp.toPx() }
    val exitPx = with(density) { EXIT_DISTANCE_DP.dp.toPx() }

    val rotation = (offsetX.value / 20f).coerceIn(-20f, 20f)

    // Se relanza en cada click nuevo (el token siempre cambia), incluso si
    // el click anterior seguía animando: cancela esa animación limpiamente
    // y arranca la nueva desde la posición actual, sin quedar atorado.
    LaunchedEffect(manualTrigger) {
        val request = manualTrigger ?: return@LaunchedEffect
        offsetX.animateTo(if (request.decision == SwipeDecision.RIGHT) exitPx else -exitPx)
        onSwiped(request.decision)
        offsetX.snapTo(0f)
    }

    Box(
        modifier = Modifier
            .offset { IntOffset(offsetX.value.roundToInt(), 0) }
            .rotate(rotation)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        scope.launch { offsetX.snapTo(offsetX.value + dragAmount.x) }
                    },
                    onDragEnd = {
                        scope.launch {
                            when {
                                offsetX.value > thresholdPx -> {
                                    offsetX.animateTo(exitPx)
                                    onSwiped(SwipeDecision.RIGHT)
                                    offsetX.snapTo(0f)
                                }
                                offsetX.value < -thresholdPx -> {
                                    offsetX.animateTo(-exitPx)
                                    onSwiped(SwipeDecision.LEFT)
                                    offsetX.snapTo(0f)
                                }
                                else -> offsetX.animateTo(0f)
                            }
                        }
                    }
                )
            }
    ) {
        content(offsetX.value)
    }
}
