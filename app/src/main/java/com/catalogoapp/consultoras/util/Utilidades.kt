package com.catalogoapp.consultoras.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.LruCache
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import org.osmdroid.util.GeoPoint

object AdminConfig {
    private val CORREOS_ADMIN = setOf("admin@catalogoapp.com")

    private val CORREOS_SIN_VERIFICACION = setOf("admin@catalogoapp.com", "test@test.com")

    private val _administradores = MutableStateFlow<Set<String>>(emptySet())
    val administradores: StateFlow<Set<String>> = _administradores

    fun normalizar(email: String?): String? {
        val limpio = email?.trim()?.lowercase()
        return if (limpio.isNullOrEmpty()) null else limpio
    }

    fun sinVerificacion(email: String?): Boolean {
        val correo = normalizar(email) ?: return false
        return CORREOS_SIN_VERIFICACION.contains(correo)
    }

    fun esSuperAdmin(email: String?): Boolean {
        val correo = normalizar(email) ?: return false
        return CORREOS_ADMIN.contains(correo)
    }

    fun esAdmin(email: String?): Boolean {
        val correo = normalizar(email) ?: return false
        return CORREOS_ADMIN.contains(correo) || _administradores.value.contains(correo)
    }

    private val ultimaConsulta = mutableMapOf<String, Long>()

    suspend fun cargarRol(email: String?, forzar: Boolean = false) {
        val correo = normalizar(email) ?: return
        if (CORREOS_ADMIN.contains(correo)) return
        val ahora = System.currentTimeMillis()
        val anterior = ultimaConsulta[correo]
        if (!forzar && anterior != null && ahora - anterior < VIGENCIA_ROL_MS) return
        ultimaConsulta[correo] = ahora
        try {
            val documento = FirebaseFirestore.getInstance().collection("admins").document(correo).get().await()
            _administradores.value = if (documento.exists()) {
                _administradores.value + correo
            } else {
                _administradores.value - correo
            }
        } catch (e: Exception) {
            ultimaConsulta.remove(correo)
        }
    }

    private const val VIGENCIA_ROL_MS = 60_000L
}

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

object PolylineDecoder {

    fun decodificar(codificado: String): List<GeoPoint> {
        val puntos = mutableListOf<GeoPoint>()
        var indice = 0
        var latitud = 0
        var longitud = 0

        while (indice < codificado.length) {
            var resultado = 0
            var desplazamiento = 0
            var byte: Int
            do {
                byte = codificado[indice++].code - 63
                resultado = resultado or ((byte and 0x1f) shl desplazamiento)
                desplazamiento += 5
            } while (byte >= 0x20)
            val deltaLatitud = if ((resultado and 1) != 0) (resultado shr 1).inv() else (resultado shr 1)
            latitud += deltaLatitud

            resultado = 0
            desplazamiento = 0
            do {
                byte = codificado[indice++].code - 63
                resultado = resultado or ((byte and 0x1f) shl desplazamiento)
                desplazamiento += 5
            } while (byte >= 0x20)
            val deltaLongitud = if ((resultado and 1) != 0) (resultado shr 1).inv() else (resultado shr 1)
            longitud += deltaLongitud

            puntos.add(GeoPoint(latitud / 1e5, longitud / 1e5))
        }
        return puntos
    }
}

// Extensión para crear el DataStore
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class ThemeManager(private val context: Context) {
    companion object {
        private val THEME_KEY = stringPreferencesKey("selected_theme")
    }

    // Flujo que emite el tema seleccionado (light, dark, black, system)
    val themeFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[THEME_KEY] ?: "system"
        }

    suspend fun setTheme(theme: String) {
        context.dataStore.edit { preferences ->
            preferences[THEME_KEY] = theme
        }
    }
}
