package com.catalogoapp.consultoras.ui.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.catalogoapp.consultoras.data.model.Producto
import com.catalogoapp.consultoras.util.AdminConfig
import com.catalogoapp.consultoras.util.base64ABitmap
import com.catalogoapp.consultoras.viewmodel.HomeViewModel
import com.google.firebase.auth.FirebaseAuth

data class QuickAction(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val color: Color)

@Composable
fun HomeScreen(
    uid: String,
    viewModel: HomeViewModel,
    onNavegarACatalogo: () -> Unit,
    onNavegarAPedidos: () -> Unit,
    onNavegarACapacitacion: () -> Unit,
    onNavegarAPerfil: () -> Unit,
    onNavegarAComisiones: () -> Unit = {}
) {
    val estado by viewModel.uiState.collectAsState()
    val correo = remember(uid) { FirebaseAuth.getInstance().currentUser?.email }
    val administradores by AdminConfig.administradores.collectAsState()
    val esAdmin = remember(correo, administradores) { AdminConfig.esAdmin(correo) }
    var mostrarDialogoCampana by remember { mutableStateOf(false) }
    val nombreMostrado = estado.nombre.ifBlank { correo?.substringBefore('@') ?: "Consultora" }

    LaunchedEffect(uid) {
        viewModel.cargar(uid)
    }

    val acciones = listOf(
        QuickAction("Catálogo", Icons.Default.ShoppingCart, MaterialTheme.colorScheme.primary),
        QuickAction("Pedidos", Icons.Default.List, MaterialTheme.colorScheme.primary),
        QuickAction("Aprender", Icons.Default.Info, MaterialTheme.colorScheme.primary),
        QuickAction("Comisiones", Icons.Default.AccountBalanceWallet, MaterialTheme.colorScheme.primary)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Hola, $nombreMostrado",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )
                Text(
                    text = if (esAdmin) "Administrador" else "${estado.rol} · Nivel ${estado.nivel}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(
                modifier = Modifier
                    .size(45.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onNavegarAPerfil() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = "Perfil", tint = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = estado.mensajeProgreso,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = estado.progreso.toFloat().coerceIn(0f, 1f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Toca para ver tu progreso",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (mostrarDialogoCampana) {
            DialogoCampana(
                etiquetaInicial = estado.campana?.etiqueta ?: "",
                tituloInicial = estado.campana?.titulo ?: "",
                detalleInicial = estado.campana?.detalle ?: "",
                activaInicial = estado.campana?.activa ?: true,
                guardando = estado.guardandoCampana,
                error = estado.errorCampana,
                onGuardar = { etiqueta, titulo, detalle, activa ->
                    viewModel.guardarCampana(etiqueta, titulo, detalle, activa) { mostrarDialogoCampana = false }
                },
                onCancelar = {
                    viewModel.limpiarErrorCampana()
                    mostrarDialogoCampana = false
                }
            )
        }

        val campana = estado.campana
        if (campana != null && campana.activa) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    if (campana.etiqueta.isNotBlank()) {
                        Text(
                            campana.etiqueta,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        campana.titulo,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    if (campana.detalle.isNotBlank()) {
                        Text(
                            campana.detalle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (esAdmin) {
                        TextButton(onClick = { mostrarDialogoCampana = true }) { Text("Editar campaña") }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        } else if (esAdmin) {
            OutlinedButton(
                onClick = { mostrarDialogoCampana = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (campana == null) "Crear campaña para el inicio" else "Campaña desactivada: editar")
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        if (esAdmin && estado.productosStockBajo.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Stock bajo (${estado.productosStockBajo.size} producto(s))",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    estado.productosStockBajo.take(5).forEach { producto ->
                        Text(
                            "${producto.nombre}: ${if (producto.stock <= 0) "agotado" else "quedan ${producto.stock}"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    if (estado.productosStockBajo.size > 5) {
                        Text(
                            "y ${estado.productosStockBajo.size - 5} más en Perfil, Administrar productos",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        Text(
            text = "Acciones Rápidas",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            acciones.forEach { accion ->
                QuickActionItem(
                    accion = accion,
                    onClick = {
                        when(accion.title) {
                            "Catálogo" -> onNavegarACatalogo()
                            "Pedidos" -> onNavegarAPedidos()
                            "Aprender" -> onNavegarACapacitacion()
                            "Comisiones" -> onNavegarAComisiones()
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Recomendado para tus clientas",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (estado.productosDestacados.isEmpty()) {
            Text(
                "Aún no hay productos destacados",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(end = 20.dp, bottom = 24.dp)
            ) {
                items(estado.productosDestacados) { producto ->
                    FeaturedProductCard(producto = producto, onClick = onNavegarACatalogo)
                }
            }
        }
    }
}

@Composable
fun QuickActionItem(accion: QuickAction, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = accion.icon,
                contentDescription = accion.title,
                tint = accion.color,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = accion.title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun FeaturedProductCard(producto: Producto, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(158.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(158.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                val bitmap = remember(producto.imagenUrl) {
                    if (producto.imagenUrl.isNotBlank() && !producto.imagenUrl.startsWith("http")) {
                        base64ABitmap(producto.imagenUrl)
                    } else null
                }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = producto.nombre,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    producto.nombre,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${producto.categoria} · ${producto.especificaciones ?: "S/N"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "S/ ${"%.2f".format(producto.precio)}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (producto.comision > 0) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Gana S/ ${"%.2f".format(producto.comision)}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun DialogoCampana(
    etiquetaInicial: String,
    tituloInicial: String,
    detalleInicial: String,
    activaInicial: Boolean,
    guardando: Boolean,
    error: String?,
    onGuardar: (String, String, String, Boolean) -> Unit,
    onCancelar: () -> Unit
) {
    var etiqueta by remember { mutableStateOf(etiquetaInicial) }
    var titulo by remember { mutableStateOf(tituloInicial) }
    var detalle by remember { mutableStateOf(detalleInicial) }
    var activa by remember { mutableStateOf(activaInicial) }

    AlertDialog(
        onDismissRequest = { if (!guardando) onCancelar() },
        title = { Text("Campaña del inicio") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = etiqueta,
                    onValueChange = { etiqueta = it },
                    label = { Text("Etiqueta (ej. Campaña 12)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = titulo,
                    onValueChange = { titulo = it },
                    label = { Text("Título") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = detalle,
                    onValueChange = { detalle = it },
                    label = { Text("Detalle (ej. fecha de cierre)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Mostrar a las consultoras")
                    Switch(checked = activa, onCheckedChange = { activa = it })
                }
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(onClick = { onGuardar(etiqueta, titulo, detalle, activa) }, enabled = !guardando) {
                if (guardando) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp) else Text("Guardar")
            }
        },
        dismissButton = { TextButton(onClick = onCancelar, enabled = !guardando) { Text("Cancelar") } }
    )
}
