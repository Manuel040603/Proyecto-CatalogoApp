package com.catalogoapp.consultoras.ui.screens.reparto

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.catalogoapp.consultoras.data.model.ParadaRepartoDto
import com.catalogoapp.consultoras.data.repository.ProfileRepository
import com.catalogoapp.consultoras.util.AdminConfig
import com.catalogoapp.consultoras.util.HoraLima
import com.catalogoapp.consultoras.util.PolylineDecoder
import com.catalogoapp.consultoras.viewmodel.RepartoViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

@Composable
fun RepartoDetalleScreen(
    idReparto: Int,
    viewModel: RepartoViewModel,
    onVolver: () -> Unit
) {
    val context = LocalContext.current
    val estado by viewModel.uiState.collectAsState()
    val usuarioActual = FirebaseAuth.getInstance().currentUser
    var rolPerfil by remember { mutableStateOf("") }
    LaunchedEffect(usuarioActual?.uid) {
        val uid = usuarioActual?.uid ?: return@LaunchedEffect
        rolPerfil = ProfileRepository().obtenerPerfil(uid).getOrNull()?.rol ?: ""
    }
    val administradores by AdminConfig.administradores.collectAsState()
    val esOperador = remember(administradores, rolPerfil, usuarioActual?.email) {
        AdminConfig.esAdmin(usuarioActual?.email) || rolPerfil == "LOGISTICA" || rolPerfil == "SUPER_ADMIN"
    }
    var permisoUbicacion by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    LaunchedEffect(idReparto) {
        val configuracion = Configuration.getInstance()
        configuracion.userAgentValue = context.packageName
        configuracion.osmdroidBasePath = context.cacheDir
        configuracion.osmdroidTileCache = context.cacheDir
        viewModel.cargarReparto(idReparto)
    }

    LaunchedEffect(idReparto) {
        while (true) {
            delay(INTERVALO_REFRESCO_MS)
            if (viewModel.uiState.value.repartoActual?.estado == "COMPLETADO") break
            viewModel.cargarReparto(idReparto, silencioso = true)
        }
    }

    var compartir by rememberSaveable { mutableStateOf(false) }

    val lanzadorPermisoUbicacion = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { resultados ->
        val concedido = resultados[Manifest.permission.ACCESS_FINE_LOCATION] == true
        permisoUbicacion = concedido
        if (concedido) {
            obtenerUbicacionYActualizar(context, idReparto, viewModel)
        } else {
            compartir = false
        }
    }

    val compartiendoUbicacion = esOperador && compartir && permisoUbicacion &&
        estado.repartoActual?.estado != "COMPLETADO"

    DisposableEffect(compartiendoUbicacion, idReparto) {
        val detener = if (compartiendoUbicacion) {
            obtenerUbicacionYActualizar(context, idReparto, viewModel)
            iniciarUbicacionEnVivo(context, idReparto, viewModel)
        } else {
            null
        }
        onDispose { detener?.invoke() }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Reparto #$idReparto", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = onVolver) { Text("Volver") }
        }
        Spacer(modifier = Modifier.height(8.dp))

        if (estado.cargandoReparto && estado.repartoActual == null) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Column
        }

        val reparto = estado.repartoActual
        if (reparto == null) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(estado.error ?: "No se pudo cargar el reparto")
            }
            return@Column
        }

        Text("${reparto.zona} - ${reparto.chofer} - ${reparto.vehiculo}", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(8.dp))

        val latitudRepartidor = reparto.latitudActual
        val longitudRepartidor = reparto.longitudActual
        val posicionRepartidor = if (latitudRepartidor != null && longitudRepartidor != null) {
            GeoPoint(latitudRepartidor, longitudRepartidor)
        } else {
            null
        }

        MapaReparto(
            paradas = reparto.paradas,
            geometriaRuta = reparto.geometriaRuta,
            posicionRepartidor = posicionRepartidor,
            nombreRepartidor = reparto.chofer,
            modifier = Modifier.fillMaxWidth().height(280.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))

        val textoUbicacion = when {
            reparto.estado == "COMPLETADO" -> "Reparto completado"
            esOperador && compartiendoUbicacion -> "Compartiendo tu ubicación en vivo"
            posicionRepartidor != null && !reparto.fechaUbicacion.isNullOrBlank() ->
                "Ubicación del repartidor: ${HoraLima.desdeServidor(reparto.fechaUbicacion)}"
            else -> "El repartidor aún no comparte su ubicación"
        }
        Text(
            textoUbicacion,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (esOperador && reparto.estado != "COMPLETADO") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Compartir mi ubicación en vivo (soy el repartidor)",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = compartir,
                    onCheckedChange = { activado ->
                        compartir = activado
                        if (activado && !permisoUbicacion) {
                            lanzadorPermisoUbicacion.launch(PERMISOS_UBICACION)
                        }
                    }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    if (permisoUbicacion) {
                        obtenerUbicacionYActualizar(context, idReparto, viewModel)
                    } else {
                        lanzadorPermisoUbicacion.launch(PERMISOS_UBICACION)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Actualizar mi ubicación")
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        estado.error?.let { error ->
            Text(error, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))
        }

        Text("Paradas en orden de entrega", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(reparto.paradas.sortedBy { it.orden }) { parada ->
                FilaParada(
                    parada = parada,
                    actualizando = estado.actualizandoParadaId == parada.id,
                    puedeEntregar = esOperador,
                    onMarcarEntregada = { viewModel.marcarParadaEntregada(idReparto, parada.id) }
                )
            }
        }
    }
}

