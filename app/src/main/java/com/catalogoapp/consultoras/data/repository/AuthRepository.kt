package com.catalogoapp.consultoras.data.repository

import com.catalogoapp.consultoras.data.model.ConsultoraProfile
import com.catalogoapp.consultoras.util.AdminConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepository {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    suspend fun registrar(email: String, password: String): Result<Unit> = try {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        result.user?.sendEmailVerification()?.await()
        auth.signOut()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun iniciarSesion(email: String, password: String): Result<FirebaseUser> = try {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        val user = result.user ?: throw IllegalStateException("Usuario no encontrado")
        if (!user.isEmailVerified && !AdminConfig.sinVerificacion(user.email)) {
            try {
                user.sendEmailVerification().await()
            } catch (e: Exception) {
            }
            auth.signOut()
            throw IllegalStateException("Debes verificar tu correo para entrar. Te reenviamos el mensaje de verificacion.")
        }
        val perfil = FirebaseFirestore.getInstance().collection("consultoras").document(user.uid).get().await()
            .toObject(ConsultoraProfile::class.java)
        if (perfil != null && !perfil.activo) {
            auth.signOut()
            throw IllegalStateException("Tu cuenta fue dada de baja. Comunicate con la administracion.")
        }
        Result.success(user)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun enviarCorreoRecuperacion(email: String): Result<Unit> = try {
        auth.sendPasswordResetEmail(email).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun sesionValida(): Boolean {
        val user = auth.currentUser ?: return false
        if (user.isEmailVerified || AdminConfig.sinVerificacion(user.email)) return true
        auth.signOut()
        return false
    }

    fun cerrarSesion() {
        auth.signOut()
    }
}
