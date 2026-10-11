package com.catalogoapp.consultoras.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.catalogoapp.consultoras.data.model.PedidoDetalle
import com.catalogoapp.consultoras.data.repository.PedidoDetalleRepository
import com.catalogoapp.consultoras.data.repository.ProfileRepository
import com.catalogoapp.consultoras.util.MensajeError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

data class AdminPedidosUiState(
    val cargando: Boolean = true,
    val pedidos: List<PedidoDetalle> = emptyList(),
    val nombresConsultoras: Map<String, String> = emptyMap(),
    val filtroEstado: String? = null,
    val error: String? = null
) {
    val visibles: List<PedidoDetalle>
        get() = if (filtroEstado == null) pedidos else pedidos.filter { it.estado == filtroEstado }

    val estadosPresentes: List<String>
        get() = pedidos.map { it.estado }.distinct()

    val ventasTotales: Double
        get() = pedidos.sumOf { it.total }
}

class AdminPedidosViewModel @JvmOverloads constructor(
    private val detalleRepository: PedidoDetalleRepository = PedidoDetalleRepository(),
    private val profileRepository: ProfileRepository = ProfileRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminPedidosUiState())
    val uiState: StateFlow<AdminPedidosUiState> = _uiState

    fun cargar() {
        _uiState.value = _uiState.value.copy(cargando = true, error = null)
        viewModelScope.launch {
            val resultado = detalleRepository.obtenerTodos()
            val pedidos = resultado.getOrNull()
            if (pedidos == null) {
                _uiState.value = _uiState.value.copy(
                    cargando = false,
                    error = MensajeError.de(resultado.exceptionOrNull(), "No se pudieron cargar los pedidos")
                )
                return@launch
            }
            val consultoras = withTimeoutOrNull(10_000L) {
                profileRepository.observarConsultoras().first()
            }.orEmpty()
            val nombres = consultoras.associate { it.uid to it.nombre.ifBlank { it.email } }
            _uiState.value = _uiState.value.copy(cargando = false, pedidos = pedidos, nombresConsultoras = nombres)
        }
    }

    fun filtrarPorEstado(estado: String?) {
        _uiState.value = _uiState.value.copy(filtroEstado = estado)
    }
}
