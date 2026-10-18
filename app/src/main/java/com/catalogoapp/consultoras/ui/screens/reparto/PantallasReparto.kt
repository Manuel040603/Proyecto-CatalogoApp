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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.catalogoapp.consultoras.data.model.ParadaRepartoDto
import com.catalogoapp.consultoras.data.model.PedidoDetalle
import com.catalogoapp.consultoras.data.model.PedidoResponse
import com.catalogoapp.consultoras.data.model.ResumenRepartoDto
import com.catalogoapp.consultoras.data.repository.ProfileRepository
import com.catalogoapp.consultoras.util.AdminConfig
import com.catalogoapp.consultoras.util.HoraLima
import com.catalogoapp.consultoras.util.PedidoValidacion
import com.catalogoapp.consultoras.util.PolylineDecoder
import com.catalogoapp.consultoras.viewmodel.RepartoViewModel
import com.catalogoapp.consultoras.viewmodel.SeguimientoViewModel
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
fun ListaRepartosScreen(
    viewModel: RepartoViewModel,
    onVolver: () -> Unit,
    onNuevoReparto: () -> Unit,
    onAbrirReparto: (Int) -> Unit
) {
    val estado by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.cargarRepartos()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Mis repartos", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = onVolver) { Text("Volver") }
        }
        Spacer(modifier = Modifier.height(12.dp))

        Button(onClick = onNuevoReparto, modifier = Modifier.fillMaxWidth()) {
            Text("+ Nuevo reparto")
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (estado.cargandoRepartos) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (estado.repartos.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Todavía no has creado ningún reparto")
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(estado.repartos) { reparto ->
                    FilaReparto(reparto = reparto, onClick = { onAbrirReparto(reparto.id) })
                }
            }
        }

        estado.error?.let { error ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(error, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun FilaReparto(
    reparto: ResumenRepartoDto,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Reparto #${reparto.id}", fontWeight = FontWeight.Bold)
                Text(reparto.estado, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("${reparto.zona} - ${reparto.chofer} - ${reparto.vehiculo}", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "${reparto.paradasEntregadas}/${reparto.totalParadas} paradas entregadas",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun CrearRepartoScreen(
    viewModel: RepartoViewModel,
    onVolver: () -> Unit,
    onRepartoCreado: (Int) -> Unit
) {
    val estado by viewModel.uiState.collectAsState()
    var mostrarDialogoRepartidor by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.cargarPedidosDisponibles()
    }

    LaunchedEffect(estado.repartoCreado) {
        estado.repartoCreado?.let { reparto ->
            val idCreado = reparto.id
            viewModel.limpiarRepartoCreado()
            onRepartoCreado(idCreado)
        }
    }

    if (mostrarDialogoRepartidor) {
        DialogoNuevoRepartidor(
            onCancelar = { mostrarDialogoRepartidor = false },
            onGuardar = { nombre, placa, telefono ->
                viewModel.agregarRepartidor(nombre, placa, telefono)
                mostrarDialogoRepartidor = false
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Nuevo reparto", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = onVolver) { Text("Volver") }
        }
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = estado.zona,
            onValueChange = viewModel::actualizarZona,
            label = { Text("Zona") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Repartidor", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            TextButton(onClick = { mostrarDialogoRepartidor = true }) { Text("Agregar repartidor") }
        }
        val activos = estado.repartidores.filter { it.activo }
        if (activos.isEmpty()) {
            Text(
                "Aún no hay repartidores registrados. Agrega uno para poder crear repartos.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(activos) { repartidor ->
                    val ocupado = viewModel.estaOcupado(repartidor)
                    FilterChip(
                        selected = estado.repartidorSeleccionadoId == repartidor.id,
                        onClick = { viewModel.seleccionarRepartidor(repartidor) },
                        enabled = !ocupado,
                        label = {
                            Text(
                                "${repartidor.nombre} - ${repartidor.vehiculo}" +
                                    if (ocupado) " (en reparto)" else ""
                            )
                        }
                    )
                }
            }
            if (activos.all { viewModel.estaOcupado(it) }) {
                Text(
                    "Todos los repartidores están en un reparto en curso.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = estado.direccionOrigen,
            onValueChange = viewModel::actualizarDireccionOrigen,
            label = { Text("Dirección de origen (punto de partida)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text("Pedidos a entregar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        if (estado.cargandoPedidos) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (estado.pedidosDisponibles.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No hay pedidos pendientes con dirección registrada")
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(estado.pedidosDisponibles) { pedido ->
                    FilaPedidoSeleccionable(
                        pedido = pedido,
                        seleccionado = estado.pedidosSeleccionados.contains(pedido.id),
                        onCambiar = { viewModel.alternarPedidoSeleccionado(pedido.id) }
                    )
                }
            }
        }

        estado.error?.let { error ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(error, color = MaterialTheme.colorScheme.error)
        }
        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { viewModel.crearReparto() },
            enabled = !estado.creando,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (estado.creando) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
            } else {
                Text("Crear reparto y optimizar ruta")
            }
        }
    }
}

@Composable
private fun FilaPedidoSeleccionable(
    pedido: PedidoResponse,
    seleccionado: Boolean,
    onCambiar: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = seleccionado, onCheckedChange = { onCambiar() })
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(pedido.nombreCliente, fontWeight = FontWeight.Bold)
                Text(pedido.direccion, style = MaterialTheme.typography.bodySmall)
                Text(pedido.estado, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun DialogoNuevoRepartidor(
    onCancelar: () -> Unit,
    onGuardar: (String, String, String) -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var placa by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Nuevo repartidor") },
        text = {
            Column {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre completo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = placa,
                    onValueChange = { placa = it },
                    label = { Text("Placa del vehículo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = telefono,
                    onValueChange = { telefono = it.filter { c -> c.isDigit() }.take(9) },
                    label = { Text("Celular (opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onGuardar(nombre, placa, telefono) },
                enabled = nombre.isNotBlank() && placa.isNotBlank()
            ) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text("Cancelar") }
        }
    )
}

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

private fun fechaCorta(texto: String): String = HoraLima.desdeServidor(texto)

private fun fechaHora(milisegundos: Long): String {
    return HoraLima.formatear(milisegundos)
}

private fun fechaDeEstado(detalle: PedidoDetalle?, estados: List<String>): String? {
    val cambio = detalle?.historial?.lastOrNull { it.estado in estados && it.fecha > 0L }
    return cambio?.let { fechaHora(it.fecha) }
}

@Composable
fun SeguimientoPedidoScreen(
    pedidoId: Int,
    pedido: PedidoResponse?,
    detalle: PedidoDetalle?,
    viewModel: SeguimientoViewModel,
    onVolver: () -> Unit,
    onVerRuta: (Int) -> Unit
) {
    val estado by viewModel.uiState.collectAsState()

    LaunchedEffect(pedidoId) {
        viewModel.cargar(pedidoId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(bottom = 32.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onVolver) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
            }
            Text(
                text = "Seguimiento de Pedido",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.size(48.dp))
        }

        if (pedido == null) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No se encontró el pedido #$pedidoId", style = MaterialTheme.typography.bodyMedium)
            }
            return@Column
        }

        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Pedido #${pedido.id}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(pedido.nombreCliente, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(4.dp))
                val direccion = if (detalle != null) {
                    PedidoValidacion.armarDireccion(detalle.direccion, detalle.distrito)
                } else {
                    pedido.direccion
                }
                if (direccion.isNotBlank()) {
                    Text(direccion, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (detalle != null && detalle.fechaEntrega.isNotBlank()) {
                    Text("Entrega: ${detalle.fechaEntrega}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.height(8.dp))
                pedido.items.forEach { item ->
                    Text(
                        "${item.cantidad} x ${item.nombreProducto}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (detalle != null && detalle.costoEnvio > 0.0) {
                    Text(
                        "Envío: S/ ${"%.2f".format(detalle.costoEnvio)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Text(
                    "Total: S/ ${"%.2f".format(if (detalle != null && detalle.totalCobrado > 0.0) detalle.totalCobrado else pedido.total)}",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        val enviado = pedido.estado == "ENVIADO" || pedido.estado == "EN RUTA"
        val entregado = pedido.estado == "ENTREGADO"
        val reparto = estado.reparto
        val parada = estado.parada

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            EstadoTimelineItem(
                titulo = "Registrado",
                subtitulo = fechaDeEstado(detalle, listOf("PENDIENTE"))
                    ?: fechaCorta(pedido.fechaCreacion).ifBlank { "Fecha no disponible" },
                completado = true,
                esActual = !enviado && !entregado,
                color = MaterialTheme.colorScheme.primary
            )
            EstadoTimelineItem(
                titulo = "En ruta",
                subtitulo = when {
                    reparto != null -> "Reparto #${reparto.id} - ${reparto.zona}"
                    enviado || entregado -> fechaDeEstado(detalle, listOf("ENVIADO", "EN RUTA")) ?: "Pedido enviado"
                    else -> "Pendiente de envío"
                },
                completado = enviado || entregado,
                esActual = enviado,
                color = MaterialTheme.colorScheme.primary
            )
            EstadoTimelineItem(
                titulo = "Entregado",
                subtitulo = if (entregado) {
                    fechaDeEstado(detalle, listOf("ENTREGADO"))
                        ?: fechaCorta(pedido.fechaActualizacion).ifBlank { "Entregado" }
                } else "Pendiente",
                completado = entregado,
                esActual = entregado,
                color = Color(0xFF4CAF50)
            )
        }

        val historial = detalle?.historial.orEmpty().filter { it.fecha > 0L }.sortedBy { it.fecha }
        if (historial.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Historial de estados", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(8.dp))
                    historial.forEach { cambio ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(cambio.estado, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                            Text(
                                fechaHora(cambio.fecha),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (estado.cargando) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (reparto != null) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = reparto.chofer,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Vehículo ${reparto.vehiculo}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (parada != null) {
                            val textoParada = "Parada ${parada.orden + 1} de ${reparto.paradas.size}" +
                                if (parada.entregada) " - entregada" else ""
                            Text(
                                text = textoParada,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (reparto.latitudActual != null && !reparto.fechaUbicacion.isNullOrBlank()) {
                            Text(
                                text = "Ubicación del repartidor: ${HoraLima.desdeServidor(reparto.fechaUbicacion)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { onVerRuta(reparto.id) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ver ruta en el mapa")
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Sin reparto asignado", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    Text(
                        estado.error ?: "Este pedido aún no está en un reparto. Cuando se asigne, aquí verás chofer, vehículo y la ruta.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun EstadoTimelineItem(
    titulo: String,
    subtitulo: String,
    completado: Boolean,
    esActual: Boolean,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(if (completado) color else MaterialTheme.colorScheme.outlineVariant)
                .border(
                    width = if (completado) 0.dp else 1.dp,
                    color = if (completado) Color.Transparent else MaterialTheme.colorScheme.outline,
                    shape = CircleShape
                )
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = titulo,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (esActual) FontWeight.Bold else FontWeight.Normal,
                    color = if (esActual) color else MaterialTheme.colorScheme.onSurface
                )
            )
            Text(
                text = subtitulo,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
