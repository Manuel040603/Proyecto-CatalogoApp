package com.catalogoapp.consultoras.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.Calendar

object HoraLima {

    val zona: TimeZone = TimeZone.getTimeZone("America/Lima")

    fun calendario(): Calendar = Calendar.getInstance(zona)

    fun formatear(milisegundos: Long): String {
        val formato = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US)
        formato.timeZone = zona
        return formato.format(Date(milisegundos))
    }

    fun desdeServidor(texto: String): String {
        if (texto.isBlank()) return ""
        val respaldo = texto.replace('T', ' ').take(16)
        return try {
            val entrada = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US)
            entrada.timeZone = TimeZone.getTimeZone("UTC")
            val fecha = entrada.parse(texto.take(16))
            if (fecha == null) respaldo else formatear(fecha.time)
        } catch (e: Exception) {
            respaldo
        }
    }
}
