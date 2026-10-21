package com.catalogoapp.consultoras.data.repository

import com.catalogoapp.consultoras.data.model.ActualizarUbicacionRequest
import com.catalogoapp.consultoras.data.model.CrearRepartoRequest
import com.catalogoapp.consultoras.data.model.ParadaRepartoDto
import com.catalogoapp.consultoras.data.model.RepartoResponse
import com.catalogoapp.consultoras.data.model.ResumenRepartoDto
import com.catalogoapp.consultoras.data.remote.RepartoApiService
import com.catalogoapp.consultoras.data.remote.RetrofitClient

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
