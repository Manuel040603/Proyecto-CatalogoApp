package com.catalogoapp.consultoras.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.catalogoapp.consultoras.data.model.CambioEstado
import com.catalogoapp.consultoras.data.model.ClienteFrecuente
import com.catalogoapp.consultoras.data.model.CrearPedidoRequest
import com.catalogoapp.consultoras.data.model.ItemPedidoDto
import com.catalogoapp.consultoras.data.model.PedidoDetalle
import com.catalogoapp.consultoras.data.model.PedidoResponse
import com.catalogoapp.consultoras.data.model.Producto
import com.catalogoapp.consultoras.data.repository.ClienteRepository
import com.catalogoapp.consultoras.data.repository.PedidoDetalleRepository
import com.catalogoapp.consultoras.data.repository.PedidoRepository
import com.catalogoapp.consultoras.data.repository.ProductRepository
import com.catalogoapp.consultoras.data.repository.ProfileRepository
import com.catalogoapp.consultoras.util.MensajeError
import com.catalogoapp.consultoras.util.PedidoValidacion
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

data class ItemCarrito(
    val producto: Producto,
    val cantidad: Int
)

data class PedidoUiState(
    val cargandoLista: Boolean = true,
    val pedidos: List<PedidoResponse> = emptyList(),
    val detalles: Map<Int, PedidoDetalle> = emptyMap(),
    val productosDisponibles: List<Producto> = emptyList(),
    val nombreCliente: String = "",
    val telefonoCliente: String = "",
    val tipoDocumento: String = PedidoValidacion.SIN_DOCUMENTO,
    val numeroDocumento: String = "",
    val razonSocial: String = "",
    val tipoEntrega: String = PedidoValidacion.ENTREGA_CLIENTE,
    val direccion: String = "",
    val distrito: String = "",
    val referencia: String = "",
    val indicaciones: String = "",
    val fechaEntrega: String = "",
    val metodoPago: String = PedidoValidacion.METODOS_PAGO.first(),
    val carrito: List<ItemCarrito> = emptyList(),
    val clientesFrecuentes: List<ClienteFrecuente> = emptyList(),
    val guardando: Boolean = false,
    val actualizandoId: Int? = null,
    val ultimoPedidoId: Int? = null,
    val mensaje: String? = null,
    val aviso: String? = null,
    val error: String? = null
)

