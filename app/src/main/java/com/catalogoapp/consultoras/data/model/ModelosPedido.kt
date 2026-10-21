package com.catalogoapp.consultoras.data.model

data class ItemPedidoDto(
    val productoId: String = "",
    val nombreProducto: String = "",
    val cantidad: Int = 0,
    val precioUnitario: Double = 0.0
)

data class CrearPedidoRequest(
    val nombreCliente: String,
    val telefonoCliente: String,
    val direccion: String = "",
    val items: List<ItemPedidoDto>
)

data class PedidoResponse(
    val id: Int = 0,
    val uidConsultora: String = "",
    val nombreCliente: String = "",
    val telefonoCliente: String = "",
    val direccion: String = "",
    val total: Double = 0.0,
    val estado: String = "PENDIENTE",
    val fechaCreacion: String = "",
    val fechaActualizacion: String = "",
    val items: List<ItemPedidoDto> = emptyList()
)

data class CambiarEstadoRequest(
    val nuevoEstado: String
)

data class RegistrarDispositivoRequest(
    val tokenFcm: String
)

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

data class CambioEstado(
    val estado: String = "",
    val fecha: Long = 0L
)

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
