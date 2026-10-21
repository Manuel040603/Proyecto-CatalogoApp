package com.catalogoapp.consultoras.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.catalogoapp.consultoras.data.model.Campana
import com.catalogoapp.consultoras.data.model.Producto
import com.catalogoapp.consultoras.data.repository.CampanaRepository
import com.catalogoapp.consultoras.data.repository.PedidoRepository
import com.catalogoapp.consultoras.data.repository.ProductRepository
import com.catalogoapp.consultoras.data.repository.ProfileRepository
import com.catalogoapp.consultoras.util.AdminConfig
import com.catalogoapp.consultoras.util.Inventario
import com.catalogoapp.consultoras.util.MensajeError
import com.catalogoapp.consultoras.util.NivelConsultora
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
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
    val productosDestacados: List<Producto> = emptyList(),
    val cuentaInactiva: Boolean = false,
    val campana: Campana? = null,
    val productosStockBajo: List<Producto> = emptyList(),
    val guardandoCampana: Boolean = false,
    val errorCampana: String? = null
)

class HomeViewModel @JvmOverloads constructor(
    private val profileRepository: ProfileRepository = ProfileRepository(),
    private val pedidoRepository: PedidoRepository = PedidoRepository(),
    private val productRepository: ProductRepository = ProductRepository(),
    private val campanaRepository: CampanaRepository = CampanaRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    private var trabajoCampana: Job? = null

    fun cargar(uid: String) {
        _uiState.value = HomeUiState()

        viewModelScope.launch {
            AdminConfig.cargarRol(FirebaseAuth.getInstance().currentUser?.email)
        }

        trabajoCampana?.cancel()
        trabajoCampana = viewModelScope.launch {
            campanaRepository.observar()
                .catch { }
                .collect { campana ->
                    _uiState.value = _uiState.value.copy(campana = campana)
                }
        }

        viewModelScope.launch {
            val perfil = profileRepository.obtenerPerfil(uid).getOrNull()
            _uiState.value = _uiState.value.copy(
                cuentaInactiva = perfil != null && !perfil.activo,
                nombre = perfil?.nombre ?: "",
                rol = perfil?.rol ?: "Consultora",
            )
        }

        viewModelScope.launch {
            val resultado = pedidoRepository.listarPedidos()
            _uiState.value = resultado.fold(
                onSuccess = { pedidos ->
                    val ventasEntregadas = pedidos.filter { it.estado == "ENTREGADO" }.sumOf { it.total }
                    val nivel = NivelConsultora.calcular(ventasEntregadas)
                    profileRepository.actualizarProgreso(uid, nivel.nivel, nivel.progreso)
                    _uiState.value.copy(
                        cargandoResumen = false,
                        ventasTotales = pedidos.sumOf { it.total },
                        totalPedidos = pedidos.size,
                        pedidosEntregados = pedidos.count { it.estado == "ENTREGADO" },
                        nivel = nivel.nivel,
                        progreso = nivel.progreso,
                        mensajeProgreso = NivelConsultora.mensaje(nivel)
                    )
                },
                onFailure = {
                    _uiState.value.copy(
                        cargandoResumen = false,
                        mensajeProgreso = "No se pudo calcular tu progreso"
                    )
                }
            )
        }

        viewModelScope.launch {
            productRepository.observarCatalogo()
                .catch { }
                .collect { productos ->
                    _uiState.value = _uiState.value.copy(
                        productosDestacados = productos.filter { it.stock > 0 }.take(4),
                        productosStockBajo = productos
                            .filter { Inventario.esBajo(it.stock) || Inventario.agotado(it.stock) }
                            .sortedBy { it.stock }
                    )
                }
        }
    }

    fun guardarCampana(etiqueta: String, titulo: String, detalle: String, activa: Boolean, alTerminar: () -> Unit) {
        if (titulo.isBlank()) {
            _uiState.value = _uiState.value.copy(errorCampana = "Escribe el título de la campaña")
            return
        }
        _uiState.value = _uiState.value.copy(guardandoCampana = true, errorCampana = null)
        viewModelScope.launch {
            val resultado = campanaRepository.guardar(
                Campana(
                    etiqueta = etiqueta.trim(),
                    titulo = titulo.trim(),
                    detalle = detalle.trim(),
                    activa = activa,
                    actualizadoPor = FirebaseAuth.getInstance().currentUser?.email ?: "",
                    ultimaActualizacion = System.currentTimeMillis()
                )
            )
            if (resultado.isSuccess) {
                _uiState.value = _uiState.value.copy(guardandoCampana = false)
                alTerminar()
            } else {
                _uiState.value = _uiState.value.copy(
                    guardandoCampana = false,
                    errorCampana = MensajeError.de(resultado.exceptionOrNull(), "No se pudo guardar la campaña")
                )
            }
        }
    }

    fun limpiarErrorCampana() {
        _uiState.value = _uiState.value.copy(errorCampana = null)
    }
}
