package com.catalogoapp.consultoras.data.model

data class PedidoDetalle(
    val pedidoId: Int = 0,
    val uidConsultora: String = "",
    val nombreCliente: String = "",
    val telefonoCliente: String = "",
    val tipoDocumento: String = "",
    val numeroDocumento: String = "",
    val razonSocial: String = "",
    val tipoEntrega: String = "Al cliente",
    val puntoEntrega: String = "",
    val direccion: String = "",
    val distrito: String = "",
    val referencia: String = "",
    val indicaciones: String = "",
    val fechaEntrega: String = "",
    val metodoPago: String = "Contra entrega",
    val total: Double = 0.0,
    val costoEnvio: Double = 0.0,
    val totalCobrado: Double = 0.0,
    val estado: String = "PENDIENTE",
    val items: List<ItemPedidoDto> = emptyList(),
    val fechaCreacion: Long = 0L,
    val fechaActualizacion: Long = 0L,
    val historial: List<CambioEstado> = emptyList(),
    val comisionTotal: Double? = null
)
