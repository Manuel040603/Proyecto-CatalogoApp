package com.catalogoapp.consultoras.data.model

import com.google.firebase.firestore.PropertyName

data class Producto @JvmOverloads constructor(
    val id: String = "",
    val nombre: String = "",
    val categoria: String = "",
    val precio: Double = 0.0,
    val descripcion: String = "",
    val stock: Int = 0,
    @get:PropertyName("imagenUrl") @set:PropertyName("imagenUrl") var imagenUrl: String = "",
    val comision: Double = 0.0,
    val esNuevo: Boolean = false,
    val volumen: String = "",
    val especificaciones: String = "",
    val resenas_promedio: Double = 0.0,
    val resenas_cantidad: Int = 0
)

data class Campana(
    val etiqueta: String = "",
    val titulo: String = "",
    val detalle: String = "",
    val activa: Boolean = true,
    val actualizadoPor: String = "",
    val ultimaActualizacion: Long = 0L
)
