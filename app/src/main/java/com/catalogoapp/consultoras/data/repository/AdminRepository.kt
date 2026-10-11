package com.catalogoapp.consultoras.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AdminRepository {

    private val coleccion = FirebaseFirestore.getInstance().collection("admins")

    suspend fun listar(): Result<Set<String>> = try {
        val snapshot = coleccion.get().await()
        Result.success(snapshot.documents.map { it.id }.toSet())
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun asignar(correo: String, asignadoPor: String): Result<Unit> = try {
        val limpio = correo.trim().lowercase()
        coleccion.document(limpio).set(
            mapOf(
                "email" to limpio,
                "asignadoPor" to asignadoPor,
                "fecha" to System.currentTimeMillis()
            )
        ).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun quitar(correo: String): Result<Unit> = try {
        coleccion.document(correo.trim().lowercase()).delete().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
