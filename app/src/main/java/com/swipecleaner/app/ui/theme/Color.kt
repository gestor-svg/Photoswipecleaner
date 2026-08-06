package com.swipecleaner.app.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Tokens de color de PhotoSwipeCleaner, definidos por el rediseño (Claude
 * Design). Los acentos (azul/verde/naranja/amarillo) y los gradientes son
 * IGUALES en modo claro y oscuro — solo cambian fondo/tarjeta/texto/bordes.
 */
object PsColor {
    // Base — oscuro
    val BgDark = Color(0xFF0F1115)
    val CardDark = Color(0xFF1A1D24)
    val TextDark = Color(0xFFF5F3EF)
    val BorderDark = Color(0x24FFFFFF)   // white @ 14%
    val DividerDark = Color(0x1AFFFFFF)  // white @ 10%

    // Base — claro
    val BgLight = Color(0xFFFAF8F5)
    val CardLight = Color(0xFFFFFFFF)
    val TextLight = Color(0xFF1A1D24)
    val BorderLight = Color(0x24000000)  // black @ 14%
    val DividerLight = Color(0x1A000000) // black @ 10%

    // Acento (igual en ambos modos)
    val Blue = Color(0xFF1E88E5)
    val BlueDeep = Color(0xFF0D47A1)
    val Green = Color(0xFF7FAE6A)     // conservar / éxito
    val Orange = Color(0xFFFF7043)    // borrar
    val OrangeDeep = Color(0xFFD84315)
    val Yellow = Color(0xFFF2C94C)    // donar
    val YellowDeep = Color(0xFFF2994A)

    // Gradientes de marca
    val GradBrand = Brush.linearGradient(listOf(Blue, BlueDeep))       // logo, botones primarios azules
    val GradDelete = Brush.linearGradient(listOf(Orange, OrangeDeep))  // confirmar borrado
    val GradDonate = Brush.linearGradient(listOf(Yellow, YellowDeep))  // todos los botones de donar

    /** Texto secundario: el texto principal del modo actual, al 56% de opacidad. */
    fun subText(baseText: Color): Color = baseText.copy(alpha = 0.56f)
}
