package com.catalogoapp.consultoras.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.catalogoapp.consultoras.data.model.Capacitacion
import com.catalogoapp.consultoras.data.repository.CapacitacionRepository
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
