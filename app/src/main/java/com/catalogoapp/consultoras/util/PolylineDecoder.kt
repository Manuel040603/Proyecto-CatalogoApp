package com.catalogoapp.consultoras.util

import org.osmdroid.util.GeoPoint

object PolylineDecoder {

    fun decodificar(codificado: String): List<GeoPoint> {
        val puntos = mutableListOf<GeoPoint>()
        var indice = 0
        var latitud = 0
        var longitud = 0

        while (indice < codificado.length) {
            var resultado = 0
            var desplazamiento = 0
            var byte: Int
            do {
                byte = codificado[indice++].code - 63
                resultado = resultado or ((byte and 0x1f) shl desplazamiento)
                desplazamiento += 5
            } while (byte >= 0x20)
            val deltaLatitud = if ((resultado and 1) != 0) (resultado shr 1).inv() else (resultado shr 1)
            latitud += deltaLatitud

            resultado = 0
            desplazamiento = 0
            do {
                byte = codificado[indice++].code - 63
                resultado = resultado or ((byte and 0x1f) shl desplazamiento)
                desplazamiento += 5
            } while (byte >= 0x20)
            val deltaLongitud = if ((resultado and 1) != 0) (resultado shr 1).inv() else (resultado shr 1)
            longitud += deltaLongitud

            puntos.add(GeoPoint(latitud / 1e5, longitud / 1e5))
        }
        return puntos
    }
}
