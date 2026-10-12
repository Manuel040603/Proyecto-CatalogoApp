package com.catalogoapp.consultoras.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.catalogoapp.consultoras.data.model.CambioEstado
import com.catalogoapp.consultoras.data.model.ClienteFrecuente
import com.catalogoapp.consultoras.data.model.CrearPedidoRequest
import com.catalogoapp.consultoras.data.model.CrearRepartoRequest
import com.catalogoapp.consultoras.data.model.ItemPedidoDto
import com.catalogoapp.consultoras.data.model.ParadaRepartoDto
import com.catalogoapp.consultoras.data.model.PedidoDetalle
import com.catalogoapp.consultoras.data.model.PedidoResponse
import com.catalogoapp.consultoras.data.model.Producto
import com.catalogoapp.consultoras.data.model.Repartidor
import com.catalogoapp.consultoras.data.model.RepartoResponse
import com.catalogoapp.consultoras.data.model.ResumenRepartoDto
import com.catalogoapp.consultoras.data.repository.ClienteRepository
import com.catalogoapp.consultoras.data.repository.PedidoDetalleRepository
import com.catalogoapp.consultoras.data.repository.PedidoRepository
import com.catalogoapp.consultoras.data.repository.ProductRepository
import com.catalogoapp.consultoras.data.repository.ProfileRepository
import com.catalogoapp.consultoras.data.repository.RepartidorRepository
import com.catalogoapp.consultoras.data.repository.RepartoRepository
import com.catalogoapp.consultoras.util.AdminConfig
import com.catalogoapp.consultoras.util.MensajeError
import com.catalogoapp.consultoras.util.PedidoValidacion
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

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
                        error = "Primero completa tu dirección y distrito en Perfil, Datos y dirección"
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
                return "Completa tu dirección y distrito en Perfil, Datos y dirección"
            }
        } else {
            if (estado.direccion.isBlank()) return "Ingresa la dirección de entrega del cliente"
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
                "El pedido se registró, pero los datos de entrega no se guardaron en Firebase: ${MensajeError.de(it, "error desconocido")}"
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

private fun PedidoDetalle.aRespuesta(): PedidoResponse = PedidoResponse(
    id = pedidoId,
    uidConsultora = uidConsultora,
    nombreCliente = nombreCliente,
    telefonoCliente = telefonoCliente,
    direccion = PedidoValidacion.armarDireccion(direccion, distrito),
    total = total,
    estado = estado,
    items = items
)

class RepartoViewModel @JvmOverloads constructor(
    private val repartoRepository: RepartoRepository = RepartoRepository(),
    private val pedidoRepository: PedidoRepository = PedidoRepository(),
    private val repartidorRepository: RepartidorRepository = RepartidorRepository(),
    private val detalleRepository: PedidoDetalleRepository = PedidoDetalleRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(RepartoUiState())
    val uiState: StateFlow<RepartoUiState> = _uiState

    init {
        viewModelScope.launch {
            repartidorRepository.observar()
                .catch { excepcion -> _uiState.value = _uiState.value.copy(error = MensajeError.de(excepcion, "Ocurrió un error")) }
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
            _uiState.value = _uiState.value.copy(error = "El repartidor necesita nombre y placa del vehículo")
            return
        }
        viewModelScope.launch {
            val resultado = repartidorRepository.agregar(
                Repartidor(nombre = nombre.trim(), vehiculo = vehiculo.trim().uppercase(), telefono = telefono.trim())
            )
            resultado.exceptionOrNull()?.let { excepcion ->
                _uiState.value = _uiState.value.copy(error = MensajeError.de(excepcion, "Ocurrió un error"))
            }
        }
    }

    fun cargarPedidosDisponibles() {
        _uiState.value = _uiState.value.copy(cargandoPedidos = true)
        viewModelScope.launch {
            val correo = FirebaseAuth.getInstance().currentUser?.email
            val resultado: Result<List<PedidoResponse>> = if (AdminConfig.esAdmin(correo)) {
                detalleRepository.obtenerTodos().map { detalles -> detalles.map { it.aRespuesta() } }
            } else {
                pedidoRepository.listarPedidos()
            }
            val lista = resultado.getOrNull()
            if (lista == null) {
                _uiState.value = _uiState.value.copy(cargandoPedidos = false, error = MensajeError.de(resultado.exceptionOrNull(), "Ocurrió un error"))
                return@launch
            }

            val resumenes = repartoRepository.listarRepartos().getOrNull()
            val choferesOcupados = mutableSetOf<String>()
            val pedidosEnReparto = mutableSetOf<Int>()
            if (resumenes != null) {
                val enCurso = resumenes.filter { it.estado == "EN_CURSO" }
                enCurso.forEach { choferesOcupados.add(normalizar(it.chofer)) }
                val detalles = coroutineScope {
                    resumenes.map { resumen -> async { repartoRepository.obtenerReparto(resumen.id).getOrNull() } }.awaitAll()
                }
                detalles.forEach { detalle ->
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
                error = if (resumenes == null) "No se pudo verificar que repartidores están ocupados" else null
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
                    "Completa zona, dirección de origen y selecciona al menos un pedido"
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
                onFailure = { excepcion -> _uiState.value.copy(cargandoRepartos = false, error = MensajeError.de(excepcion, "Ocurrió un error")) }
            )
        }
    }

    fun cargarReparto(id: Int) {
        _uiState.value = _uiState.value.copy(cargandoReparto = true)
        viewModelScope.launch {
            val resultado = repartoRepository.obtenerReparto(id)
            _uiState.value = resultado.fold(
                onSuccess = { reparto -> _uiState.value.copy(cargandoReparto = false, repartoActual = reparto, error = null) },
                onFailure = { excepcion -> _uiState.value.copy(cargandoReparto = false, error = MensajeError.de(excepcion, "Ocurrió un error")) }
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
                onFailure = { excepcion -> _uiState.value.copy(actualizandoParadaId = null, error = MensajeError.de(excepcion, "Ocurrió un error")) }
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
            val consultoras = try {
                withTimeoutOrNull(10_000L) { profileRepository.observarConsultoras().first() }.orEmpty()
            } catch (e: FirebaseFirestoreException) {
                emptyList()
            }
            val nombres = consultoras.associate { it.uid to it.nombre.ifBlank { it.email } }
            _uiState.value = _uiState.value.copy(cargando = false, pedidos = pedidos, nombresConsultoras = nombres)
        }
    }

    fun filtrarPorEstado(estado: String?) {
        _uiState.value = _uiState.value.copy(filtroEstado = estado)
    }
}

data class AdminProductosUiState(
    val cargando: Boolean = true,
    val productos: List<Producto> = emptyList(),
    val trabajando: Boolean = false,
    val mensaje: String? = null,
    val error: String? = null,
    val errorEdicion: String? = null
)

class AdminProductosViewModel @JvmOverloads constructor(
    private val repository: ProductRepository = ProductRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminProductosUiState())
    val uiState: StateFlow<AdminProductosUiState> = _uiState

    init {
        viewModelScope.launch {
            repository.observarCatalogo()
                .catch { excepcion ->
                    _uiState.value = _uiState.value.copy(
                        cargando = false,
                        error = MensajeError.de(excepcion, "No se pudo cargar el catálogo")
                    )
                }
                .collect { productos ->
                    _uiState.value = _uiState.value.copy(
                        cargando = false,
                        productos = productos.sortedBy { it.nombre.lowercase() }
                    )
                }
        }
    }

    fun guardar(
        id: String,
        nombre: String,
        categoria: String,
        precio: String,
        stock: String,
        comision: String,
        descripcion: String,
        alTerminar: () -> Unit
    ) {
        val precioNumero = precio.replace(',', '.').toDoubleOrNull()
        val stockNumero = stock.toIntOrNull()
        val comisionNumero = if (comision.isBlank()) 0.0 else comision.replace(',', '.').toDoubleOrNull()
        val problema = when {
            nombre.isBlank() -> "El nombre no puede estar vacío"
            categoria.isBlank() -> "La categoría no puede estar vacía"
            precioNumero == null || precioNumero <= 0.0 -> "El precio debe ser un número mayor a 0"
            stockNumero == null || stockNumero < 0 -> "El stock debe ser un número entero, 0 o más"
            comisionNumero == null || comisionNumero < 0.0 -> "La comisión debe ser un número, 0 o más"
            precioNumero != null && comisionNumero != null && comisionNumero > precioNumero ->
                "La comisión no puede ser mayor al precio"
            else -> null
        }
        if (problema != null) {
            _uiState.value = _uiState.value.copy(errorEdicion = problema)
            return
        }
        _uiState.value = _uiState.value.copy(trabajando = true, errorEdicion = null, mensaje = null, error = null)
        viewModelScope.launch {
            val resultado = repository.actualizarProducto(
                id = id,
                nombre = nombre.trim(),
                categoria = categoria.trim(),
                precio = precioNumero ?: 0.0,
                descripcion = descripcion.trim(),
                stock = stockNumero ?: 0,
                comision = comisionNumero ?: 0.0
            )
            if (resultado.isSuccess) {
                _uiState.value = _uiState.value.copy(trabajando = false, mensaje = "Producto actualizado")
                alTerminar()
            } else {
                _uiState.value = _uiState.value.copy(
                    trabajando = false,
                    errorEdicion = MensajeError.de(resultado.exceptionOrNull(), "No se pudo guardar el producto")
                )
            }
        }
    }

    fun eliminar(id: String) {
        _uiState.value = _uiState.value.copy(trabajando = true, mensaje = null, error = null)
        viewModelScope.launch {
            val resultado = repository.eliminarProducto(id)
            _uiState.value = _uiState.value.copy(
                trabajando = false,
                mensaje = if (resultado.isSuccess) "Producto eliminado" else null,
                error = resultado.exceptionOrNull()?.let { MensajeError.de(it, "No se pudo eliminar el producto") }
            )
        }
    }

    fun limpiarErrorEdicion() {
        _uiState.value = _uiState.value.copy(errorEdicion = null)
    }
}

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
