package com.catalogoapp.consultoras.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.catalogoapp.consultoras.data.model.ConsultoraProfile
import com.catalogoapp.consultoras.data.repository.ProfileRepository
import com.catalogoapp.consultoras.util.AdminConfig

class ProfileSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val repository = ProfileRepository()

    override suspend fun doWork(): Result {
        val uid = inputData.getString(KEY_UID) ?: return Result.failure()
        val nombre = inputData.getString(KEY_NOMBRE) ?: ""
        val email = inputData.getString(KEY_EMAIL) ?: ""
        val preferencias = inputData.getString(KEY_PREFERENCIAS) ?: ""

        val existente = repository.obtenerPerfil(uid).getOrNull() ?: ConsultoraProfile(uid = uid)

        val perfil = existente.copy(
            uid = uid,
            nombre = nombre,
            email = email,
            preferencias = preferencias,
            esAdmin = AdminConfig.esAdmin(email)
        )

        val resultado = repository.guardarPerfil(perfil)
        return if (resultado.isSuccess) Result.success() else Result.retry()
    }

    companion object {
        const val KEY_UID = "uid"
        const val KEY_NOMBRE = "nombre"
        const val KEY_EMAIL = "email"
        const val KEY_PREFERENCIAS = "preferencias"
    }
}
