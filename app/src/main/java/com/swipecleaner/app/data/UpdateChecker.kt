package com.swipecleaner.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Resultado de comparar la versión instalada contra el último Release
 * publicado en GitHub. Usa la API pública de GitHub (api.github.com),
 * de solo lectura — no requiere backend propio.
 */
sealed interface UpdateCheckResult {
    data class UpdateAvailable(val latestVersion: String, val downloadUrl: String) : UpdateCheckResult
    data object UpToDate : UpdateCheckResult
    data object CheckFailed : UpdateCheckResult
}

class UpdateChecker(
    private val owner: String = "gestor-svg",
    private val repo: String = "PhotoSwipeCleaner"
) {
    /**
     * Consulta el último Release público del repo y lo compara contra
     * [currentVersionName] (BuildConfig.VERSION_NAME). Nunca lanza excepción
     * hacia afuera: cualquier fallo de red o parseo se traduce en
     * CheckFailed para no bloquear el uso normal de la app.
     */
    suspend fun checkForUpdate(currentVersionName: String): UpdateCheckResult =
        withContext(Dispatchers.IO) {
            try {
                val url = URL("https://api.github.com/repos/$owner/$repo/releases/latest")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/vnd.github+json")
                connection.connectTimeout = 8_000
                connection.readTimeout = 8_000

                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    return@withContext UpdateCheckResult.CheckFailed
                }

                val body = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)

                val tagName = json.optString("tag_name").removePrefix("v")
                if (tagName.isBlank()) return@withContext UpdateCheckResult.CheckFailed

                val assets = json.optJSONArray("assets")
                var apkUrl: String? = null
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name")
                        if (name.endsWith(".apk")) {
                            apkUrl = asset.optString("browser_download_url")
                            break
                        }
                    }
                }
                // Si no hay .apk adjunto como asset, manda a la página del Release.
                val downloadUrl = apkUrl ?: json.optString("html_url")

                if (isNewer(tagName, currentVersionName)) {
                    UpdateCheckResult.UpdateAvailable(tagName, downloadUrl)
                } else {
                    UpdateCheckResult.UpToDate
                }
            } catch (e: Exception) {
                UpdateCheckResult.CheckFailed
            }
        }

    /**
     * Compara versiones tipo "1.4.0" segmento por segmento numéricamente,
     * para que "1.10.0" se reconozca correctamente como más nueva que "1.9.0"
     * (una comparación de strings simple fallaría en ese caso).
     */
    private fun isNewer(remote: String, current: String): Boolean {
        val remoteParts = remote.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = current.split(".").mapNotNull { it.toIntOrNull() }
        val maxLen = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r != c) return r > c
        }
        return false
    }
}
