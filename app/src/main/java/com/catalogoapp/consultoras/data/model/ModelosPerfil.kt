package com.catalogoapp.consultoras.data.model

data class ConsultoraProfile @JvmOverloads constructor(
    val uid: String = "",
    val nombre: String = "",
    val email: String = "",
    val preferencias: String = "",
    val fotoBase64: String = "",
    val activo: Boolean = true,
    val esAdmin: Boolean = false,
    val rol: String = "CONSULTORA",
    val ultimaActualizacion: Long = System.currentTimeMillis(),
    val nivel: String = "Plata",
    val progreso: Double = 0.0,
    val dni: String = "",
    val celular: String = "",
    val direccionEntrega: String = "",
    val distrito: String = "",
    val referencia: String = "",
    val tipoComprobante: String = "Boleta"
)
