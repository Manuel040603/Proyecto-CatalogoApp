package com.catalogoapp.consultoras.data.remote

import com.catalogoapp.consultoras.BuildConfig
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

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
