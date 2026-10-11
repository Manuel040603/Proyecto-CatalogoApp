package com.catalogoapp.consultoras.data.model

data class ClienteFrecuente(
    val id: String = "",
    val uidConsultora: String = "",
    val nombre: String = "",
    val celular: String = "",
    val tipoDocumento: String = "",
    val numeroDocumento: String = "",
    val razonSocial: String = "",
    val direccion: String = "",
    val distrito: String = "",
    val referencia: String = "",
    val totalPedidos: Int = 0,
    val ultimoPedido: Long = 0L
)
