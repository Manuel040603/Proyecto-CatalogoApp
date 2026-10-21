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