@Composable
private fun MapaReparto(
    paradas: List<ParadaRepartoDto>,
    geometriaRuta: String?,
    posicionRepartidor: GeoPoint?,
    nombreRepartidor: String,
    modifier: Modifier = Modifier
) {
    val formaMapa = RoundedCornerShape(16.dp)
    val modificadorContenedor = modifier
        .clip(formaMapa)
        .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), formaMapa)

    if (paradas.isEmpty()) {
        Box(modifier = modificadorContenedor, contentAlignment = Alignment.Center) {
            Text("Sin paradas para mostrar")
        }
        return
    }

    val mapViewReferencia = remember { mutableStateOf<MapView?>(null) }
    val encuadrado = remember { booleanArrayOf(false) }
    DisposableEffect(Unit) {
        onDispose {
            mapViewReferencia.value?.onDetach()
        }
    }

    AndroidView(
        modifier = modificadorContenedor,
        factory = { contexto ->
            MapView(contexto).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                mapViewReferencia.value = this
            }
        },
        update = { mapView ->
            mapView.overlays.clear()

            val paradasOrdenadas = paradas.sortedBy { it.orden }
            val puntosParadas = paradasOrdenadas.map { GeoPoint(it.latitud, it.longitud) }
            paradasOrdenadas.forEachIndexed { indice, parada ->
                val punto = GeoPoint(parada.latitud, parada.longitud)
                val marcador = Marker(mapView)
                marcador.position = punto
                marcador.title = "${indice + 1}. ${parada.nombreCliente}"
                marcador.snippet = if (parada.entregada) "Entregado" else "Pendiente"
                mapView.overlays.add(marcador)
            }

            val puntosRutaDecodificados: List<GeoPoint> = if (!geometriaRuta.isNullOrBlank()) {
                try {
                    val puntosRuta = PolylineDecoder.decodificar(geometriaRuta)
                    if (puntosRuta.isNotEmpty()) {
                        val linea = Polyline()
                        linea.setPoints(puntosRuta)
                        mapView.overlays.add(linea)
                    }
                    puntosRuta
                } catch (e: Exception) {
                    emptyList()
                }
            } else {
                emptyList()
            }

            if (posicionRepartidor != null) {
                val marcadorRepartidor = Marker(mapView)
                marcadorRepartidor.position = posicionRepartidor
                marcadorRepartidor.title = "Repartidor: $nombreRepartidor"
                marcadorRepartidor.snippet = "Ubicación en vivo"
                marcadorRepartidor.icon = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(0xFF1E88E5.toInt())
                    setStroke(6, 0xFFFFFFFF.toInt())
                    setSize(44, 44)
                }
                marcadorRepartidor.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                mapView.overlays.add(marcadorRepartidor)
            }

            mapView.invalidate()

            if (!encuadrado[0]) {
                encuadrado[0] = true
                val puntosParaEncuadre = puntosParadas + puntosRutaDecodificados +
                    listOfNotNull(posicionRepartidor)
                mapView.post {
                    val caja = BoundingBox.fromGeoPoints(puntosParaEncuadre)
                    mapView.zoomToBoundingBox(caja, false, 120, 17.0, 0L)
                }
            }
        }
    )
}

