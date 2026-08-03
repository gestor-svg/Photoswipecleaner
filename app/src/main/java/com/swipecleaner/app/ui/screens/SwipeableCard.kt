package com.swipecleaner.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class SwipeDecision { LEFT, RIGHT }

private const val SWIPE_THRESHOLD_DP = 120
private const val EXIT_DISTANCE_DP = 500

/**
 * Aplica gesto de arrastre horizontal a la tarjeta superior del deck.
 * Al soltar: si supera el umbral, anima la salida y llama [onSwiped];
 * si no, regresa al centro con animación de resorte.
 *
 * [content] recibe el desplazamiento actual en px, útil para overlays
 * ("PAPELERA" / "CONSERVAR") que se desvanecen según el arrastre.
 */
@Composable
fun SwipeableCard(
    onSwiped: (SwipeDecision) -> Unit,
    content: @Composable (progressPx: Float) -> Unit
) {
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val thresholdPx = with(density) { SWIPE_THRESHOLD_DP.dp.toPx() }
    val exitPx = with(density) { EXIT_DISTANCE_DP.dp.toPx() }

    val rotation = (offsetX.value / 20f).coerceIn(-20f, 20f)

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

/** Dispara la animación de salida sin arrastre (para los botones de acción). */
fun triggerProgrammaticSwipe(
    scope: kotlinx.coroutines.CoroutineScope,
    offsetX: Animatable<Float, *>,
    exitPx: Float,
    direction: SwipeDecision,
    onSwiped: (SwipeDecision) -> Unit
) {
    scope.launch {
        offsetX.animateTo(if (direction == SwipeDecision.RIGHT) exitPx else -exitPx)
        onSwiped(direction)
        offsetX.snapTo(0f)
    }
}
