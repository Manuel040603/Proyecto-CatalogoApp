package com.catalogoapp.consultoras.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.catalogoapp.consultoras.data.model.Campana
import com.catalogoapp.consultoras.data.model.Devolucion
import com.catalogoapp.consultoras.data.model.Producto
import com.catalogoapp.consultoras.data.repository.CampanaRepository
import com.catalogoapp.consultoras.data.repository.DevolucionRepository
import com.catalogoapp.consultoras.data.repository.PedidoRepository
import com.catalogoapp.consultoras.data.repository.ProductRepository
import com.catalogoapp.consultoras.data.repository.ProfileRepository
import com.catalogoapp.consultoras.util.AdminConfig
import com.catalogoapp.consultoras.util.Inventario
import com.catalogoapp.consultoras.util.MensajeError
import com.catalogoapp.consultoras.util.NivelConsultora
import com.google.firebase.auth.FirebaseAuth
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class CatalogoUiState(
    val cargando: Boolean = true,
    val productos: List<Producto> = emptyList(),
    val categoriaSeleccionada: String? = null,
    val error: String? = null
)

class CatalogoViewModel @JvmOverloads constructor(
    private val repository: ProductRepository = ProductRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(CatalogoUiState())
    val uiState: StateFlow<CatalogoUiState> = _uiState

    private var todosLosProductos: List<Producto> = emptyList()
    private var siembraIntentada = false

    init {
        viewModelScope.launch {
            repository.observarCatalogo()
                .catch { excepcion ->
                    _uiState.value = _uiState.value.copy(cargando = false, error = excepcion.message)
                }
                .collect { productos ->
                    todosLosProductos = productos
                    aplicarFiltro()

                    if (productos.isEmpty() && !siembraIntentada) {
                        siembraIntentada = true
                        cargarProductosDeEjemplo()
                    }
                }
        }
    }

    fun filtrarPorCategoria(categoria: String?) {
        _uiState.value = _uiState.value.copy(categoriaSeleccionada = categoria)
        aplicarFiltro()
    }

    private fun aplicarFiltro() {
        val categoria = _uiState.value.categoriaSeleccionada
        val filtrados = if (categoria == null) todosLosProductos else todosLosProductos.filter { it.categoria == categoria }
        _uiState.value = _uiState.value.copy(cargando = false, productos = filtrados)
    }

    fun cargarProductosDeEjemplo() {
        viewModelScope.launch {
            repository.sembrarProductosDeEjemplo()
        }
    }

    fun obtenerProductoPorId(id: String): Producto? {
        return todosLosProductos.find { it.id == id }
    }

    fun categoriasDisponibles(): List<String> = todosLosProductos.map { it.categoria }.distinct()
}

sealed class AgregarProductoUiState {
    data object Idle : AgregarProductoUiState()
    data object Guardando : AgregarProductoUiState()
    data class Guardado(val mensaje: String) : AgregarProductoUiState()
    data class Error(val mensaje: String) : AgregarProductoUiState()
}

class AgregarProductoViewModel(
    private val repository: ProductRepository = ProductRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<AgregarProductoUiState>(AgregarProductoUiState.Idle)
    val uiState: StateFlow<AgregarProductoUiState> = _uiState

    fun guardarProducto(
        nombre: String,
        categoria: String,
        precio: Double,
        descripcion: String,
        stock: Int,
        imagenBase64: String,
        comision: Double = 0.0
    ) {
        _uiState.value = AgregarProductoUiState.Guardando
        viewModelScope.launch {
            val producto = Producto(
                nombre = nombre,
                categoria = categoria,
                precio = precio,
                descripcion = descripcion,
                stock = stock,
                imagenUrl = imagenBase64,
                comision = comision
            )
            val resultado = repository.agregarProducto(producto)
            _uiState.value = resultado.fold(
                onSuccess = { AgregarProductoUiState.Guardado("Producto agregado correctamente") },
                onFailure = { AgregarProductoUiState.Error(com.catalogoapp.consultoras.util.MensajeError.de(it, "No se pudo agregar el producto")) }
            )
        }
    }
}

data class DevolucionUiState(
    val productos: List<Producto> = emptyList(),
    val productoSeleccionado: Producto? = null,
    val foto: Bitmap? = null,
    val clasificando: Boolean = false,
    val resultadoApto: Boolean? = null,
    val etiquetaDetectada: String? = null,
    val confianza: Float? = null,
    val actualizando: Boolean = false,
    val mensaje: String? = null,
    val error: String? = null,
    val historial: List<Devolucion> = emptyList(),
    val cargandoHistorial: Boolean = true,
    val historialDeTodas: Boolean = false,
    val avisoHistorial: String? = null
)

