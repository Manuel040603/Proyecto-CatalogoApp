package com.catalogoapp.consultoras.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.catalogoapp.consultoras.data.model.ConsultoraProfile
import com.catalogoapp.consultoras.data.repository.AdminRepository
import com.catalogoapp.consultoras.data.repository.ProfileRepository
import com.catalogoapp.consultoras.util.AdminConfig
import com.catalogoapp.consultoras.util.MensajeError
import com.catalogoapp.consultoras.util.PedidoValidacion
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

data class AdminConsultorasUiState(
    val cargando: Boolean = true,
    val consultoras: List<ConsultoraProfile> = emptyList(),
    val actualizando: Boolean = false,
    val error: String? = null,
    val mensaje: String? = null,
    val errorEdicion: String? = null,
    val esSuperAdmin: Boolean = false,
    val administradores: Set<String> = emptySet()
)

class AdminConsultorasViewModel(
    private val repository: ProfileRepository = ProfileRepository(),
    private val adminRepository: AdminRepository = AdminRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminConsultorasUiState())
    val uiState: StateFlow<AdminConsultorasUiState> = _uiState

    init {
        val correoActual = FirebaseAuth.getInstance().currentUser?.email
        val esSuper = AdminConfig.esSuperAdmin(correoActual)
        _uiState.value = _uiState.value.copy(esSuperAdmin = esSuper)
        if (esSuper) cargarAdministradores()
        viewModelScope.launch {
            repository.observarConsultoras()
                .catch { excepcion ->
                    _uiState.value = _uiState.value.copy(cargando = false, error = excepcion.message)
                }
                .collect { consultoras ->
                    _uiState.value = _uiState.value.copy(
                        cargando = false,
                        consultoras = consultoras.sortedBy { it.nombre.ifBlank { it.email }.lowercase() }
                    )
                }
        }
    }

    private fun cargarAdministradores() {
        viewModelScope.launch {
            val resultado = adminRepository.listar()
            resultado.getOrNull()?.let { lista ->
                _uiState.value = _uiState.value.copy(administradores = lista)
            }
        }
    }

    fun cambiarAdministrador(correo: String, esAdmin: Boolean) {
        val limpio = correo.trim().lowercase()
        if (limpio.isEmpty() || AdminConfig.esSuperAdmin(limpio)) return
        _uiState.value = _uiState.value.copy(actualizando = true, error = null, mensaje = null)
        viewModelScope.launch {
            val resultado = if (esAdmin) {
                adminRepository.asignar(limpio, FirebaseAuth.getInstance().currentUser?.email ?: "")
            } else {
                adminRepository.quitar(limpio)
            }
            if (resultado.isSuccess) {
                val nuevos = if (esAdmin) _uiState.value.administradores + limpio else _uiState.value.administradores - limpio
                _uiState.value = _uiState.value.copy(
                    actualizando = false,
                    administradores = nuevos,
                    mensaje = if (esAdmin) "$limpio ahora es administradora" else "$limpio dejo de ser administradora"
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    actualizando = false,
                    error = MensajeError.de(resultado.exceptionOrNull(), "No se pudo cambiar el rol")
                )
            }
        }
    }

    fun cambiarEstado(uid: String, activo: Boolean) {
        _uiState.value = _uiState.value.copy(actualizando = true, error = null, mensaje = null)
        viewModelScope.launch {
            val resultado = repository.actualizarEstado(uid, activo)
            _uiState.value = _uiState.value.copy(
                actualizando = false,
                error = resultado.exceptionOrNull()?.let { "No se pudo cambiar el estado: ${MensajeError.de(it, "error desconocido")}" }
            )
        }
    }

    fun validarEdicion(nombre: String, dni: String, celular: String): String? {
        return when {
            nombre.isBlank() -> "El nombre no puede estar vacio"
            dni.isNotEmpty() && dni.length != 8 -> "El DNI debe tener 8 digitos"
            celular.isNotEmpty() && PedidoValidacion.validarCelular(celular) != null ->
                "El celular debe tener 9 digitos y empezar con 9"
            else -> null
        }
    }

    fun guardarEdicion(
        uid: String,
        nombre: String,
        dni: String,
        celular: String,
        preferencias: String,
        direccionEntrega: String,
        distrito: String,
        referencia: String,
        alTerminar: () -> Unit
    ) {
        val problema = validarEdicion(nombre.trim(), dni, celular)
        if (problema != null) {
            _uiState.value = _uiState.value.copy(errorEdicion = problema)
            return
        }
        _uiState.value = _uiState.value.copy(actualizando = true, errorEdicion = null, mensaje = null)
        viewModelScope.launch {
            val resultado = repository.actualizarDatosAdmin(
                uid = uid,
                nombre = nombre.trim(),
                dni = dni,
                celular = celular,
                preferencias = preferencias.trim(),
                direccionEntrega = direccionEntrega.trim(),
                distrito = distrito.trim(),
                referencia = referencia.trim()
            )
            if (resultado.isSuccess) {
                _uiState.value = _uiState.value.copy(actualizando = false, mensaje = "Datos de la consultora actualizados")
                alTerminar()
            } else {
                _uiState.value = _uiState.value.copy(
                    actualizando = false,
                    errorEdicion = "No se pudo guardar: ${MensajeError.de(resultado.exceptionOrNull(), "error desconocido")}"
                )
            }
        }
    }

    fun limpiarErrorEdicion() {
        _uiState.value = _uiState.value.copy(errorEdicion = null)
    }

    fun limpiarMensaje() {
        _uiState.value = _uiState.value.copy(mensaje = null, error = null)
    }
}