class PedidoViewModel @JvmOverloads constructor(
    private val repository: PedidoRepository = PedidoRepository(),
    private val productRepository: ProductRepository = ProductRepository(),
    private val detalleRepository: PedidoDetalleRepository = PedidoDetalleRepository(),
    private val profileRepository: ProfileRepository = ProfileRepository(),
    private val clienteRepository: ClienteRepository = ClienteRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(PedidoUiState())
    val uiState: StateFlow<PedidoUiState> = _uiState

    private var uidActual: String? = null

    init {
        viewModelScope.launch {
            productRepository.observarCatalogo()
                .catch { }
                .collect { productos ->
                    _uiState.value = _uiState.value.copy(productosDisponibles = productos)
                }
        }
        cargarPedidos()
    }

    fun cargarPedidos() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != uidActual) {
            uidActual = uid
            _uiState.value = PedidoUiState(productosDisponibles = _uiState.value.productosDisponibles)
        }
        _uiState.value = _uiState.value.copy(cargandoLista = true)
        viewModelScope.launch {
            val resultado = repository.listarPedidos()
            val lista = resultado.getOrNull()
            if (lista == null) {
                _uiState.value = _uiState.value.copy(
                    cargandoLista = false,
                    error = MensajeError.de(resultado.exceptionOrNull(), "No se pudieron cargar los pedidos")
                )
                return@launch
            }
            var detalles = emptyMap<Int, PedidoDetalle>()
            var aviso: String? = null
            if (uid != null) {
                val resultadoDetalles = detalleRepository.obtenerDeConsultora(uid)
                val mapa = resultadoDetalles.getOrNull()
                if (mapa != null) {
                    detalles = mapa
                } else {
                    aviso = "No se pudieron leer los datos de entrega de Firebase: ${MensajeError.de(resultadoDetalles.exceptionOrNull(), "error desconocido")}"
                }
            }
            _uiState.value = _uiState.value.copy(
                cargandoLista = false,
                pedidos = lista,
                detalles = detalles,
                aviso = aviso ?: _uiState.value.aviso,
                error = null
            )
        }
    }

    fun cargarClientesFrecuentes() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewModelScope.launch {
            val lista = clienteRepository.obtenerDeConsultora(uid).getOrNull() ?: return@launch
            _uiState.value = _uiState.value.copy(clientesFrecuentes = lista)
        }
    }

    fun aplicarCliente(cliente: ClienteFrecuente) {
        val actual = _uiState.value
        val aDomicilio = actual.tipoEntrega == PedidoValidacion.ENTREGA_CLIENTE
        _uiState.value = actual.copy(
            nombreCliente = cliente.nombre,
            telefonoCliente = cliente.celular,
            tipoDocumento = cliente.tipoDocumento,
            numeroDocumento = cliente.numeroDocumento,
            razonSocial = cliente.razonSocial,
            direccion = if (aDomicilio) cliente.direccion else actual.direccion,
            distrito = if (aDomicilio) cliente.distrito else actual.distrito,
            referencia = if (aDomicilio) cliente.referencia else actual.referencia,
            error = null
        )
    }

    fun actualizarNombreCliente(valor: String) {
        _uiState.value = _uiState.value.copy(nombreCliente = valor)
    }

    fun actualizarTelefonoCliente(valor: String) {
        _uiState.value = _uiState.value.copy(telefonoCliente = PedidoValidacion.soloDigitos(valor, 9))
    }

    fun actualizarTipoDocumento(valor: String) {
        _uiState.value = _uiState.value.copy(tipoDocumento = valor, numeroDocumento = "", razonSocial = "")
    }

    fun actualizarNumeroDocumento(valor: String) {
        val largo = PedidoValidacion.largoDocumento(_uiState.value.tipoDocumento)
        _uiState.value = _uiState.value.copy(numeroDocumento = PedidoValidacion.soloDigitos(valor, largo))
    }

    fun actualizarRazonSocial(valor: String) {
        _uiState.value = _uiState.value.copy(razonSocial = valor)
    }

    fun actualizarTipoEntrega(valor: String) {
        if (valor == _uiState.value.tipoEntrega) return
        _uiState.value = _uiState.value.copy(
            tipoEntrega = valor,
            direccion = "",
            distrito = "",
            referencia = "",
            error = null
        )
        if (valor == PedidoValidacion.ENTREGA_CONSULTORA) {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
            viewModelScope.launch {
                val perfil = profileRepository.obtenerPerfil(uid).getOrNull()
                if (_uiState.value.tipoEntrega != PedidoValidacion.ENTREGA_CONSULTORA) return@launch
                if (perfil == null || perfil.direccionEntrega.isBlank() || perfil.distrito.isBlank()) {
                    _uiState.value = _uiState.value.copy(
                        error = "Primero completa tu direccion y distrito en Perfil, Datos y direccion"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        direccion = perfil.direccionEntrega,
                        distrito = perfil.distrito,
                        referencia = perfil.referencia
                    )
                }
            }
        }
    }

    fun actualizarDireccion(valor: String) {
        _uiState.value = _uiState.value.copy(direccion = valor)
    }

    fun actualizarDistrito(valor: String) {
        _uiState.value = _uiState.value.copy(distrito = valor)
    }

    fun actualizarReferencia(valor: String) {
        _uiState.value = _uiState.value.copy(referencia = valor)
    }

    fun actualizarIndicaciones(valor: String) {
        _uiState.value = _uiState.value.copy(indicaciones = valor)
    }

    fun actualizarFechaEntrega(valor: String) {
        _uiState.value = _uiState.value.copy(fechaEntrega = valor.filter { it.isDigit() || it == '/' }.take(10))
    }

    fun actualizarMetodoPago(valor: String) {
        _uiState.value = _uiState.value.copy(metodoPago = valor)
    }

    fun cambiarCantidadEnCarrito(producto: Producto, cantidad: Int) {
        val actuales = _uiState.value.carrito.toMutableList()
        val indiceExistente = actuales.indexOfFirst { it.producto.id == producto.id }
        if (cantidad <= 0) {
            if (indiceExistente >= 0) actuales.removeAt(indiceExistente)
        } else if (indiceExistente >= 0) {
            actuales[indiceExistente] = actuales[indiceExistente].copy(cantidad = cantidad)
        } else {
            actuales.add(ItemCarrito(producto, cantidad))
        }
        _uiState.value = _uiState.value.copy(carrito = actuales)
    }

    fun cantidadEnCarritoDe(productoId: String): Int {
        return _uiState.value.carrito.firstOrNull { it.producto.id == productoId }?.cantidad ?: 0
    }

    fun totalCarrito(): Double {
        return _uiState.value.carrito.sumOf { it.cantidad * it.producto.precio }
    }

    fun costoEnvio(): Double {
        return PedidoValidacion.costoEnvio(_uiState.value.tipoEntrega)
    }

    fun totalConEnvio(): Double {
        return totalCarrito() + costoEnvio()
    }

    fun formularioValido(): Boolean {
        return validar(_uiState.value) == null
    }

    private fun validar(estado: PedidoUiState): String? {
        if (estado.nombreCliente.isBlank()) return "Ingresa el nombre del cliente"
        PedidoValidacion.validarCelular(estado.telefonoCliente)?.let { return it }
        PedidoValidacion.validarDocumento(estado.tipoDocumento, estado.numeroDocumento, estado.razonSocial)?.let { return it }
        if (estado.tipoEntrega == PedidoValidacion.ENTREGA_CONSULTORA) {
            if (estado.direccion.isBlank() || estado.distrito.isBlank()) {
                return "Completa tu direccion y distrito en Perfil, Datos y direccion"
            }
        } else {
            if (estado.direccion.isBlank()) return "Ingresa la direccion de entrega del cliente"
            if (estado.distrito.isBlank()) return "Ingresa el distrito de entrega del cliente"
        }
        PedidoValidacion.validarFechaEntrega(estado.fechaEntrega)?.let { return it }
        if (estado.carrito.isEmpty()) return "Agrega al menos un producto"
        val sinStock = estado.carrito.firstOrNull { it.cantidad > it.producto.stock }
        if (sinStock != null) {
            return "No hay stock suficiente de ${sinStock.producto.nombre} (disponible: ${sinStock.producto.stock})"
        }
        return null
    }

    fun crearPedido() {
        val estado = _uiState.value
        val problema = validar(estado)
        if (problema != null) {
            _uiState.value = estado.copy(error = problema)
            return
        }
        _uiState.value = estado.copy(guardando = true, error = null)
        viewModelScope.launch {
            val items = estado.carrito.map { item ->
                ItemPedidoDto(
                    productoId = item.producto.id,
                    nombreProducto = item.producto.nombre,
                    cantidad = item.cantidad,
                    precioUnitario = item.producto.precio
                )
            }
            val datos = CrearPedidoRequest(
                nombreCliente = estado.nombreCliente.trim(),
                telefonoCliente = estado.telefonoCliente.trim(),
                direccion = PedidoValidacion.armarDireccion(estado.direccion, estado.distrito),
                items = items
            )
            val resultado = repository.crearPedido(datos)
            val pedido = resultado.getOrNull()
            if (pedido == null) {
                _uiState.value = _uiState.value.copy(
                    guardando = false,
                    error = MensajeError.de(resultado.exceptionOrNull(), "No se pudo registrar el pedido")
                )
                return@launch
            }

            estado.carrito.forEach { item ->
                productRepository.descontarStockPorVenta(item.producto.id, item.cantidad)
            }

            val ahora = System.currentTimeMillis()
            val detalle = PedidoDetalle(
                pedidoId = pedido.id,
                uidConsultora = pedido.uidConsultora.ifBlank { FirebaseAuth.getInstance().currentUser?.uid ?: "" },
                nombreCliente = estado.nombreCliente.trim(),
                telefonoCliente = estado.telefonoCliente.trim(),
                tipoDocumento = estado.tipoDocumento,
                numeroDocumento = estado.numeroDocumento,
                razonSocial = if (estado.tipoDocumento == PedidoValidacion.RUC) estado.razonSocial.trim() else "",
                tipoEntrega = estado.tipoEntrega,
                direccion = estado.direccion.trim(),
                distrito = estado.distrito.trim(),
                referencia = estado.referencia.trim(),
                indicaciones = estado.indicaciones.trim(),
                fechaEntrega = estado.fechaEntrega.trim(),
                metodoPago = estado.metodoPago,
                total = pedido.total,
                costoEnvio = PedidoValidacion.costoEnvio(estado.tipoEntrega),
                totalCobrado = pedido.total + PedidoValidacion.costoEnvio(estado.tipoEntrega),
                estado = pedido.estado,
                items = items,
                fechaCreacion = ahora,
                fechaActualizacion = ahora,
                historial = listOf(CambioEstado(pedido.estado, ahora)),
                comisionTotal = estado.carrito.sumOf { it.producto.comision * it.cantidad }
            )
            val guardado = detalleRepository.guardar(detalle)
            val avisoDetalle = guardado.exceptionOrNull()?.let {
                "El pedido se registro, pero los datos de entrega no se guardaron en Firebase: ${MensajeError.de(it, "error desconocido")}"
            }

            val uidCliente = detalle.uidConsultora
            if (uidCliente.isNotBlank()) {
                clienteRepository.registrarPedido(
                    ClienteFrecuente(
                        uidConsultora = uidCliente,
                        nombre = detalle.nombreCliente,
                        celular = detalle.telefonoCliente,
                        tipoDocumento = detalle.tipoDocumento,
                        numeroDocumento = detalle.numeroDocumento,
                        razonSocial = detalle.razonSocial,
                        direccion = if (detalle.tipoEntrega == PedidoValidacion.ENTREGA_CLIENTE) detalle.direccion else "",
                        distrito = if (detalle.tipoEntrega == PedidoValidacion.ENTREGA_CLIENTE) detalle.distrito else "",
                        referencia = if (detalle.tipoEntrega == PedidoValidacion.ENTREGA_CLIENTE) detalle.referencia else ""
                    )
                )
            }

            _uiState.value = PedidoUiState(
                cargandoLista = false,
                pedidos = listOf(pedido) + _uiState.value.pedidos,
                detalles = _uiState.value.detalles + (pedido.id to detalle),
                productosDisponibles = _uiState.value.productosDisponibles,
                ultimoPedidoId = pedido.id,
                mensaje = "Pedido registrado correctamente",
                aviso = avisoDetalle
            )
        }
    }

    fun siguienteEstadoDe(estadoActual: String): String? {
        return when (estadoActual) {
            "PENDIENTE" -> "ENVIADO"
            "ENVIADO" -> "ENTREGADO"
            else -> null
        }
    }

    fun avanzarEstado(pedido: PedidoResponse) {
        val nuevoEstado = siguienteEstadoDe(pedido.estado) ?: return
        _uiState.value = _uiState.value.copy(actualizandoId = pedido.id)
        viewModelScope.launch {
            val resultado = repository.cambiarEstado(pedido.id, nuevoEstado)
            val actualizado = resultado.getOrNull()
            if (actualizado == null) {
                _uiState.value = _uiState.value.copy(
                    actualizandoId = null,
                    error = MensajeError.de(resultado.exceptionOrNull(), "No se pudo cambiar el estado del pedido")
                )
                return@launch
            }
            val guardadoDetalle = detalleRepository.actualizarEstado(actualizado.id, actualizado.estado)
            val ahora = System.currentTimeMillis()
            val listaActualizada = _uiState.value.pedidos.map { if (it.id == actualizado.id) actualizado else it }
            val detallesActualizados = _uiState.value.detalles.toMutableMap()
            val detalleActual = detallesActualizados[actualizado.id]
            if (detalleActual != null && guardadoDetalle.isSuccess) {
                detallesActualizados[actualizado.id] = detalleActual.copy(
                    estado = actualizado.estado,
                    fechaActualizacion = ahora,
                    historial = detalleActual.historial + CambioEstado(actualizado.estado, ahora)
                )
            }
            _uiState.value = _uiState.value.copy(
                actualizandoId = null,
                pedidos = listaActualizada,
                detalles = detallesActualizados
            )
        }
    }

    fun limpiarMensaje() {
        _uiState.value = _uiState.value.copy(mensaje = null, error = null)
    }

    fun limpiarAviso() {
        _uiState.value = _uiState.value.copy(aviso = null)
    }
}
