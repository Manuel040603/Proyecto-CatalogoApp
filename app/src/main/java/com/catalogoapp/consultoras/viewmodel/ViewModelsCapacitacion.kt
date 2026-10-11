package com.catalogoapp.consultoras.viewmodel

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.catalogoapp.consultoras.data.model.Capacitacion
import com.catalogoapp.consultoras.data.model.PreguntaCapacitacion
import com.catalogoapp.consultoras.data.model.ProgresoCapacitacion
import com.catalogoapp.consultoras.data.repository.CapacitacionRepository
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

data class CapacitacionListUiState(
    val cargando: Boolean = true,
    val capacitaciones: List<Capacitacion> = emptyList(),
    val error: String? = null
)

class CapacitacionViewModel(
    private val repository: CapacitacionRepository = CapacitacionRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(CapacitacionListUiState())
    val uiState: StateFlow<CapacitacionListUiState> = _uiState

    init {
        viewModelScope.launch {
            repository.observarCapacitaciones()
                .catch { excepcion ->
                    _uiState.value = _uiState.value.copy(cargando = false, error = excepcion.message)
                }
                .collect { capacitaciones ->
                    _uiState.value = _uiState.value.copy(cargando = false, capacitaciones = capacitaciones)
                }
        }
    }
}

data class CapacitacionDetalleUiState(
    val capacitacion: Capacitacion? = null,
    val cargando: Boolean = true,
    val reproduciendo: Boolean = false,
    val enEvaluacion: Boolean = false,
    val preguntaActual: Int = 0,
    val respuestasSeleccionadas: List<Int?> = emptyList(),
    val evaluacionTerminada: Boolean = false,
    val puntaje: Int = 0,
    val guardando: Boolean = false,
    val mensaje: String? = null,
    val error: String? = null
)

class CapacitacionDetalleViewModel(
    private val repository: CapacitacionRepository = CapacitacionRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(CapacitacionDetalleUiState())
    val uiState: StateFlow<CapacitacionDetalleUiState> = _uiState

    private var textToSpeech: TextToSpeech? = null

    fun cargar(capacitacionId: String) {
        viewModelScope.launch {
            repository.observarCapacitaciones()
                .catch { excepcion ->
                    _uiState.value = _uiState.value.copy(cargando = false, error = excepcion.message)
                }
                .collect { capacitaciones ->
                    val encontrada = capacitaciones.firstOrNull { it.id == capacitacionId }
                    if (encontrada != null && _uiState.value.capacitacion == null) {
                        _uiState.value = _uiState.value.copy(
                            capacitacion = encontrada,
                            cargando = false,
                            respuestasSeleccionadas = List(encontrada.preguntas.size) { null }
                        )
                    }
                }
        }
    }

    fun iniciarNarrador(context: Context) {
        if (textToSpeech != null) return
        textToSpeech = TextToSpeech(context.applicationContext) { estado ->
            if (estado == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale("spa", "PE")
                textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) {
                        _uiState.value = _uiState.value.copy(reproduciendo = false)
                    }
                    override fun onError(utteranceId: String?) {
                        _uiState.value = _uiState.value.copy(reproduciendo = false)
                    }
                })
            }
        }
    }

    fun reproducirPausar() {
        val texto = _uiState.value.capacitacion?.contenidoNarracion ?: return
        val narrador = textToSpeech ?: return
        if (_uiState.value.reproduciendo) {
            narrador.stop()
            _uiState.value = _uiState.value.copy(reproduciendo = false)
        } else {
            narrador.speak(texto, TextToSpeech.QUEUE_FLUSH, null, "capacitacion")
            _uiState.value = _uiState.value.copy(reproduciendo = true)
        }
    }

    fun iniciarEvaluacion() {
        val totalPreguntas = _uiState.value.capacitacion?.preguntas?.size ?: 0
        _uiState.value = _uiState.value.copy(
            enEvaluacion = true,
            preguntaActual = 0,
            respuestasSeleccionadas = List(totalPreguntas) { null },
            evaluacionTerminada = false
        )
    }

    fun seleccionarRespuesta(indiceOpcion: Int) {
        val actuales = _uiState.value.respuestasSeleccionadas.toMutableList()
        if (_uiState.value.preguntaActual < actuales.size) {
            actuales[_uiState.value.preguntaActual] = indiceOpcion
        }
        _uiState.value = _uiState.value.copy(respuestasSeleccionadas = actuales)
    }

    fun siguientePregunta() {
        val estado = _uiState.value
        val preguntas = estado.capacitacion?.preguntas ?: emptyList()
        if (estado.preguntaActual < preguntas.size - 1) {
            _uiState.value = estado.copy(preguntaActual = estado.preguntaActual + 1)
        } else {
            val correctas = preguntas.mapIndexed { indice, pregunta ->
                if (estado.respuestasSeleccionadas.getOrNull(indice) == pregunta.respuestaCorrecta) 1 else 0
            }.sum()
            _uiState.value = estado.copy(evaluacionTerminada = true, puntaje = correctas)
        }
    }

    fun guardarResultado(uid: String) {
        val estado = _uiState.value
        val capacitacion = estado.capacitacion ?: return
        _uiState.value = estado.copy(guardando = true)
        viewModelScope.launch {
            val progreso = ProgresoCapacitacion(
                uid = uid,
                capacitacionId = capacitacion.id,
                tituloCapacitacion = capacitacion.titulo,
                puntaje = estado.puntaje,
                totalPreguntas = capacitacion.preguntas.size
            )
            val resultado = repository.guardarProgreso(progreso)
            _uiState.value = resultado.fold(
                onSuccess = { _uiState.value.copy(guardando = false, mensaje = "Resultado guardado") },
                onFailure = { _uiState.value.copy(guardando = false, mensaje = it.message) }
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
    }
}

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
            _uiState.value = estado.copy(error = "Completa el titulo, el contenido a narrar y las 3 preguntas")
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
                    NuevaCapacitacionUiState(mensaje = "Capacitacion creada correctamente")
                },
                onFailure = {
                    _uiState.value.copy(guardando = false, error = it.message)
                }
            )
        }
    }
}
