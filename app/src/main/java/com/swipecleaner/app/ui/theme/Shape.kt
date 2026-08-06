package com.swipecleaner.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Radios sueltos, para usar donde `Shapes` de Material3 no encaja 1:1. */
object PsRadius {
    val Button = 16.dp
    val Card = 20.dp          // tarjetas / chips de carpeta (rango 20–24dp, usamos 20)
    val PhotoCard = 24.dp     // tarjeta de foto en el swipe
    val BottomSheetTop = 28.dp
    val Pill = 100.dp         // chips/píldoras (100% = totalmente redondeado)
}

val PsShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(PsRadius.Button),
    medium = RoundedCornerShape(PsRadius.Card),
    large = RoundedCornerShape(PsRadius.PhotoCard),
    extraLarge = RoundedCornerShape(PsRadius.BottomSheetTop)
)
