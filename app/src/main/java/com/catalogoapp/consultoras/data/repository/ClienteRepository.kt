package com.catalogoapp.consultoras.data.repository

import com.catalogoapp.consultoras.data.model.ClienteFrecuente
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

class ClienteRepository {

    private val coleccion = FirebaseFirestore.getInstance().collection("clientes")

    suspend fun obtenerDeConsultora(uid: String): Result<List<ClienteFrecuente>> = try {
        val snapshot = withTimeoutOrNull(TIEMPO_MAXIMO_MS) {
            coleccion.whereEqualTo("uidConsultora", uid).get().await()
        }
        if (snapshot == null) {
            Result.failure(Exception("Firebase no respondió a tiempo"))
        } else {
            Result.success(
                snapshot.documents
                    .mapNotNull { it.toObject(ClienteFrecuente::class.java) }
                    .sortedByDescending { it.ultimoPedido }
                    .take(MAXIMO_MOSTRADOS)
            )
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun registrarPedido(cliente: ClienteFrecuente): Result<Unit> = try {
        val id = "${cliente.uidConsultora}_${cliente.celular}"
        val datos = mapOf(
            "id" to id,
            "uidConsultora" to cliente.uidConsultora,
            "nombre" to cliente.nombre,
            "celular" to cliente.celular,
            "tipoDocumento" to cliente.tipoDocumento,
            "numeroDocumento" to cliente.numeroDocumento,
            "razonSocial" to cliente.razonSocial,
            "direccion" to cliente.direccion,
            "distrito" to cliente.distrito,
            "referencia" to cliente.referencia,
            "ultimoPedido" to System.currentTimeMillis(),
            "totalPedidos" to FieldValue.increment(1)
        )
        val terminado = withTimeoutOrNull(TIEMPO_MAXIMO_MS) {
            coleccion.document(id).set(datos, SetOptions.merge()).await()
            true
        }
        if (terminado == true) Result.success(Unit) else Result.failure(Exception("Firebase no respondió a tiempo"))
    } catch (e: Exception) {
        Result.failure(e)
    }

    companion object {
        private const val TIEMPO_MAXIMO_MS = 15_000L
        private const val MAXIMO_MOSTRADOS = 8
    }
}
