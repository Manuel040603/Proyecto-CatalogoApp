package com.catalogoapp.consultoras.data.repository

import com.catalogoapp.consultoras.data.model.CambiarEstadoRequest
import com.catalogoapp.consultoras.data.model.CrearPedidoRequest
import com.catalogoapp.consultoras.data.model.PedidoResponse
import com.catalogoapp.consultoras.data.model.RegistrarDispositivoRequest
import com.catalogoapp.consultoras.data.remote.PedidoApiService
import com.catalogoapp.consultoras.data.remote.RetrofitClient

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
