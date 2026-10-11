package com.catalogoapp.consultoras.data.repository

import com.catalogoapp.consultoras.data.model.Repartidor
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class RepartidorRepository {

    private val coleccion = FirebaseFirestore.getInstance().collection("repartidores")

    fun observar(): Flow<List<Repartidor>> = callbackFlow {
        val registro = coleccion.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val repartidores = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(Repartidor::class.java)?.copy(id = doc.id)
            }?.sortedBy { it.nombre.lowercase() } ?: emptyList()
            trySend(repartidores)
        }
        awaitClose { registro.remove() }
    }

    suspend fun agregar(repartidor: Repartidor): Result<Unit> = try {
        val documento = coleccion.document()
        documento.set(repartidor.copy(id = documento.id)).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun cambiarActivo(id: String, activo: Boolean): Result<Unit> = try {
        coleccion.document(id).update("activo", activo).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
