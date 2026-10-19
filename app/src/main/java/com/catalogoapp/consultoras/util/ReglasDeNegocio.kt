package com.catalogoapp.consultoras.util

import android.graphics.Bitmap
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.tasks.await

object PedidoValidacion {

    const val SIN_DOCUMENTO = ""
    const val DNI = "DNI"
    const val RUC = "RUC"

    const val ENTREGA_CLIENTE = "Al cliente"
    const val ENTREGA_CONSULTORA = "A la consultora"

    const val COSTO_ENVIO_CLIENTE = 5.0

    val METODOS_PAGO = listOf("Contra entrega", "Yape", "Tarjeta")

    fun costoEnvio(tipoEntrega: String): Double {
        return if (tipoEntrega == ENTREGA_CLIENTE) COSTO_ENVIO_CLIENTE else 0.0
    }

    fun soloDigitos(texto: String, maximo: Int): String = texto.filter { it.isDigit() }.take(maximo)

    fun largoDocumento(tipo: String): Int = if (tipo == RUC) 11 else 8

    fun validarCelular(celular: String): String? {
        val limpio = celular.trim()
        return when {
            limpio.isEmpty() -> "Ingresa el celular del cliente"
            limpio.length != 9 || !limpio.all { it.isDigit() } || limpio[0] != '9' ->
                "El celular debe tener 9 dígitos y empezar con 9"
            else -> null
        }
    }

    fun validarDocumento(tipo: String, numero: String, razonSocial: String): String? {
        return when (tipo) {
            DNI -> if (numero.length == 8 && numero.all { it.isDigit() }) null else "El DNI debe tener 8 dígitos"
            RUC -> when {
                !rucValido(numero) -> "El RUC no es válido, revisa los 11 dígitos"
                razonSocial.isBlank() -> "Ingresa la razón social del RUC"
                else -> null
            }
            else -> null
        }
    }

    fun rucValido(ruc: String): Boolean {
        if (ruc.length != 11 || !ruc.all { it.isDigit() }) return false
        val prefijo = ruc.substring(0, 2)
        if (prefijo != "10" && prefijo != "15" && prefijo != "17" && prefijo != "20") return false
        val pesos = intArrayOf(5, 4, 3, 2, 7, 6, 5, 4, 3, 2)
        var suma = 0
        for (i in 0 until 10) {
            suma += (ruc[i] - '0') * pesos[i]
        }
        var digito = 11 - (suma % 11)
        if (digito == 10) digito = 0
        if (digito == 11) digito = 1
        return digito == (ruc[10] - '0')
    }

    private fun formatoFecha(): SimpleDateFormat {
        val formato = SimpleDateFormat("dd/MM/yyyy", Locale.US)
        formato.timeZone = HoraLima.zona
        formato.isLenient = false
        return formato
    }

    fun fechaDesdeHoy(dias: Int): String {
        val calendario = HoraLima.calendario()
        calendario.add(Calendar.DAY_OF_YEAR, dias)
        return formatoFecha().format(calendario.time)
    }

    fun validarFechaEntrega(texto: String): String? {
        if (texto.isBlank()) return null
        val fecha = try {
            formatoFecha().parse(texto.trim())
        } catch (e: Exception) {
            null
        }
        if (fecha == null) return "La fecha de entrega debe tener el formato dd/mm/aaaa"
        val hoy = formatoFecha().parse(fechaDesdeHoy(0))
        return if (hoy != null && fecha.before(hoy)) "La fecha de entrega no puede ser anterior a hoy" else null
    }

    fun armarDireccion(direccion: String, distrito: String): String {
        val calle = direccion.trim()
        val zona = distrito.trim()
        return if (zona.isEmpty()) calle else "$calle, $zona"
    }
}

object NivelConsultora {

    const val UMBRAL_ORO = 1000.0
    const val UMBRAL_PLATINO = 3000.0

    data class Resultado(
        val nivel: String,
        val progreso: Double,
        val siguiente: String?,
        val faltante: Double
    )

    fun calcular(ventasEntregadas: Double): Resultado {
        val ventas = if (ventasEntregadas < 0.0) 0.0 else ventasEntregadas
        return when {
            ventas >= UMBRAL_PLATINO -> Resultado("Platino", 1.0, null, 0.0)
            ventas >= UMBRAL_ORO -> Resultado(
                nivel = "Oro",
                progreso = (ventas - UMBRAL_ORO) / (UMBRAL_PLATINO - UMBRAL_ORO),
                siguiente = "Platino",
                faltante = UMBRAL_PLATINO - ventas
            )
            else -> Resultado("Plata", ventas / UMBRAL_ORO, "Oro", UMBRAL_ORO - ventas)
        }
    }

    fun mensaje(resultado: Resultado): String {
        return if (resultado.siguiente == null) {
            "Alcanzaste el nivel ${resultado.nivel}"
        } else {
            "Te faltan S/ ${"%.2f".format(resultado.faltante)} para Nivel ${resultado.siguiente}"
        }
    }
}

object Inventario {

    const val STOCK_BAJO = 5

    fun esBajo(stock: Int): Boolean = stock in 1..STOCK_BAJO

    fun agotado(stock: Int): Boolean = stock <= 0

    fun etiqueta(stock: Int): String? {
        return when {
            agotado(stock) -> "Agotado"
            esBajo(stock) -> "Quedan $stock"
            else -> null
        }
    }
}

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

object BusquedaPorFoto {

    data class Deteccion(val etiquetas: List<String>, val palabraClave: String?)

    private val TRADUCCIONES = listOf(
        "lipstick" to "labial",
        "lip gloss" to "labial",
        "lip" to "labial",
        "perfume" to "perfume",
        "fragrance" to "fragancia",
        "cologne" to "perfume",
        "candle" to "vela",
        "wax" to "vela",
        "foundation" to "base",
        "makeup" to "maquillaje",
        "cosmetics" to "maquillaje",
        "eye shadow" to "maquillaje",
        "mascara" to "maquillaje",
        "eyeliner" to "maquillaje",
        "face powder" to "maquillaje",
        "nail polish" to "maquillaje",
        "cream" to "crema",
        "lotion" to "crema",
        "moisturizer" to "crema",
        "skin care" to "facial",
        "serum" to "serum",
        "skin" to "facial"
    )

    suspend fun analizar(bitmap: Bitmap): Deteccion {
        val etiquetador = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
        val etiquetas = etiquetador.process(InputImage.fromBitmap(bitmap, 0)).await()
            .filter { it.confidence >= 0.5f }
            .sortedByDescending { it.confidence }
            .map { it.text }
        var palabra: String? = null
        for (etiqueta in etiquetas) {
            val coincidencia = TRADUCCIONES.firstOrNull { etiqueta.lowercase().contains(it.first) }
            if (coincidencia != null) {
                palabra = coincidencia.second
                break
            }
        }
        return Deteccion(etiquetas, palabra)
    }
}