@Composable
private fun FilaParada(
    parada: ParadaRepartoDto,
    actualizando: Boolean,
    puedeEntregar: Boolean,
    onMarcarEntregada: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("${parada.orden + 1}. ${parada.nombreCliente}", fontWeight = FontWeight.Bold)
                Text(parada.direccion, style = MaterialTheme.typography.bodySmall)
            }
            if (parada.entregada) {
                Text("Entregado", color = MaterialTheme.colorScheme.primary)
            } else if (!puedeEntregar) {
                Text("Pendiente", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Button(onClick = onMarcarEntregada, enabled = !actualizando) {
                    if (actualizando) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp))
                    } else {
                        Text("Marcar entregada")
                    }
                }
            }
        }
    }
}

private const val INTERVALO_REFRESCO_MS = 20_000L

private val PERMISOS_UBICACION = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION
)

private const val INTERVALO_UBICACION_MS = 15_000L

private fun iniciarUbicacionEnVivo(
    context: Context,
    idReparto: Int,
    viewModel: RepartoViewModel
): () -> Unit {
    val concedido = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    if (!concedido) return {}

    val gestorUbicacion = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val escucha = object : LocationListener {
        override fun onLocationChanged(ubicacion: Location) {
            viewModel.actualizarMiUbicacion(idReparto, ubicacion.latitude, ubicacion.longitude)
        }

        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(proveedor: String?, estado: Int, extras: Bundle?) {}

        override fun onProviderEnabled(proveedor: String) {}

        override fun onProviderDisabled(proveedor: String) {}
    }

    var registrado = false
    val proveedores = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
    for (proveedor in proveedores) {
        try {
            if (gestorUbicacion.isProviderEnabled(proveedor)) {
                gestorUbicacion.requestLocationUpdates(
                    proveedor,
                    INTERVALO_UBICACION_MS,
                    0f,
                    escucha,
                    Looper.getMainLooper()
                )
                registrado = true
                break
            }
        } catch (e: SecurityException) {
        }
    }
    return {
        if (registrado) {
            gestorUbicacion.removeUpdates(escucha)
        }
    }
}

private fun obtenerUbicacionYActualizar(
    context: Context,
    idReparto: Int,
    viewModel: RepartoViewModel
) {
    val concedido = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    if (!concedido) return

    val gestorUbicacion = context.getSystemService(android.content.Context.LOCATION_SERVICE) as LocationManager
    val proveedores = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
    for (proveedor in proveedores) {
        try {
            if (gestorUbicacion.isProviderEnabled(proveedor)) {
                val ultimaUbicacion = gestorUbicacion.getLastKnownLocation(proveedor)
                if (ultimaUbicacion != null) {
                    viewModel.actualizarMiUbicacion(idReparto, ultimaUbicacion.latitude, ultimaUbicacion.longitude)
                    return
                }
            }
        } catch (e: SecurityException) {
        }
    }
}
