package com.catalogoapp.consultoras.data.remote

import com.catalogoapp.consultoras.BuildConfig
import com.catalogoapp.consultoras.data.model.ActualizarUbicacionRequest
import com.catalogoapp.consultoras.data.model.CambiarEstadoRequest
import com.catalogoapp.consultoras.data.model.CrearPedidoRequest
import com.catalogoapp.consultoras.data.model.CrearRepartoRequest
import com.catalogoapp.consultoras.data.model.ParadaRepartoDto
import com.catalogoapp.consultoras.data.model.PedidoResponse
import com.catalogoapp.consultoras.data.model.RegistrarDispositivoRequest
import com.catalogoapp.consultoras.data.model.RepartoResponse
import com.catalogoapp.consultoras.data.model.ResumenRepartoDto
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import java.util.concurrent.TimeUnit
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

class FirebaseAuthInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val solicitudOriginal = chain.request()
        val usuario = FirebaseAuth.getInstance().currentUser
        if (usuario == null) {
            return chain.proceed(solicitudOriginal)
        }
        val token = try {
            Tasks.await(usuario.getIdToken(false)).token
        } catch (e: Exception) {
            null
        }
        val solicitudFinal = if (token != null) {
            solicitudOriginal.newBuilder().addHeader("Authorization", "Bearer $token").build()
        } else {
            solicitudOriginal
        }
        return chain.proceed(solicitudFinal)
    }
}

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

object RetrofitClient {

    private val clienteHttp: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(FirebaseAuthInterceptor())
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            .connectTimeout(90, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(90, TimeUnit.SECONDS)
            .build()
    }

    val pedidoApiService: PedidoApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.PEDIDOS_BASE_URL)
            .client(clienteHttp)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PedidoApiService::class.java)
    }

    val repartoApiService: RepartoApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.PEDIDOS_BASE_URL)
            .client(clienteHttp)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RepartoApiService::class.java)
    }
}
