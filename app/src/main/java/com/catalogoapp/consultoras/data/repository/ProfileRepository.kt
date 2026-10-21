package com.catalogoapp.consultoras.data.repository

import com.catalogoapp.consultoras.data.model.ConsultoraProfile
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ProfileRepository {

    private val db = FirebaseFirestore.getInstance()
    private val coleccion = db.collection("consultoras")
    private fun documento(uid: String) = coleccion.document(uid)

    suspend fun guardarPerfil(perfil: ConsultoraProfile): Result<Unit> = try {
        val datos = mapOf(
            "uid" to perfil.uid,
            "nombre" to perfil.nombre,
            "email" to perfil.email,
            "preferencias" to perfil.preferencias,
            "fotoBase64" to perfil.fotoBase64,
            "activo" to perfil.activo,
            "rol" to perfil.rol,
            "ultimaActualizacion" to perfil.ultimaActualizacion,
            "nivel" to perfil.nivel,
            "progreso" to perfil.progreso,
            "dni" to perfil.dni,
            "celular" to perfil.celular,
            "direccionEntrega" to perfil.direccionEntrega,
            "distrito" to perfil.distrito,
            "referencia" to perfil.referencia,
            "tipoComprobante" to perfil.tipoComprobante,
            "esAdmin" to FieldValue.delete()
        )
        documento(perfil.uid).set(datos, SetOptions.merge()).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun obtenerPerfil(uid: String): Result<ConsultoraProfile?> = try {
        val snapshot = documento(uid).get().await()
        val perfil = snapshot.toObject(ConsultoraProfile::class.java)

        val rolFinal = when {
            perfil?.rol != null && perfil.rol != "CONSULTORA" -> perfil.rol
            perfil?.esAdmin == true -> "DIRECTORA_COMERCIAL"
            else -> "CONSULTORA"
        }

        val perfilMigrado = perfil?.copy(rol = rolFinal)
        Result.success(perfilMigrado)
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun observarConsultoras(): Flow<List<ConsultoraProfile>> = callbackFlow {
        val registro = coleccion.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val consultoras = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(ConsultoraProfile::class.java)
            } ?: emptyList()
            trySend(consultoras)
        }
        awaitClose { registro.remove() }
    }

    suspend fun actualizarProgreso(uid: String, nivel: String, progreso: Double): Result<Unit> = try {
        documento(uid).update(mapOf("nivel" to nivel, "progreso" to progreso)).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun actualizarDatosAdmin(
        uid: String,
        nombre: String,
        dni: String,
        celular: String,
        preferencias: String,
        direccionEntrega: String,
        distrito: String,
        referencia: String
    ): Result<Unit> = try {
        documento(uid).update(
            mapOf(
                "nombre" to nombre,
                "dni" to dni,
                "celular" to celular,
                "preferencias" to preferencias,
                "direccionEntrega" to direccionEntrega,
                "distrito" to distrito,
                "referencia" to referencia,
                "ultimaActualizacion" to System.currentTimeMillis()
            )
        ).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun actualizarEstado(uid: String, activo: Boolean): Result<Unit> = try {
        documento(uid).update(mapOf("activo" to activo)).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun asignarRol(correo: String, nuevoRol: String, asignadoPor: String): Result<Unit> = try {
        val uid = obtenerUidPorCorreo(correo) ?: throw IllegalStateException("Usuario no encontrado")
        documento(uid).update(
            mapOf(
                "rol" to nuevoRol,
                "ultimaActualizacion" to System.currentTimeMillis(),
                "asignadoPor" to asignadoPor
            )
        ).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    private suspend fun obtenerUidPorCorreo(correo: String): String? = try {
        val query = coleccion.whereEqualTo("email", correo).get().await()
        query.documents.firstOrNull()?.id
    } catch (e: Exception) {
        null
    }
}
