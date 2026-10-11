package com.catalogoapp.consultoras.viewmodel

import androidx.lifecycle.ViewModel
import com.catalogoapp.consultoras.util.MensajeError
import androidx.lifecycle.viewModelScope
import com.catalogoapp.consultoras.data.model.CrearRepartoRequest
import com.catalogoapp.consultoras.data.model.PedidoResponse
import com.catalogoapp.consultoras.data.model.RepartoResponse
import com.catalogoapp.consultoras.data.model.Repartidor
import com.catalogoapp.consultoras.data.model.ResumenRepartoDto
import com.catalogoapp.consultoras.data.repository.PedidoRepository
import com.catalogoapp.consultoras.data.repository.RepartidorRepository
import com.catalogoapp.consultoras.data.repository.RepartoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

data class RepartoUiState(
    val cargandoPedidos: Boolean = true,
    val pedidosDisponibles: List<PedidoResponse> = emptyList(),
    val pedidosSeleccionados: Set<Int> = emptySet(),
    val zona: String = "",
    val chofer: String = "",
    val vehiculo: String = "",
    val direccionOrigen: String = "",
    val creando: Boolean = false,
    val repartoCreado: RepartoResponse? = null,
    val repartoActual: RepartoResponse? = null,
    val cargandoReparto: Boolean = false,
    val actualizandoParadaId: Int? = null,
    val repartos: List<ResumenRepartoDto> = emptyList(),
    val cargandoRepartos: Boolean = false,
    val repartidores: List<Repartidor> = emptyList(),
    val choferesOcupados: Set<String> = emptySet(),
    val repartidorSeleccionadoId: String? = null,
    val error: String? = null
)

