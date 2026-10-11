package com.catalogoapp.consultoras.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.catalogoapp.consultoras.data.model.Producto
import com.catalogoapp.consultoras.data.repository.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

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
