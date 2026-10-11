package com.catalogoapp.consultoras.data.model

data class Producto @JvmOverloads constructor(
    val id: String = "",
    val nombre: String = "",
    val categoria: String = "",
    val precio: Double = 0.0,
    val descripcion: String = "",
    val stock: Int = 0,
    val imagenUrl: String = "",
    // Campos agregados para Diseño v3
    val comision: Double = 0.0,
    val esNuevo: Boolean = false,
    val volumen: String = "",
    val especificaciones: String = "",
    val resenas_promedio: Double = 0.0,
    val resenas_cantidad: Int = 0
)