class DevolucionViewModel @JvmOverloads constructor(
    private val repository: ProductRepository = ProductRepository(),
    private val devolucionRepository: DevolucionRepository = DevolucionRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(DevolucionUiState())
    val uiState: StateFlow<DevolucionUiState> = _uiState

    init {
        viewModelScope.launch {
            repository.observarCatalogo()
                .catch { }
                .collect { productos ->
                    _uiState.value = _uiState.value.copy(productos = productos)
                }
        }
        cargarHistorial()
    }

    fun cargarHistorial() {
        val usuario = FirebaseAuth.getInstance().currentUser
        if (usuario == null) {
            _uiState.value = _uiState.value.copy(cargandoHistorial = false, historial = emptyList())
            return
        }
        _uiState.value = _uiState.value.copy(cargandoHistorial = true)
        viewModelScope.launch {
            AdminConfig.cargarRol(usuario.email)
            val esAdmin = AdminConfig.esAdmin(usuario.email)
            _uiState.value = _uiState.value.copy(historialDeTodas = esAdmin)
            val resultado = if (esAdmin) {
                devolucionRepository.obtenerTodas()
            } else {
                devolucionRepository.obtenerDeConsultora(usuario.uid)
            }
            _uiState.value = resultado.fold(
                onSuccess = { lista ->
                    _uiState.value.copy(cargandoHistorial = false, historial = lista, avisoHistorial = null)
                },
                onFailure = {
                    _uiState.value.copy(
                        cargandoHistorial = false,
                        avisoHistorial = "No se pudo cargar el historial de devoluciones: ${MensajeError.de(it, "error desconocido")}"
                    )
                }
            )
        }
    }

    fun seleccionarProducto(producto: Producto) {
        _uiState.value = _uiState.value.copy(
            productoSeleccionado = producto,
            foto = null,
            resultadoApto = null,
            etiquetaDetectada = null,
            confianza = null,
            mensaje = null,
            error = null
        )
    }

    fun registrarFoto(bitmap: Bitmap) {
        _uiState.value = _uiState.value.copy(
            foto = bitmap,
            resultadoApto = null,
            etiquetaDetectada = null,
            confianza = null,
            mensaje = null,
            error = null
        )
    }

    fun clasificarProducto() {
        val bitmap = _uiState.value.foto ?: return
        _uiState.value = _uiState.value.copy(clasificando = true, error = null)
        viewModelScope.launch {
            try {
                val etiquetador = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
                val imagenEntrada = InputImage.fromBitmap(bitmap, 0)
                val etiquetas = etiquetador.process(imagenEntrada).await()
                val mejor = etiquetas.maxByOrNull { it.confidence }
                val confianza = mejor?.confidence ?: 0f

                // Lógica de clasificación: Consideramos APTO si la confianza es alta
                // y la etiqueta no contiene palabras relacionadas con daños
                val etiquetaTexto = mejor?.text?.lowercase() ?: ""
                val palabrasNoAptas = listOf("broken", "damaged", "dirty", "crack", "torn", "roto", "dañado", "sucio")
                val tieneDanio = palabrasNoAptas.any { etiquetaTexto.contains(it) }

                // VALIDACIÓN DE CATEGORÍA: Verificamos si la etiqueta coincide con la categoría del producto
                val categoria = _uiState.value.productoSeleccionado?.categoria?.lowercase() ?: ""
                val coincideCategoria = etiquetaTexto.contains(categoria) ||
                                       (categoria.contains("maquillaje") && etiquetaTexto.contains("lipstick")) ||
                                       (categoria.contains("fragrancias") && etiquetaTexto.contains("bottle"))

                val isCatalogProduct = coincideCategoria
                val isUndamaged = !tieneDanio
                val isConfident = confianza >= 0.75f

                val apto = isCatalogProduct && isUndamaged && isConfident

                _uiState.value = _uiState.value.copy(
                    clasificando = false,
                    resultadoApto = apto,
                    etiquetaDetectada = mejor?.text ?: "Sin detección clara",
                    confianza = confianza
                )

                // AUTOMATIZACIÓN: Si es APTO y coincide la categoría, actualizamos el inventario automáticamente
                if (apto && coincideCategoria) {
                    confirmarActualizacionInventario()
                } else if (apto && !coincideCategoria) {
                    _uiState.value = _uiState.value.copy(
                        mensaje = "Producto apto, pero la IA detectó algo diferente a la categoría ${_uiState.value.productoSeleccionado?.categoria}. Por favor, confirma manualmente."
                    )
                }
            } catch (excepcion: Exception) {
                _uiState.value = _uiState.value.copy(clasificando = false, error = excepcion.message)
            }
        }
    }

    fun confirmarActualizacionInventario() {
        val producto = _uiState.value.productoSeleccionado ?: return
        val apto = _uiState.value.resultadoApto ?: return
        val etiqueta = _uiState.value.etiquetaDetectada ?: ""
        val confianza = _uiState.value.confianza ?: 0f
        _uiState.value = _uiState.value.copy(actualizando = true, error = null)
        viewModelScope.launch {
            try {
                repository.actualizarStockPorDevolucion(producto.id, apto)
                val usuario = FirebaseAuth.getInstance().currentUser
                val registro = Devolucion(
                    uidConsultora = usuario?.uid ?: "",
                    emailConsultora = usuario?.email ?: "",
                    productoId = producto.id,
                    productoNombre = producto.nombre,
                    apto = apto,
                    etiquetaDetectada = etiqueta,
                    confianza = confianza.toDouble(),
                    stockRepuesto = apto,
                    fecha = System.currentTimeMillis()
                )
                val guardado = devolucionRepository.guardar(registro)
                val mensajeBase = if (apto) {
                    "Producto apto: se repuso 1 unidad al inventario"
                } else {
                    "Producto no apto: descartado, inventario sin cambios"
                }
                val mensaje = if (guardado.isSuccess) {
                    mensajeBase
                } else {
                    "$mensajeBase. No se pudo guardar el historial: ${guardado.exceptionOrNull()?.message}"
                }
                _uiState.value = _uiState.value.copy(actualizando = false, mensaje = mensaje)
                if (guardado.isSuccess) cargarHistorial()
            } catch (excepcion: Exception) {
                _uiState.value = _uiState.value.copy(actualizando = false, error = excepcion.message)
            }
        }
    }

    fun reiniciar() {
        _uiState.value = _uiState.value.copy(
            productoSeleccionado = null,
            foto = null,
            resultadoApto = null,
            etiquetaDetectada = null,
            confianza = null,
            mensaje = null,
            error = null
        )
    }
}

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
