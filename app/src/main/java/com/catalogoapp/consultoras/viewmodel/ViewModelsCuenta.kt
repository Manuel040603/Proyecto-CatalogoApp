package com.catalogoapp.consultoras.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.catalogoapp.consultoras.data.model.ConsultoraProfile
import com.catalogoapp.consultoras.data.repository.AdminRepository
import com.catalogoapp.consultoras.data.repository.AuthRepository
import com.catalogoapp.consultoras.data.repository.PedidoRepository
import com.catalogoapp.consultoras.data.repository.ProfileRepository
import com.catalogoapp.consultoras.util.AdminConfig
import com.catalogoapp.consultoras.util.MensajeError
import com.catalogoapp.consultoras.util.PedidoValidacion
import com.catalogoapp.consultoras.worker.ProfileSyncWorker
import com.catalogoapp.consultoras.worker.RecordatorioPedidoWorker
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.messaging.FirebaseMessaging
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

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

sealed class ProfileUiState {
    data object Idle : ProfileUiState()
    data object Guardando : ProfileUiState()
    data class Guardado(val mensaje: String) : ProfileUiState()
    data class Error(val mensaje: String) : ProfileUiState()
}

data class ProfileStats(
    val cargando: Boolean = true,
    val ventasTotales: Double = 0.0,
    val totalPedidos: Int = 0,
    val pedidosEntregados: Int = 0
)

class ProfileViewModel(
    private val repository: ProfileRepository = ProfileRepository(),
    private val pedidoRepository: PedidoRepository = PedidoRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Idle)
    val uiState: StateFlow<ProfileUiState> = _uiState

    private val _perfil = MutableStateFlow<ConsultoraProfile?>(null)
    val perfil: StateFlow<ConsultoraProfile?> = _perfil

    private val _estadisticas = MutableStateFlow(ProfileStats())
    val estadisticas: StateFlow<ProfileStats> = _estadisticas

    fun cargarPerfil(uid: String) {
        _perfil.value = null
        viewModelScope.launch {
            val resultado = repository.obtenerPerfil(uid)
            resultado.onSuccess { perfilEncontrado ->
                _perfil.value = perfilEncontrado
            }
        }
    }

    fun cargarEstadisticas() {
        _estadisticas.value = ProfileStats()
        viewModelScope.launch {
            val resultado = pedidoRepository.listarPedidos()
            _estadisticas.value = resultado.fold(
                onSuccess = { pedidos ->
                    ProfileStats(
                        cargando = false,
                        ventasTotales = pedidos.sumOf { it.total },
                        totalPedidos = pedidos.size,
                        pedidosEntregados = pedidos.count { it.estado == "ENTREGADO" }
                    )
                },
                onFailure = { ProfileStats(cargando = false) }
            )
        }
    }

    fun guardarPerfil(
        context: Context,
        uid: String,
        email: String,
        nombre: String,
        preferencias: String,
        fotoBase64: String,
        dni: String? = null,
        celular: String? = null,
        direccionEntrega: String? = null,
        distrito: String? = null,
        referencia: String? = null,
        tipoComprobante: String? = null
    ) {
        _uiState.value = ProfileUiState.Guardando
        viewModelScope.launch {
            val existente = _perfil.value?.takeIf { it.uid == uid }
                ?: repository.obtenerPerfil(uid).getOrNull()
                ?: ConsultoraProfile(uid = uid)
            val perfil = existente.copy(
                uid = uid,
                nombre = nombre,
                email = email,
                preferencias = preferencias,
                fotoBase64 = fotoBase64,
                esAdmin = AdminConfig.esAdmin(email),
                dni = dni ?: existente.dni,
                celular = celular ?: existente.celular,
                direccionEntrega = direccionEntrega ?: existente.direccionEntrega,
                distrito = distrito ?: existente.distrito,
                referencia = referencia ?: existente.referencia,
                tipoComprobante = tipoComprobante ?: existente.tipoComprobante,
                ultimaActualizacion = System.currentTimeMillis()
            )
            val resultado = repository.guardarPerfil(perfil)

            _uiState.value = resultado.fold(
                onSuccess = {
                    _perfil.value = perfil
                    encolarSincronizacion(context, uid, nombre, email, preferencias)
                    ProfileUiState.Guardado("Perfil guardado y sincronizandose en segundo plano")
                },
                onFailure = { ProfileUiState.Error(it.message ?: "No se pudo guardar el perfil") }
            )
        }
    }

    private fun encolarSincronizacion(
        context: Context,
        uid: String,
        nombre: String,
        email: String,
        preferencias: String
    ) {
        val datos = Data.Builder()
            .putString(ProfileSyncWorker.KEY_UID, uid)
            .putString(ProfileSyncWorker.KEY_NOMBRE, nombre)
            .putString(ProfileSyncWorker.KEY_EMAIL, email)
            .putString(ProfileSyncWorker.KEY_PREFERENCIAS, preferencias)
            .build()

        val solicitud = OneTimeWorkRequestBuilder<ProfileSyncWorker>()
            .setInputData(datos)
            .build()

        WorkManager.getInstance(context).enqueue(solicitud)
    }

    private var uidConTokenRegistrado: String? = null

    fun registrarTokenFcm() {
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
        if (uidConTokenRegistrado == uid) return
        viewModelScope.launch {
            try {
                val token = FirebaseMessaging.getInstance().token.await()
                val resultado = pedidoRepository.registrarDispositivo(token)
                if (resultado.isSuccess) uidConTokenRegistrado = uid
            } catch (e: Exception) {
            }
        }
    }

    fun programarRecordatorioPedidos(context: Context) {
        val solicitud = PeriodicWorkRequestBuilder<RecordatorioPedidoWorker>(1, TimeUnit.DAYS).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            RecordatorioPedidoWorker.NOMBRE_TRABAJO,
            ExistingPeriodicWorkPolicy.KEEP,
            solicitud
        )
    }
}

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
