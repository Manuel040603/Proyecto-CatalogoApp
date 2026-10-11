package com.catalogoapp.consultoras.viewmodel

import androidx.lifecycle.ViewModel
import com.catalogoapp.consultoras.util.MensajeError
import androidx.lifecycle.viewModelScope
import com.catalogoapp.consultoras.data.model.ParadaRepartoDto
import com.catalogoapp.consultoras.data.model.RepartoResponse
import com.catalogoapp.consultoras.data.repository.RepartoRepository
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
            for (resumen in lista.sortedByDescending { it.id }) {
                val detalle = repartoRepository.obtenerReparto(resumen.id).getOrNull() ?: continue
                val parada = detalle.paradas.firstOrNull { it.pedidoId == pedidoId }
                if (parada != null) {
                    _uiState.value = SeguimientoUiState(cargando = false, reparto = detalle, parada = parada)
                    return@launch
                }
            }
            _uiState.value = SeguimientoUiState(cargando = false)
        }
    }
}
