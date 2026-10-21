package com.catalogoapp.consultoras.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.catalogoapp.consultoras.data.model.Producto
import com.catalogoapp.consultoras.data.repository.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

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
