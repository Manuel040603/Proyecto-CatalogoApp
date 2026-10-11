package com.catalogoapp.consultoras.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.catalogoapp.consultoras.data.model.Producto
import com.catalogoapp.consultoras.data.repository.ProductRepository
import com.catalogoapp.consultoras.util.MensajeError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

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
                        error = MensajeError.de(excepcion, "No se pudo cargar el catalogo")
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
            nombre.isBlank() -> "El nombre no puede estar vacio"
            categoria.isBlank() -> "La categoria no puede estar vacia"
            precioNumero == null || precioNumero <= 0.0 -> "El precio debe ser un numero mayor a 0"
            stockNumero == null || stockNumero < 0 -> "El stock debe ser un numero entero, 0 o mas"
            comisionNumero == null || comisionNumero < 0.0 -> "La comision debe ser un numero, 0 o mas"
            precioNumero != null && comisionNumero != null && comisionNumero > precioNumero ->
                "La comision no puede ser mayor al precio"
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
