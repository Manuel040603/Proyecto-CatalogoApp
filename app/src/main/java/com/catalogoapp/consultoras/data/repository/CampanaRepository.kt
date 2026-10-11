package com.catalogoapp.consultoras.data.repository

import com.catalogoapp.consultoras.data.model.Campana
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class CampanaRepository {

    private val documento = FirebaseFirestore.getInstance().collection("campanas").document("actual")

    fun observar(): Flow<Campana?> = callbackFlow {
        val registro = documento.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            trySend(snapshot?.toObject(Campana::class.java))
        }
        awaitClose { registro.remove() }
    }

    suspend fun guardar(campana: Campana): Result<Unit> = try {
        documento.set(campana).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
