package com.catalogoapp.consultoras.data.repository

import com.catalogoapp.consultoras.data.model.Devolucion
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

class DevolucionRepository {

    private val coleccion = FirebaseFirestore.getInstance().collection("devoluciones")

    suspend fun guardar(devolucion: Devolucion): Result<Unit> = try {
        val documento = coleccion.document()
        val terminado = withTimeoutOrNull(TIEMPO_MAXIMO_MS) {
            documento.set(devolucion.copy(id = documento.id)).await()
            true
        }
        if (terminado == true) Result.success(Unit) else Result.failure(Exception("Firebase no respondio a tiempo"))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun obtenerDeConsultora(uid: String): Result<List<Devolucion>> = try {
        val snapshot = withTimeoutOrNull(TIEMPO_MAXIMO_MS) {
            coleccion.whereEqualTo("uidConsultora", uid).get().await()
        }
        if (snapshot == null) {
            Result.failure(Exception("Firebase no respondio a tiempo"))
        } else {
            Result.success(
                snapshot.documents
                    .mapNotNull { it.toObject(Devolucion::class.java) }
                    .sortedByDescending { it.fecha }
            )
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun obtenerTodas(): Result<List<Devolucion>> = try {
        val snapshot = withTimeoutOrNull(TIEMPO_MAXIMO_MS) {
            coleccion.get().await()
        }
        if (snapshot == null) {
            Result.failure(Exception("Firebase no respondio a tiempo"))
        } else {
            Result.success(
                snapshot.documents
                    .mapNotNull { it.toObject(Devolucion::class.java) }
                    .sortedByDescending { it.fecha }
            )
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    companion object {
        private const val TIEMPO_MAXIMO_MS = 15_000L
    }
}
