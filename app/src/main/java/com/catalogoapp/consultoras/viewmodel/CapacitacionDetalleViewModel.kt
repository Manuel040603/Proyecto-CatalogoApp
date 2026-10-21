package com.catalogoapp.consultoras.viewmodel

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.catalogoapp.consultoras.data.model.Capacitacion
import com.catalogoapp.consultoras.data.model.ProgresoCapacitacion
import com.catalogoapp.consultoras.data.repository.CapacitacionRepository
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

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

    fun detenerNarracion() {
        textToSpeech?.stop()
        _uiState.value = _uiState.value.copy(reproduciendo = false)
    }

    fun pausarNarracion() {
        textToSpeech?.stop()
        _uiState.value = _uiState.value.copy(reproduciendo = false)
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
