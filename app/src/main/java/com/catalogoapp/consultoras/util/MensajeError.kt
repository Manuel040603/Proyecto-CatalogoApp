package com.catalogoapp.consultoras.util

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestoreException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object MensajeError {

    private const val SIN_CONEXION = "Sin conexión a internet. Revisa tu red e intenta de nuevo"

    fun de(error: Throwable?, porDefecto: String): String {
        var actual: Throwable? = error
        var vueltas = 0
        while (actual != null && vueltas < 6) {
            if (actual is UnknownHostException || actual is ConnectException ||
                actual is SocketTimeoutException || actual is FirebaseNetworkException
            ) {
                return SIN_CONEXION
            }
            if (actual is FirebaseAuthInvalidCredentialsException || actual is FirebaseAuthInvalidUserException) {
                return "Correo o contraseña incorrectos"
            }
            if (actual is FirebaseAuthUserCollisionException) {
                return "Ese correo ya está registrado"
            }
            if (actual is FirebaseAuthWeakPasswordException) {
                return "La contraseña es muy débil, usa al menos 6 caracteres"
            }
            if (actual is FirebaseTooManyRequestsException) {
                return "Demasiados intentos. Espera un momento e intenta de nuevo"
            }
            if (actual is FirebaseFirestoreException) {
                when (actual.code) {
                    FirebaseFirestoreException.Code.UNAVAILABLE -> return SIN_CONEXION
                    FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> return SIN_CONEXION
                    FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                        return "No tienes permiso para realizar esta acción"
                    else -> {}
                }
            }
            actual = actual.cause
            vueltas++
        }
        val texto = error?.message
        return when {
            texto.isNullOrBlank() -> porDefecto
            error is IllegalArgumentException && texto.contains("empty or null", ignoreCase = true) ->
                "Completa todos los campos para continuar"
            texto.contains("no respondió a tiempo") -> "El servidor tardó demasiado en responder. Revisa tu conexión e intenta de nuevo"
            texto.contains("network", ignoreCase = true) -> SIN_CONEXION
            else -> texto
        }
    }
}
