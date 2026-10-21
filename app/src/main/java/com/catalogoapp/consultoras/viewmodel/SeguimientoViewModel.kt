package com.catalogoapp.consultoras.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.catalogoapp.consultoras.data.model.ParadaRepartoDto
import com.catalogoapp.consultoras.data.model.RepartoResponse
import com.catalogoapp.consultoras.data.repository.RepartoRepository
import com.catalogoapp.consultoras.util.MensajeError
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SeguimientoUiState(
    val cargando: Boolean = true,
    val reparto: RepartoResponse? = null,
    val parada: ParadaRepartoDto? = null,
    val error: String? = null
)

class SeguimientoViewModel @JvmOverloads constructor(
    private val repartoRepository: RepartoRepository = RepartoRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SeguimientoUiState())
    val uiState: StateFlow<SeguimientoUiState> = _uiState

    fun cargar(pedidoId: Int) {
        _uiState.value = SeguimientoUiState()
        viewModelScope.launch {
            val resumenes = repartoRepository.listarRepartos()
            val lista = resumenes.getOrNull()
            if (lista == null) {
                _uiState.value = SeguimientoUiState(
                    cargando = false,
                    error = MensajeError.de(resumenes.exceptionOrNull(), "No se pudo consultar el reparto")
                )
                return@launch
            }
            val recientes = lista.sortedByDescending { it.id }.take(MAXIMO_REPARTOS_A_REVISAR)
            val detalles = coroutineScope {
                recientes.map { resumen -> async { repartoRepository.obtenerReparto(resumen.id).getOrNull() } }.awaitAll()
            }
            for (detalle in detalles) {
                if (detalle == null) continue
                val parada = detalle.paradas.firstOrNull { it.pedidoId == pedidoId }
                if (parada != null) {
                    _uiState.value = SeguimientoUiState(cargando = false, reparto = detalle, parada = parada)
                    return@launch
                }
            }
            _uiState.value = SeguimientoUiState(cargando = false)
        }
    }

    private companion object {
        const val MAXIMO_REPARTOS_A_REVISAR = 30
    }
}
