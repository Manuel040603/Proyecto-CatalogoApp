package com.catalogoapp.consultoras.data.repository

import com.catalogoapp.consultoras.data.model.ActualizarUbicacionRequest
import com.catalogoapp.consultoras.data.model.CambiarEstadoRequest
import com.catalogoapp.consultoras.data.model.CambioEstado
import com.catalogoapp.consultoras.data.model.Campana
import com.catalogoapp.consultoras.data.model.ClienteFrecuente
import com.catalogoapp.consultoras.data.model.CrearPedidoRequest
import com.catalogoapp.consultoras.data.model.CrearRepartoRequest
import com.catalogoapp.consultoras.data.model.Devolucion
import com.catalogoapp.consultoras.data.model.ParadaRepartoDto
import com.catalogoapp.consultoras.data.model.PedidoDetalle
import com.catalogoapp.consultoras.data.model.PedidoResponse
import com.catalogoapp.consultoras.data.model.RegistrarDispositivoRequest
import com.catalogoapp.consultoras.data.model.Repartidor
import com.catalogoapp.consultoras.data.model.RepartoResponse
import com.catalogoapp.consultoras.data.model.ResumenRepartoDto
import com.catalogoapp.consultoras.data.remote.PedidoApiService
import com.catalogoapp.consultoras.data.remote.RepartoApiService
import com.catalogoapp.consultoras.data.remote.RetrofitClient
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

class PedidoRepository(
    private val api: PedidoApiService = RetrofitClient.pedidoApiService
) {

    suspend fun crearPedido(datos: CrearPedidoRequest): Result<PedidoResponse> = try {
        Result.success(api.crearPedido(datos))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun listarPedidos(): Result<List<PedidoResponse>> = try {
        Result.success(api.listarPedidos())
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun cambiarEstado(id: Int, nuevoEstado: String): Result<PedidoResponse> = try {
        Result.success(api.cambiarEstado(id, CambiarEstadoRequest(nuevoEstado)))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun listarPendientesAntiguos(dias: Int): Result<List<PedidoResponse>> = try {
        Result.success(api.listarPendientesAntiguos(dias))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun registrarDispositivo(token: String): Result<Unit> = try {
        api.registrarDispositivo(RegistrarDispositivoRequest(token))
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

class RepartoRepository(
    private val api: RepartoApiService = RetrofitClient.repartoApiService
) {

    suspend fun crearReparto(datos: CrearRepartoRequest): Result<RepartoResponse> = try {
        Result.success(api.crearReparto(datos))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun listarRepartos(): Result<List<ResumenRepartoDto>> = try {
        Result.success(api.listarRepartos())
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun obtenerReparto(id: Int): Result<RepartoResponse> = try {
        Result.success(api.obtenerReparto(id))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun actualizarUbicacion(id: Int, latitud: Double, longitud: Double): Result<Unit> = try {
        api.actualizarUbicacion(id, ActualizarUbicacionRequest(latitud, longitud))
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun marcarParadaEntregada(idReparto: Int, idParada: Int): Result<ParadaRepartoDto> = try {
        Result.success(api.marcarParadaEntregada(idReparto, idParada))
    } catch (e: Exception) {
        Result.failure(e)
    }
}

class PedidoDetalleRepository {

    private val coleccion = FirebaseFirestore.getInstance().collection("pedidos")

    suspend fun guardar(detalle: PedidoDetalle): Result<Unit> = try {
        val terminado = withTimeoutOrNull(TIEMPO_MAXIMO_MS) {
            coleccion.document(detalle.pedidoId.toString()).set(detalle).await()
            true
        }
        if (terminado == true) Result.success(Unit) else Result.failure(Exception("Firebase no respondio a tiempo"))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun obtenerDeConsultora(uid: String): Result<Map<Int, PedidoDetalle>> = try {
        val snapshot = withTimeoutOrNull(TIEMPO_MAXIMO_MS) {
            coleccion.whereEqualTo("uidConsultora", uid).get().await()
        }
        if (snapshot == null) {
            Result.failure(Exception("Firebase no respondio a tiempo"))
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
            Result.failure(Exception("Firebase no respondio a tiempo"))
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
        if (terminado == true) Result.success(Unit) else Result.failure(Exception("Firebase no respondio a tiempo"))
    } catch (e: Exception) {
        Result.failure(e)
    }

    companion object {
        private const val TIEMPO_MAXIMO_MS = 15_000L
    }
}

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

class ClienteRepository {

    private val coleccion = FirebaseFirestore.getInstance().collection("clientes")

    suspend fun obtenerDeConsultora(uid: String): Result<List<ClienteFrecuente>> = try {
        val snapshot = withTimeoutOrNull(TIEMPO_MAXIMO_MS) {
            coleccion.whereEqualTo("uidConsultora", uid).get().await()
        }
        if (snapshot == null) {
            Result.failure(Exception("Firebase no respondio a tiempo"))
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
        if (terminado == true) Result.success(Unit) else Result.failure(Exception("Firebase no respondio a tiempo"))
    } catch (e: Exception) {
        Result.failure(e)
    }

    companion object {
        private const val TIEMPO_MAXIMO_MS = 15_000L
        private const val MAXIMO_MOSTRADOS = 8
    }
}

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
