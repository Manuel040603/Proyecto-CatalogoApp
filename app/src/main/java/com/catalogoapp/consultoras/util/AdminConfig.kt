package com.catalogoapp.consultoras.util

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await

object AdminConfig {
    private val CORREOS_ADMIN = setOf("admin@catalogoapp.com")

    private val CORREOS_SIN_VERIFICACION = setOf("admin@catalogoapp.com", "test@test.com")

    private val _administradores = MutableStateFlow<Set<String>>(emptySet())
    val administradores: StateFlow<Set<String>> = _administradores

    fun normalizar(email: String?): String? {
        val limpio = email?.trim()?.lowercase()
        return if (limpio.isNullOrEmpty()) null else limpio
    }

    fun sinVerificacion(email: String?): Boolean {
        val correo = normalizar(email) ?: return false
        return CORREOS_SIN_VERIFICACION.contains(correo)
    }

    fun esSuperAdmin(email: String?): Boolean {
        val correo = normalizar(email) ?: return false
        return CORREOS_ADMIN.contains(correo)
    }

    fun esAdmin(email: String?): Boolean {
        val correo = normalizar(email) ?: return false
        return CORREOS_ADMIN.contains(correo) || _administradores.value.contains(correo)
    }

    suspend fun cargarRol(email: String?) {
        val correo = normalizar(email) ?: return
        if (CORREOS_ADMIN.contains(correo)) return
        try {
            val documento = FirebaseFirestore.getInstance().collection("admins").document(correo).get().await()
            _administradores.value = if (documento.exists()) {
                _administradores.value + correo
            } else {
                _administradores.value - correo
            }
        } catch (e: Exception) {
        }
    }
}
