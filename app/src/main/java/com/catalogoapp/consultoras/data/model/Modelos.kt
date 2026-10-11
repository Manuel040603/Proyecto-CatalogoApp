package com.catalogoapp.consultoras.data.model



data class PreguntaCapacitacion @JvmOverloads constructor(
    val enunciado: String = "",
    val opciones: List<String> = emptyList(),
    val respuestaCorrecta: Int = 0
)

data class Capacitacion @JvmOverloads constructor(
    val id: String = "",
    val titulo: String = "",
    val descripcion: String = "",
    val contenidoNarracion: String = "",
    val preguntas: List<PreguntaCapacitacion> = emptyList(),
    val fechaCreacion: Long = System.currentTimeMillis()
)

data class ProgresoCapacitacion @JvmOverloads constructor(
    val uid: String = "",
    val capacitacionId: String = "",
    val tituloCapacitacion: String = "",
    val puntaje: Int = 0,
    val totalPreguntas: Int = 0,
    val fecha: Long = System.currentTimeMillis()
)

data class ConsultoraProfile @JvmOverloads constructor(
    val uid: String = "",
    val nombre: String = "",
    val email: String = "",
    val preferencias: String = "",
    val fotoBase64: String = "",
    val activo: Boolean = true,
    val esAdmin: Boolean = false,
    val ultimaActualizacion: Long = System.currentTimeMillis(),
    // Campos agregados para Diseño v3
    val rol: String = "Consultora",
    val nivel: String = "Plata",
    val progreso: Double = 0.0,
    val dni: String = "",
    val celular: String = "",
    val direccionEntrega: String = "",
    val distrito: String = "",
    val referencia: String = "",
    val tipoComprobante: String = "Boleta"
)

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

data class CrearRepartoRequest(
    val zona: String,
    val chofer: String,
    val vehiculo: String,
    val direccionOrigen: String,
    val pedidoIds: List<Int>
)

data class ParadaRepartoDto(
    val id: Int = 0,
    val pedidoId: Int = 0,
    val nombreCliente: String = "",
    val direccion: String = "",
    val latitud: Double = 0.0,
    val longitud: Double = 0.0,
    val orden: Int = 0,
    val entregada: Boolean = false
)

data class RepartoResponse(
    val id: Int = 0,
    val zona: String = "",
    val chofer: String = "",
    val vehiculo: String = "",
    val estado: String = "",
    val paradas: List<ParadaRepartoDto> = emptyList(),
    val geometriaRuta: String? = null
)

data class ActualizarUbicacionRequest(
    val latitud: Double,
    val longitud: Double
)

data class ResumenRepartoDto(
    val id: Int = 0,
    val zona: String = "",
    val chofer: String = "",
    val vehiculo: String = "",
    val estado: String = "",
    val fechaCreacion: String = "",
    val totalParadas: Int = 0,
    val paradasEntregadas: Int = 0
)

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

data class Campana(
    val etiqueta: String = "",
    val titulo: String = "",
    val detalle: String = "",
    val activa: Boolean = true,
    val actualizadoPor: String = "",
    val ultimaActualizacion: Long = 0L
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

data class Repartidor(
    val id: String = "",
    val nombre: String = "",
    val vehiculo: String = "",
    val telefono: String = "",
    val activo: Boolean = true
)
