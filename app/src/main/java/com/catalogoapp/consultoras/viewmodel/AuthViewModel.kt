package com.catalogoapp.consultoras.viewmodel

import androidx.lifecycle.ViewModel
import com.catalogoapp.consultoras.util.MensajeError
import androidx.lifecycle.viewModelScope
import com.catalogoapp.consultoras.data.repository.AuthRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AuthUiState {
    data object Idle : AuthUiState()
    data object Cargando : AuthUiState()
    data class Exito(val mensaje: String) : AuthUiState()
    data class Error(val mensaje: String) : AuthUiState()
}

class AuthViewModel @JvmOverloads constructor(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState

    val usuarioActual: FirebaseUser?
        get() = repository.currentUser

    fun sesionValida(): Boolean = repository.sesionValida()

    fun registrar(email: String, password: String) {
        _uiState.value = AuthUiState.Cargando
        viewModelScope.launch {
            val resultado = repository.registrar(email, password)
            _uiState.value = resultado.fold(
                onSuccess = { AuthUiState.Exito("Cuenta creada. Revisa tu correo para verificarla.") },
                onFailure = { AuthUiState.Error(MensajeError.de(it, "No se pudo registrar la cuenta")) }
            )
        }
    }

    fun iniciarSesion(email: String, password: String) {
        _uiState.value = AuthUiState.Cargando
        viewModelScope.launch {
            val resultado = repository.iniciarSesion(email, password)
            _uiState.value = resultado.fold(
                onSuccess = { AuthUiState.Exito("Bienvenida") },
                onFailure = { AuthUiState.Error(MensajeError.de(it, "Credenciales invalidas")) }
            )
        }
    }

    fun enviarCorreoRecuperacion(email: String) {
        _uiState.value = AuthUiState.Cargando
        viewModelScope.launch {
            val resultado = repository.enviarCorreoRecuperacion(email)
            _uiState.value = resultado.fold(
                onSuccess = { AuthUiState.Exito("Te enviamos un correo para recuperar tu contrasena") },
                onFailure = { AuthUiState.Error(MensajeError.de(it, "No se pudo enviar el correo")) }
            )
        }
    }

    fun resetEstado() {
        _uiState.value = AuthUiState.Idle
    }

    fun cerrarSesion() {
        repository.cerrarSesion()
    }
}
