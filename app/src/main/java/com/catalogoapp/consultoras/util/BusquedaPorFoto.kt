package com.catalogoapp.consultoras.util

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.tasks.await

object BusquedaPorFoto {

    data class Deteccion(val etiquetas: List<String>, val palabraClave: String?)

    private val TRADUCCIONES = listOf(
        "lipstick" to "labial",
        "lip gloss" to "labial",
        "lip" to "labial",
        "perfume" to "perfume",
        "fragrance" to "fragancia",
        "cologne" to "perfume",
        "candle" to "vela",
        "wax" to "vela",
        "foundation" to "base",
        "makeup" to "maquillaje",
        "cosmetics" to "maquillaje",
        "eye shadow" to "maquillaje",
        "mascara" to "maquillaje",
        "eyeliner" to "maquillaje",
        "face powder" to "maquillaje",
        "nail polish" to "maquillaje",
        "cream" to "crema",
        "lotion" to "crema",
        "moisturizer" to "crema",
        "skin care" to "facial",
        "serum" to "serum",
        "skin" to "facial"
    )

    suspend fun analizar(bitmap: Bitmap): Deteccion {
        val etiquetador = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
        val etiquetas = etiquetador.process(InputImage.fromBitmap(bitmap, 0)).await()
            .filter { it.confidence >= 0.5f }
            .sortedByDescending { it.confidence }
            .map { it.text }
        var palabra: String? = null
        for (etiqueta in etiquetas) {
            val coincidencia = TRADUCCIONES.firstOrNull { etiqueta.lowercase().contains(it.first) }
            if (coincidencia != null) {
                palabra = coincidencia.second
                break
            }
        }
        return Deteccion(etiquetas, palabra)
    }
}
