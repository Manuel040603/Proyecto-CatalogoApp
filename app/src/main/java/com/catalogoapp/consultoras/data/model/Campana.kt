package com.catalogoapp.consultoras.data.model

data class Campana(
    val etiqueta: String = "",
    val titulo: String = "",
    val detalle: String = "",
    val activa: Boolean = true,
    val actualizadoPor: String = "",
    val ultimaActualizacion: Long = 0L
)
