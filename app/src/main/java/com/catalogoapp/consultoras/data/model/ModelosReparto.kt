package com.catalogoapp.consultoras.data.model

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
    val geometriaRuta: String? = null,
    val latitudActual: Double? = null,
    val longitudActual: Double? = null,
    val fechaUbicacion: String? = null
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

data class Repartidor(
    val id: String = "",
    val nombre: String = "",
    val vehiculo: String = "",
    val telefono: String = "",
    val activo: Boolean = true
)
