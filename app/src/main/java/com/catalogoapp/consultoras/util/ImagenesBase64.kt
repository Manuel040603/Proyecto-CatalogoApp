package com.catalogoapp.consultoras.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.LruCache
import java.io.ByteArrayOutputStream

private val cacheBitmaps = object : LruCache<String, Bitmap>(
    ((Runtime.getRuntime().maxMemory() / 1024L) / 12L).toInt()
) {
    override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
}

fun bitmapABase64(bitmap: Bitmap, lado: Int = 300, calidad: Int = 70): String {
    val redimensionado = Bitmap.createScaledBitmap(bitmap, lado, lado, true)
    val outputStream = ByteArrayOutputStream()
    redimensionado.compress(Bitmap.CompressFormat.JPEG, calidad, outputStream)
    return Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
}

fun base64ABitmap(base64: String, ladoMaximo: Int = 600): Bitmap? {
    val clave = "${base64.length}:${base64.hashCode()}:$ladoMaximo"
    val enCache = cacheBitmaps.get(clave)
    if (enCache != null) return enCache
    return try {
        val bytes = Base64.decode(base64, Base64.DEFAULT)
        val limites = BitmapFactory.Options()
        limites.inJustDecodeBounds = true
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, limites)
        var muestra = 1
        while (limites.outWidth / (muestra * 2) >= ladoMaximo && limites.outHeight / (muestra * 2) >= ladoMaximo) {
            muestra *= 2
        }
        val opciones = BitmapFactory.Options()
        opciones.inSampleSize = muestra
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opciones)
        if (bitmap != null) cacheBitmaps.put(clave, bitmap)
        bitmap
    } catch (e: Exception) {
        null
    }
}
