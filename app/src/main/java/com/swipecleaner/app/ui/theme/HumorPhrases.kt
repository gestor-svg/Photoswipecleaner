package com.swipecleaner.app.ui.theme

/**
 * Frases de humor/motivación mostradas junto a los botones de donar.
 * Vive en un solo lugar para poder editarse sin tocar la UI de cada
 * pantalla que las usa.
 */
object HumorPhrases {
    val donationPool = listOf(
        "Invítanos desde una coca hasta un café y desbloquea 90 días de swipes ilimitados — el becario te lo va a agradecer.",
        "Sigue borrando las fotos de tu ex, no te detengas ahora. O dona y sigue sin límites.",
        "Miles de memes (y el contenido incómodo de esos grupos de WhatsApp) te esperan. Dona y sigue limpiando sin parar.",
        "Cada donación le paga un café al becario que programó esto solito.",
        "Tu galería te lo va a agradecer. Y el becario, más.",
        "Tu celular tiene 3 GB libres esperando. Dona y termina la limpieza hoy mismo.",
        "Esa foto borrosa del concierto de 2019 no se va a borrar sola. Ayúdanos a ayudarte.",
        "Somos más baratos que un café de Starbucks y liberamos más espacio que Marie Kondo.",
        "Ni rastreadores, ni anuncios, ni letra chiquita. Solo pedimos que nos invites algo si te sirvió.",
        "347 capturas de pantalla de memes que ya no entiendes. Dona y termina la faena.",
        "El servidor no se paga solo (todavía no aceptamos tandas, pero lo estamos pensando).",
        "Si esta app te ahorró tiempo, nosotros ahorramos en no vender tus datos. Regrésanos el favor.",
        "Dona lo que gastarías en un refresco y sigue tirando fotos duplicadas sin parar.",
        "No cobramos suscripción, no vendemos tu info, solo pedimos una feria si te cayó bien la app.",
        "Esa carpeta de 'WhatsApp' con 876 fotos no se limpia de milagro. Ayúdanos a seguir mejorando esto.",
        "Entre más dones, más rápido dejamos de ver el mensaje de 'límite diario alcanzado'.",
        "Gratis, de código abierto, sin trucos — pero el hosting sí cuesta. Un cafecito ayuda mucho.",
        "Menos fotos borrosas del perro, más espacio para las buenas. Dona y sigue.",
        "Hicimos esto para que no pagaras con tus datos. Si quieres, paga con lo que gustes y puedas.",
        "Tu 'para después' de limpiar la galería ya duró dos años. Hoy sí, con tu ayuda."
    )

    fun random(excluding: String? = null): String {
        if (donationPool.size <= 1) return donationPool.first()
        var pick: String
        do {
            pick = donationPool.random()
        } while (pick == excluding)
        return pick
    }
}
