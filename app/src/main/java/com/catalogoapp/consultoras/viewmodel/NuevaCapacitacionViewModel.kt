package com.catalogoapp.consultoras.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.catalogoapp.consultoras.data.model.Capacitacion
import com.catalogoapp.consultoras.data.model.PreguntaCapacitacion
import com.catalogoapp.consultoras.data.repository.CapacitacionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class PreguntaEditable(
    val enunciado: String = "",
    val opcion1: String = "",
    val opcion2: String = "",
    val opcion3: String = "",
    val respuestaCorrecta: Int = 0
)

data class NuevaCapacitacionUiState(
    val titulo: String = "",
    val descripcion: String = "",
    val contenidoNarracion: String = "",
    val preguntas: List<PreguntaEditable> = List(3) { PreguntaEditable() },
    val guardando: Boolean = false,
    val mensaje: String? = null,
    val error: String? = null
)

class NuevaCapacitacionViewModel(
    private val repository: CapacitacionRepository = CapacitacionRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(NuevaCapacitacionUiState())
    val uiState: StateFlow<NuevaCapacitacionUiState> = _uiState

    fun actualizarTitulo(valor: String) {
        _uiState.value = _uiState.value.copy(titulo = valor)
    }

    fun actualizarDescripcion(valor: String) {
        _uiState.value = _uiState.value.copy(descripcion = valor)
    }

    fun actualizarContenidoNarracion(valor: String) {
        _uiState.value = _uiState.value.copy(contenidoNarracion = valor)
    }

    fun actualizarPregunta(indice: Int, pregunta: PreguntaEditable) {
        val actuales = _uiState.value.preguntas.toMutableList()
        if (indice < actuales.size) {
            actuales[indice] = pregunta
        }
        _uiState.value = _uiState.value.copy(preguntas = actuales)
    }

    fun formularioValido(): Boolean {
        val estado = _uiState.value
        if (estado.titulo.isBlank() || estado.contenidoNarracion.isBlank()) return false
        return estado.preguntas.all {
            it.enunciado.isNotBlank() && it.opcion1.isNotBlank() && it.opcion2.isNotBlank() && it.opcion3.isNotBlank()
        }
    }

    fun guardarCapacitacion() {
        val estado = _uiState.value
        if (!formularioValido()) {
            _uiState.value = estado.copy(error = "Completa el título, el contenido a narrar y las 3 preguntas")
            return
        }
        _uiState.value = estado.copy(guardando = true, error = null)
        viewModelScope.launch {
            val preguntas = estado.preguntas.map { editable ->
                PreguntaCapacitacion(
                    enunciado = editable.enunciado,
                    opciones = listOf(editable.opcion1, editable.opcion2, editable.opcion3),
                    respuestaCorrecta = editable.respuestaCorrecta
                )
            }
            val capacitacion = Capacitacion(
                titulo = estado.titulo,
                descripcion = estado.descripcion,
                contenidoNarracion = estado.contenidoNarracion,
                preguntas = preguntas
            )
            val resultado = repository.crearCapacitacion(capacitacion)
            _uiState.value = resultado.fold(
                onSuccess = {
                    NuevaCapacitacionUiState(mensaje = "Capacitación creada correctamente")
                },
                onFailure = {
                    _uiState.value.copy(guardando = false, error = it.message)
                }
            )
        }
    }
}
