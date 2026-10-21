package com.catalogoapp.consultoras.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.catalogoapp.consultoras.data.model.Devolucion
import com.catalogoapp.consultoras.data.model.Producto
import com.catalogoapp.consultoras.data.repository.DevolucionRepository
import com.catalogoapp.consultoras.data.repository.ProductRepository
import com.catalogoapp.consultoras.util.AdminConfig
import com.catalogoapp.consultoras.util.MensajeError
import com.google.firebase.auth.FirebaseAuth
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

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

                val etiquetaTexto = mejor?.text?.lowercase() ?: ""
                val palabrasNoAptas = listOf("broken", "damaged", "dirty", "crack", "torn", "roto", "dañado", "sucio")
                val tieneDanio = palabrasNoAptas.any { etiquetaTexto.contains(it) }

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
