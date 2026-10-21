package com.catalogoapp.consultoras.data.repository

import com.catalogoapp.consultoras.data.model.Capacitacion
import com.catalogoapp.consultoras.data.model.ProgresoCapacitacion
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class CapacitacionRepository {

    private val db = FirebaseFirestore.getInstance()
    private val coleccionCapacitaciones = db.collection("capacitaciones")
    private val coleccionProgreso = db.collection("progreso_capacitaciones")

    fun observarCapacitaciones(): Flow<List<Capacitacion>> = callbackFlow {
        val registro = coleccionCapacitaciones.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val capacitaciones = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(Capacitacion::class.java)?.copy(id = doc.id)
            } ?: emptyList()
            trySend(capacitaciones)
        }
        awaitClose { registro.remove() }
    }

    suspend fun crearCapacitacion(capacitacion: Capacitacion): Result<Unit> = try {
        val doc = coleccionCapacitaciones.document()
        doc.set(capacitacion.copy(id = doc.id)).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun guardarProgreso(progreso: ProgresoCapacitacion): Result<Unit> = try {
        coleccionProgreso.document().set(progreso).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun observarProgresoDe(uid: String): Flow<List<ProgresoCapacitacion>> = callbackFlow {
        val registro = coleccionProgreso.whereEqualTo("uid", uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val progresos = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(ProgresoCapacitacion::class.java)
            } ?: emptyList()
            trySend(progresos)
        }
        awaitClose { registro.remove() }
    }
}
