package com.catalogoapp.consultoras.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.catalogoapp.consultoras.data.model.Producto
import com.catalogoapp.consultoras.data.repository.PedidoRepository
import com.catalogoapp.consultoras.data.repository.ProductRepository
import com.catalogoapp.consultoras.data.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

data class HomeUiState(
    val nombre: String = "",
    val rol: String = "Consultora",
    val nivel: String = "Plata",
    val progreso: Double = 0.0,
    val mensajeProgreso: String = "Cargando progreso...",
    val cargandoResumen: Boolean = true,
    val ventasTotales: Double = 0.0,
    val totalPedidos: Int = 0,
    val pedidosEntregados: Int = 0,
    val productosDestacados: List<Producto> = emptyList()
)

class HomeViewModel @JvmOverloads constructor(
    private val profileRepository: ProfileRepository = ProfileRepository(),
    private val pedidoRepository: PedidoRepository = PedidoRepository(),
    private val productRepository: ProductRepository = ProductRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    fun cargar(uid: String) {
        _uiState.value = HomeUiState()

        viewModelScope.launch {
            val perfil = profileRepository.obtenerPerfil(uid).getOrNull()
            _uiState.value = _uiState.value.copy(
                nombre = perfil?.nombre ?: "",
                rol = perfil?.rol ?: "Consultora",
                nivel = perfil?.nivel ?: "Plata",
                progreso = perfil?.progreso ?: 0.0,
                mensajeProgreso = "Te faltan S/ ${ (1000 - (perfil?.progreso ?: 0.0) * 10).toInt() } para Nivel Oro"
            )
        }

        viewModelScope.launch {
            val resultado = pedidoRepository.listarPedidos()
            _uiState.value = resultado.fold(
                onSuccess = { pedidos ->
                    _uiState.value.copy(
                        cargandoResumen = false,
                        ventasTotales = pedidos.sumOf { it.total },
                        totalPedidos = pedidos.size,
                        pedidosEntregados = pedidos.count { it.estado == "ENTREGADO" }
                    )
                },
                onFailure = { _uiState.value.copy(cargandoResumen = false) }
            )
        }

        viewModelScope.launch {
            productRepository.observarCatalogo()
                .catch { }
                .collect { productos ->
                    _uiState.value = _uiState.value.copy(productosDestacados = productos.take(4))
                }
        }
    }
}
