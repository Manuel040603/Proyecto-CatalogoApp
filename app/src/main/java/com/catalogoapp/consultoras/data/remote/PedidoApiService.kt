package com.catalogoapp.consultoras.data.remote

import com.catalogoapp.consultoras.data.model.CambiarEstadoRequest
import com.catalogoapp.consultoras.data.model.CrearPedidoRequest
import com.catalogoapp.consultoras.data.model.PedidoResponse
import com.catalogoapp.consultoras.data.model.RegistrarDispositivoRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface PedidoApiService {

    @POST("pedidos")
    suspend fun crearPedido(@Body datos: CrearPedidoRequest): PedidoResponse

    @GET("pedidos")
    suspend fun listarPedidos(): List<PedidoResponse>

    @GET("pedidos/{id}")
    suspend fun obtenerPedido(@Path("id") id: Int): PedidoResponse

    @PATCH("pedidos/{id}/estado")
    suspend fun cambiarEstado(@Path("id") id: Int, @Body datos: CambiarEstadoRequest): PedidoResponse

    @GET("pedidos/pendientes-antiguos")
    suspend fun listarPendientesAntiguos(@Query("dias") dias: Int): List<PedidoResponse>

    @POST("dispositivos")
    suspend fun registrarDispositivo(@Body datos: RegistrarDispositivoRequest)
}
