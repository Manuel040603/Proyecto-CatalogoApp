package com.catalogoapp.consultoras.viewmodel

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.catalogoapp.consultoras.data.repository.AuthRepository
import com.catalogoapp.consultoras.util.MensajeError
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

    private val _aviso = MutableStateFlow<String?>(null)
    val aviso: StateFlow<String?> = _aviso

    val usuarioActual: FirebaseUser?
        get() = repository.currentUser

    fun sesionValida(): Boolean = repository.sesionValida()

    fun registrar(email: String, password: String) {
        val errorLocal = when {
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "El correo no tiene un formato válido"
            password.length < 6 -> "La contraseña debe tener al menos 6 caracteres"
            else -> null
        }
        if (errorLocal != null) {
            _uiState.value = AuthUiState.Error(errorLocal)
            return
        }
        _aviso.value = null
        _uiState.value = AuthUiState.Cargando
        viewModelScope.launch {
            val resultado = repository.registrar(email, password)
            if (resultado.isSuccess) {
                _aviso.value = "Cuenta creada. Revisa tu correo para verificarla y luego inicia sesión."
            }
            _uiState.value = resultado.fold(
                onSuccess = { AuthUiState.Exito("Cuenta creada. Revisa tu correo para verificarla.") },
                onFailure = { AuthUiState.Error(MensajeError.de(it, "No se pudo registrar la cuenta")) }
            )
        }
    }

    private fun errorDeCredenciales(email: String, password: String): String? {
        return when {
            email.isBlank() -> "Ingresa tu correo electrónico"
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "El correo no tiene un formato válido"
            password.isEmpty() -> "Ingresa tu contraseña"
            else -> null
        }
    }

    fun iniciarSesion(email: String, password: String) {
        val errorLocal = errorDeCredenciales(email, password)
        if (errorLocal != null) {
            _uiState.value = AuthUiState.Error(errorLocal)
            return
        }
        _aviso.value = null
        _uiState.value = AuthUiState.Cargando
        viewModelScope.launch {
            val resultado = repository.iniciarSesion(email, password)
            _uiState.value = resultado.fold(
                onSuccess = { AuthUiState.Exito("Bienvenida") },
                onFailure = { AuthUiState.Error(MensajeError.de(it, "Credenciales inválidas")) }
            )
        }
    }

    fun enviarCorreoRecuperacion(email: String) {
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _uiState.value = AuthUiState.Error("El correo no tiene un formato válido")
            return
        }
        _uiState.value = AuthUiState.Cargando
        viewModelScope.launch {
            val resultado = repository.enviarCorreoRecuperacion(email)
            _uiState.value = resultado.fold(
                onSuccess = { AuthUiState.Exito("Te enviamos un correo para recuperar tu contraseña") },
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