class RepartoViewModel @JvmOverloads constructor(
    private val repartoRepository: RepartoRepository = RepartoRepository(),
    private val pedidoRepository: PedidoRepository = PedidoRepository(),
    private val repartidorRepository: RepartidorRepository = RepartidorRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(RepartoUiState())
    val uiState: StateFlow<RepartoUiState> = _uiState

    init {
        viewModelScope.launch {
            repartidorRepository.observar()
                .catch { excepcion -> _uiState.value = _uiState.value.copy(error = MensajeError.de(excepcion, "Ocurrio un error")) }
                .collect { lista ->
                    _uiState.value = _uiState.value.copy(repartidores = lista)
                    asignarAutomaticamente()
                }
        }
        cargarPedidosDisponibles()
    }

    private fun normalizar(texto: String): String = texto.trim().lowercase()

    fun estaOcupado(repartidor: Repartidor): Boolean {
        return _uiState.value.choferesOcupados.contains(normalizar(repartidor.nombre))
    }

    private fun asignarAutomaticamente() {
        val estado = _uiState.value
        val actual = estado.repartidores.firstOrNull { it.id == estado.repartidorSeleccionadoId }
        if (actual != null && actual.activo && !estaOcupado(actual)) return
        val libre = estado.repartidores.firstOrNull { it.activo && !estaOcupado(it) }
        _uiState.value = estado.copy(
            repartidorSeleccionadoId = libre?.id,
            chofer = libre?.nombre ?: "",
            vehiculo = libre?.vehiculo ?: ""
        )
    }

    fun seleccionarRepartidor(repartidor: Repartidor) {
        if (!repartidor.activo || estaOcupado(repartidor)) return
        _uiState.value = _uiState.value.copy(
            repartidorSeleccionadoId = repartidor.id,
            chofer = repartidor.nombre,
            vehiculo = repartidor.vehiculo
        )
    }

    fun agregarRepartidor(nombre: String, vehiculo: String, telefono: String) {
        if (nombre.isBlank() || vehiculo.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "El repartidor necesita nombre y placa del vehiculo")
            return
        }
        viewModelScope.launch {
            val resultado = repartidorRepository.agregar(
                Repartidor(nombre = nombre.trim(), vehiculo = vehiculo.trim().uppercase(), telefono = telefono.trim())
            )
            resultado.exceptionOrNull()?.let { excepcion ->
                _uiState.value = _uiState.value.copy(error = MensajeError.de(excepcion, "Ocurrio un error"))
            }
        }
    }

    fun cargarPedidosDisponibles() {
        _uiState.value = _uiState.value.copy(cargandoPedidos = true)
        viewModelScope.launch {
            val resultado = pedidoRepository.listarPedidos()
            val lista = resultado.getOrNull()
            if (lista == null) {
                _uiState.value = _uiState.value.copy(cargandoPedidos = false, error = MensajeError.de(resultado.exceptionOrNull(), "Ocurrio un error"))
                return@launch
            }

            val resumenes = repartoRepository.listarRepartos().getOrNull()
            val choferesOcupados = mutableSetOf<String>()
            val pedidosEnReparto = mutableSetOf<Int>()
            if (resumenes != null) {
                for (resumen in resumenes.filter { it.estado == "EN_CURSO" }) {
                    choferesOcupados.add(normalizar(resumen.chofer))
                    val detalle = repartoRepository.obtenerReparto(resumen.id).getOrNull()
                    detalle?.paradas?.forEach { pedidosEnReparto.add(it.pedidoId) }
                }
            }

            val disponibles = lista.filter {
                it.estado != "ENTREGADO" && it.direccion.isNotBlank() && !pedidosEnReparto.contains(it.id)
            }
            _uiState.value = _uiState.value.copy(
                cargandoPedidos = false,
                pedidosDisponibles = disponibles,
                choferesOcupados = choferesOcupados,
                pedidosSeleccionados = _uiState.value.pedidosSeleccionados.filter { id -> disponibles.any { it.id == id } }.toSet(),
                error = if (resumenes == null) "No se pudo verificar que repartidores estan ocupados" else null
            )
            asignarAutomaticamente()
        }
    }

    fun limpiarRepartoCreado() {
        _uiState.value = _uiState.value.copy(
            repartoCreado = null,
            pedidosSeleccionados = emptySet(),
            zona = "",
            direccionOrigen = ""
        )
    }

    fun actualizarZona(valor: String) {
        _uiState.value = _uiState.value.copy(zona = valor)
    }

    fun actualizarChofer(valor: String) {
        _uiState.value = _uiState.value.copy(chofer = valor)
    }

    fun actualizarVehiculo(valor: String) {
        _uiState.value = _uiState.value.copy(vehiculo = valor)
    }

    fun actualizarDireccionOrigen(valor: String) {
        _uiState.value = _uiState.value.copy(direccionOrigen = valor)
    }

    fun alternarPedidoSeleccionado(idPedido: Int) {
        val actuales = _uiState.value.pedidosSeleccionados.toMutableSet()
        if (actuales.contains(idPedido)) actuales.remove(idPedido) else actuales.add(idPedido)
        _uiState.value = _uiState.value.copy(pedidosSeleccionados = actuales)
    }

    fun formularioValido(): Boolean {
        val estado = _uiState.value
        return estado.zona.isNotBlank() &&
            estado.chofer.isNotBlank() &&
            estado.vehiculo.isNotBlank() &&
            estado.direccionOrigen.isNotBlank() &&
            estado.pedidosSeleccionados.isNotEmpty()
    }

    fun crearReparto() {
        val estado = _uiState.value
        if (!formularioValido()) {
            val faltaRepartidor = estado.chofer.isBlank() || estado.vehiculo.isBlank()
            _uiState.value = estado.copy(
                error = if (faltaRepartidor) {
                    "No hay un repartidor disponible. Registra uno o espera a que termine su reparto en curso"
                } else {
                    "Completa zona, direccion de origen y selecciona al menos un pedido"
                }
            )
            return
        }
        _uiState.value = estado.copy(creando = true, error = null)
        viewModelScope.launch {
            val datos = CrearRepartoRequest(
                zona = estado.zona,
                chofer = estado.chofer,
                vehiculo = estado.vehiculo,
                direccionOrigen = estado.direccionOrigen,
                pedidoIds = estado.pedidosSeleccionados.toList()
            )
            val resultado = repartoRepository.crearReparto(datos)
            _uiState.value = resultado.fold(
                onSuccess = { reparto -> _uiState.value.copy(creando = false, repartoCreado = reparto) },
                onFailure = { excepcion -> _uiState.value.copy(creando = false, error = MensajeError.de(excepcion, "No se pudo crear el reparto")) }
            )
        }
    }

    fun cargarRepartos() {
        _uiState.value = _uiState.value.copy(cargandoRepartos = true)
        viewModelScope.launch {
            val resultado = repartoRepository.listarRepartos()
            _uiState.value = resultado.fold(
                onSuccess = { lista -> _uiState.value.copy(cargandoRepartos = false, repartos = lista, error = null) },
                onFailure = { excepcion -> _uiState.value.copy(cargandoRepartos = false, error = MensajeError.de(excepcion, "Ocurrio un error")) }
            )
        }
    }

    fun cargarReparto(id: Int) {
        _uiState.value = _uiState.value.copy(cargandoReparto = true)
        viewModelScope.launch {
            val resultado = repartoRepository.obtenerReparto(id)
            _uiState.value = resultado.fold(
                onSuccess = { reparto -> _uiState.value.copy(cargandoReparto = false, repartoActual = reparto, error = null) },
                onFailure = { excepcion -> _uiState.value.copy(cargandoReparto = false, error = MensajeError.de(excepcion, "Ocurrio un error")) }
            )
        }
    }

    fun marcarParadaEntregada(idReparto: Int, idParada: Int) {
        _uiState.value = _uiState.value.copy(actualizandoParadaId = idParada)
        viewModelScope.launch {
            val resultado = repartoRepository.marcarParadaEntregada(idReparto, idParada)
            _uiState.value = resultado.fold(
                onSuccess = {
                    _uiState.value.copy(actualizandoParadaId = null)
                },
                onFailure = { excepcion -> _uiState.value.copy(actualizandoParadaId = null, error = MensajeError.de(excepcion, "Ocurrio un error")) }
            )
            if (resultado.isSuccess) {
                cargarReparto(idReparto)
            }
        }
    }

    fun actualizarMiUbicacion(idReparto: Int, latitud: Double, longitud: Double) {
        viewModelScope.launch {
            repartoRepository.actualizarUbicacion(idReparto, latitud, longitud)
        }
    }

    fun limpiarError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
