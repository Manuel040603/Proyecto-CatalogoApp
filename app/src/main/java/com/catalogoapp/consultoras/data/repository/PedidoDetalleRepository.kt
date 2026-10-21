package com.catalogoapp.consultoras.data.repository

import com.catalogoapp.consultoras.data.model.CambioEstado
import com.catalogoapp.consultoras.data.model.PedidoDetalle
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

class PedidoDetalleRepository {

    private val coleccion = FirebaseFirestore.getInstance().collection("pedidos")

    suspend fun guardar(detalle: PedidoDetalle): Result<Unit> = try {
        val terminado = withTimeoutOrNull(TIEMPO_MAXIMO_MS) {
            coleccion.document(detalle.pedidoId.toString()).set(detalle).await()
            true
        }
        if (terminado == true) Result.success(Unit) else Result.failure(Exception("Firebase no respondió a tiempo"))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun obtenerDeConsultora(uid: String): Result<Map<Int, PedidoDetalle>> = try {
        val snapshot = withTimeoutOrNull(TIEMPO_MAXIMO_MS) {
            coleccion.whereEqualTo("uidConsultora", uid).get().await()
        }
        if (snapshot == null) {
            Result.failure(Exception("Firebase no respondió a tiempo"))
        } else {
            val mapa = snapshot.documents
                .mapNotNull { it.toObject(PedidoDetalle::class.java) }
                .associateBy { it.pedidoId }
            Result.success(mapa)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun obtenerTodos(): Result<List<PedidoDetalle>> = try {
        val snapshot = withTimeoutOrNull(TIEMPO_MAXIMO_MS) {
            coleccion.get().await()
        }
        if (snapshot == null) {
            Result.failure(Exception("Firebase no respondió a tiempo"))
        } else {
            Result.success(
                snapshot.documents
                    .mapNotNull { it.toObject(PedidoDetalle::class.java) }
                    .sortedByDescending { it.fechaCreacion }
            )
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun actualizarEstado(pedidoId: Int, estado: String): Result<Unit> = try {
        val terminado = withTimeoutOrNull(TIEMPO_MAXIMO_MS) {
            val ahora = System.currentTimeMillis()
            coleccion.document(pedidoId.toString())
                .update(
                    mapOf(
                        "estado" to estado,
                        "fechaActualizacion" to ahora,
                        "historial" to FieldValue.arrayUnion(CambioEstado(estado, ahora))
                    )
                )
                .await()
            true
        }
        if (terminado == true) Result.success(Unit) else Result.failure(Exception("Firebase no respondió a tiempo"))
    } catch (e: Exception) {
        Result.failure(e)
    }

    companion object {
        private const val TIEMPO_MAXIMO_MS = 15_000L
    }
}
