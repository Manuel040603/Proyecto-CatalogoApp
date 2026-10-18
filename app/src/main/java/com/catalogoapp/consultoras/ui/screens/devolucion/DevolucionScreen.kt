package com.catalogoapp.consultoras.ui.screens.devolucion

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.catalogoapp.consultoras.data.model.Devolucion
import com.catalogoapp.consultoras.util.HoraLima
import com.catalogoapp.consultoras.viewmodel.DevolucionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevolucionScreen(
    viewModel: DevolucionViewModel,
    onVolver: () -> Unit
) {
    val estado by viewModel.uiState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    val lanzadorCamara = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap -> bitmap?.let { viewModel.registrarFoto(it) } }

    val lanzadorPermiso = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { permisoConcedido ->
        if (permisoConcedido) {
            lanzadorCamara.launch(null)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Registrar Devolución", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp)
        ) {
            if (estado.productoSeleccionado == null) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Text(
                            "SELECCIONA EL PRODUCTO",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    items(estado.productos) { producto ->
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.seleccionarProducto(producto) }
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Inventory,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(producto.nombre, style = MaterialTheme.typography.titleMedium)
                                    Text(producto.categoria, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    item {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Text(
                            "Historial de Devoluciones",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    estado.avisoHistorial?.let { aviso ->
                        item {
                            Text(
                                aviso,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                    }

                    if (estado.cargandoHistorial) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        }
                    } else if (estado.historial.isEmpty() && estado.avisoHistorial == null) {
                        item {
                            Text(
                                "Aún no hay devoluciones registradas",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                        }
                    } else {
                        items(estado.historial) { devolucion ->
                            TarjetaDevolucion(devolucion, mostrarConsultora = estado.historialDeTodas)
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Product Info Section
                    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Inventory, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("PRODUCTO", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(estado.productoSeleccionado?.nombre ?: "", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }

                    // Photo Section
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        estado.foto?.let { bitmap ->
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(250.dp)
                            ) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Foto del producto",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(16.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        } ?: Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No hay foto registrada", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        FilledTonalButton(
                            onClick = {
                                val tienePermiso = androidx.core.content.ContextCompat.checkSelfPermission(
                                    context,
                                    android.Manifest.permission.CAMERA
                                ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                                if (tienePermiso) lanzadorCamara.launch(null)
                                else lanzadorPermiso.launch(android.Manifest.permission.CAMERA)
                            },
                            modifier = Modifier.fillMaxWidth(0.7f)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (estado.foto == null) "Tomar Foto" else "Cambiar Foto")
                        }
                    }

                    // AI Classification Section
                    if (estado.foto != null && estado.resultadoApto == null) {
                        Button(
                            onClick = { viewModel.clasificarProducto() },
                            enabled = !estado.clasificando,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (estado.clasificando) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Clasificar con IA")
                            }
                        }
                    }

                    // Result Section
                    estado.resultadoApto?.let { apto ->
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = if (apto) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AssistChip(
                                    onClick = { },
                                    label = {
                                        Text(
                                            if (apto) "APTO" else "NO APTO",
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = if (apto) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                        labelColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Etiqueta: ", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(estado.etiquetaDetectada ?: "Sin detección", style = MaterialTheme.typography.bodyLarge)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Confianza: ", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${(((estado.confianza ?: 0f) * 100).toInt())}%", style = MaterialTheme.typography.bodyLarge)
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = { viewModel.confirmarActualizacionInventario() },
                                    enabled = !estado.actualizando,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (estado.actualizando) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                                    } else {
                                        Text("Confirmar y Actualizar Inventario")
                                    }
                                }
                            }
                        }
                    }

                    estado.mensaje?.let { mensaje ->
                        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(mensaje, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 12.dp))
                                OutlinedButton(onClick = { viewModel.reiniciar() }, modifier = Modifier.fillMaxWidth()) {
                                    Text("Registrar otra devolución")
                                }
                            }
                        }
                    }

                    estado.error?.let { error ->
                        Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaDevolucion(devolucion: Devolucion, mostrarConsultora: Boolean) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    devolucion.productoNombre,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                AssistChip(
                    onClick = { },
                    label = { Text(if (devolucion.apto) "APTO" else "NO APTO", style = MaterialTheme.typography.labelSmall) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (devolucion.apto) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                        labelColor = if (devolucion.apto) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                    )
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(8.dp))
                Text(HoraLima.formatear(devolucion.fecha), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Etiqueta: ${devolucion.etiquetaDetectada} - Confianza: ${(devolucion.confianza * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (mostrarConsultora && devolucion.emailConsultora.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Registrada por ${devolucion.emailConsultora}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
