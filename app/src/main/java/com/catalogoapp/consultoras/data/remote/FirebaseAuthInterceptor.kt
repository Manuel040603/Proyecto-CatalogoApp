package com.catalogoapp.consultoras.data.remote

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import okhttp3.Interceptor
import okhttp3.Response

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
