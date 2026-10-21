package com.catalogoapp.consultoras.data.model

data class Devolucion(
    val id: String = "",
    val uidConsultora: String = "",
    val emailConsultora: String = "",
    val productoId: String = "",
    val productoNombre: String = "",
    val apto: Boolean = false,
    val etiquetaDetectada: String = "",
    val confianza: Double = 0.0,
    val stockRepuesto: Boolean = false,
    val fecha: Long = 0L
)
