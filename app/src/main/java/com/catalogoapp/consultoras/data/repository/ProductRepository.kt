package com.catalogoapp.consultoras.data.repository

import com.catalogoapp.consultoras.data.model.Producto
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.tasks.await

class ProductRepository {

    private val coleccion = FirebaseFirestore.getInstance().collection("productos")

    fun observarCatalogo(): Flow<List<Producto>> = catalogoCompartido.map { it.getOrThrow() }

    suspend fun agregarProducto(producto: Producto): Result<Unit> = try {
        val doc = coleccion.document()
        doc.set(producto.copy(id = doc.id)).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun actualizarProducto(
        id: String,
        nombre: String,
        categoria: String,
        precio: Double,
        descripcion: String,
        stock: Int,
        comision: Double
    ): Result<Unit> = try {
        coleccion.document(id).update(
            mapOf(
                "nombre" to nombre,
                "categoria" to categoria,
                "precio" to precio,
                "descripcion" to descripcion,
                "stock" to stock,
                "comision" to comision
            )
        ).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun eliminarProducto(id: String): Result<Unit> = try {
        coleccion.document(id).delete().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun actualizarStockPorDevolucion(productoId: String, apto: Boolean) {
        if (apto) {
            coleccion.document(productoId).update("stock", FieldValue.increment(1)).await()
        }
    }

    suspend fun descontarStockPorVenta(productoId: String, cantidadVendida: Int): Result<Unit> = try {
        coleccion.document(productoId).update("stock", FieldValue.increment(-cantidadVendida.toLong())).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    companion object {
        private val ambito = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        private val catalogoCompartido: Flow<Result<List<Producto>>> = callbackFlow<Result<QuerySnapshot>> {
            val registro = FirebaseFirestore.getInstance().collection("productos")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(Result.failure(error))
                    } else if (snapshot != null) {
                        trySend(Result.success(snapshot))
                    }
                }
            awaitClose { registro.remove() }
        }
            .map { resultado ->
                resultado.mapCatching { snapshot ->
                    snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Producto::class.java)?.copy(id = doc.id)
                    }
                }
            }
            .flowOn(Dispatchers.Default)
            .shareIn(ambito, SharingStarted.WhileSubscribed(5_000L), replay = 1)
    }
}
