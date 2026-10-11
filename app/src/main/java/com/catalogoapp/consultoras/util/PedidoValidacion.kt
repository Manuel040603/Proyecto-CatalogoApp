package com.catalogoapp.consultoras.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

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
                "El celular debe tener 9 digitos y empezar con 9"
            else -> null
        }
    }

    fun validarDocumento(tipo: String, numero: String, razonSocial: String): String? {
        return when (tipo) {
            DNI -> if (numero.length == 8 && numero.all { it.isDigit() }) null else "El DNI debe tener 8 digitos"
            RUC -> when {
                !rucValido(numero) -> "El RUC no es valido, revisa los 11 digitos"
                razonSocial.isBlank() -> "Ingresa la razon social del RUC"
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
        formato.isLenient = false
        return formato
    }

    fun fechaDesdeHoy(dias: Int): String {
        val calendario = Calendar.getInstance()
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
