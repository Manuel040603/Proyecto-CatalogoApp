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
import com.catalogoapp.consultoras.data.repository.PedidoRepository
import com.catalogoapp.consultoras.data.repository.ProfileRepository
import com.catalogoapp.consultoras.util.AdminConfig
import com.catalogoapp.consultoras.worker.ProfileSyncWorker
import com.catalogoapp.consultoras.worker.RecordatorioPedidoWorker
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

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

    fun registrarTokenFcm() {
        viewModelScope.launch {
            try {
                val token = FirebaseMessaging.getInstance().token.await()
                pedidoRepository.registrarDispositivo(token)
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
