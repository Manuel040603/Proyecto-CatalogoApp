package com.catalogoapp.consultoras.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.catalogoapp.consultoras.data.model.PedidoDetalle
import com.catalogoapp.consultoras.data.model.PedidoResponse
import com.catalogoapp.consultoras.data.model.Producto
import com.catalogoapp.consultoras.data.repository.PedidoDetalleRepository
import com.catalogoapp.consultoras.data.repository.PedidoRepository
import com.catalogoapp.consultoras.data.repository.ProductRepository
import com.catalogoapp.consultoras.util.MensajeError
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

data class ComisionPedido(
    val pedido: PedidoResponse,
    val comision: Double
)

data class ComisionesUiState(
    val cargando: Boolean = true,
    val ganadas: Double = 0.0,
    val porGanar: Double = 0.0,
    val ventasEntregadas: Double = 0.0,
    val entregados: List<ComisionPedido> = emptyList(),
    val enCurso: List<ComisionPedido> = emptyList(),
    val sinComisionConfigurada: Boolean = false,
    val error: String? = null
)

class ComisionesViewModel @JvmOverloads constructor(
    private val pedidoRepository: PedidoRepository = PedidoRepository(),
    private val detalleRepository: PedidoDetalleRepository = PedidoDetalleRepository(),
    private val productRepository: ProductRepository = ProductRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ComisionesUiState())
    val uiState: StateFlow<ComisionesUiState> = _uiState

    fun cargar() {
        _uiState.value = ComisionesUiState(cargando = true)
        viewModelScope.launch {
            val resultado = pedidoRepository.listarPedidos()
            val pedidos = resultado.getOrNull()
            if (pedidos == null) {
                _uiState.value = ComisionesUiState(
                    cargando = false,
                    error = MensajeError.de(resultado.exceptionOrNull(), "No se pudieron cargar los pedidos")
                )
                return@launch
            }
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            val detalles: Map<Int, PedidoDetalle> = if (uid != null) {
                detalleRepository.obtenerDeConsultora(uid).getOrNull() ?: emptyMap()
            } else {
                emptyMap()
            }
            val productos: List<Producto> = try {
                withTimeoutOrNull(10_000L) { productRepository.observarCatalogo().first() }.orEmpty()
            } catch (e: FirebaseFirestoreException) {
                emptyList()
            }
            val catalogo: Map<String, Producto> = productos.associateBy { it.id }

            val calculados = pedidos.map { pedido ->
                val guardada = detalles[pedido.id]?.comisionTotal
                val comision = guardada ?: pedido.items.sumOf { item ->
                    (catalogo[item.productoId]?.comision ?: 0.0) * item.cantidad
                }
                ComisionPedido(pedido, comision)
            }
            val entregados = calculados.filter { it.pedido.estado == "ENTREGADO" }
            val enCurso = calculados.filter { it.pedido.estado != "ENTREGADO" }
            _uiState.value = ComisionesUiState(
                cargando = false,
                ganadas = entregados.sumOf { it.comision },
                porGanar = enCurso.sumOf { it.comision },
                ventasEntregadas = entregados.sumOf { it.pedido.total },
                entregados = entregados,
                enCurso = enCurso,
                sinComisionConfigurada = calculados.isNotEmpty() && calculados.all { it.comision == 0.0 }
            )
        }
    }
}
