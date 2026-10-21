package com.catalogoapp.consultoras.data.remote

import com.catalogoapp.consultoras.data.model.ActualizarUbicacionRequest
import com.catalogoapp.consultoras.data.model.CrearRepartoRequest
import com.catalogoapp.consultoras.data.model.ParadaRepartoDto
import com.catalogoapp.consultoras.data.model.RepartoResponse
import com.catalogoapp.consultoras.data.model.ResumenRepartoDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface RepartoApiService {

    @POST("repartos")
    suspend fun crearReparto(@Body datos: CrearRepartoRequest): RepartoResponse

    @GET("repartos")
    suspend fun listarRepartos(): List<ResumenRepartoDto>

    @GET("repartos/{id}")
    suspend fun obtenerReparto(@Path("id") id: Int): RepartoResponse

    @PATCH("repartos/{id}/ubicacion")
    suspend fun actualizarUbicacion(@Path("id") id: Int, @Body datos: ActualizarUbicacionRequest)

    @PATCH("repartos/{id}/paradas/{paradaId}/entregar")
    suspend fun marcarParadaEntregada(@Path("id") id: Int, @Path("paradaId") paradaId: Int): ParadaRepartoDto
}
