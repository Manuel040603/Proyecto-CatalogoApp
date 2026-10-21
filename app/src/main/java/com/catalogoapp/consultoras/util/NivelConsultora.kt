package com.catalogoapp.consultoras.util

object NivelConsultora {

    const val UMBRAL_ORO = 1000.0
    const val UMBRAL_PLATINO = 3000.0

    data class Resultado(
        val nivel: String,
        val progreso: Double,
        val siguiente: String?,
        val faltante: Double
    )

    fun calcular(ventasEntregadas: Double): Resultado {
        val ventas = if (ventasEntregadas < 0.0) 0.0 else ventasEntregadas
        return when {
            ventas >= UMBRAL_PLATINO -> Resultado("Platino", 1.0, null, 0.0)
            ventas >= UMBRAL_ORO -> Resultado(
                nivel = "Oro",
                progreso = (ventas - UMBRAL_ORO) / (UMBRAL_PLATINO - UMBRAL_ORO),
                siguiente = "Platino",
                faltante = UMBRAL_PLATINO - ventas
            )
            else -> Resultado("Plata", ventas / UMBRAL_ORO, "Oro", UMBRAL_ORO - ventas)
        }
    }

    fun mensaje(resultado: Resultado): String {
        return if (resultado.siguiente == null) {
            "Alcanzaste el nivel ${resultado.nivel}"
        } else {
            "Te faltan S/ ${"%.2f".format(resultado.faltante)} para Nivel ${resultado.siguiente}"
        }
    }
}
