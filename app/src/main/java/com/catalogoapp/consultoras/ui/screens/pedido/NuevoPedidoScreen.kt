package com.catalogoapp.consultoras.ui.screens.pedido

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.catalogoapp.consultoras.data.model.Producto
import com.catalogoapp.consultoras.util.PedidoValidacion
import com.catalogoapp.consultoras.viewmodel.PedidoViewModel

@Composable
fun NuevoPedidoScreen(
    viewModel: PedidoViewModel,
    onVolver: () -> Unit,
    onPedidoCreado: (Int) -> Unit
) {
    val estado by viewModel.uiState.collectAsState()
    val hoy = remember { PedidoValidacion.fechaDesdeHoy(0) }
    val manana = remember { PedidoValidacion.fechaDesdeHoy(1) }

    LaunchedEffect(Unit) {
        viewModel.cargarClientesFrecuentes()
    }

    LaunchedEffect(estado.mensaje) {
        if (estado.mensaje != null) {
            val idCreado = estado.ultimoPedidoId ?: 0
            viewModel.limpiarMensaje()
            onPedidoCreado(idCreado)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Pedido de cliente",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            )
            IconButton(
                onClick = onVolver,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Datos del Cliente",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        if (estado.clientesFrecuentes.isNotEmpty()) {
                            Text("Clientes frecuentes", style = MaterialTheme.typography.labelLarge)
                            Spacer(modifier = Modifier.height(4.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(estado.clientesFrecuentes) { cliente ->
                                    AssistChip(
                                        onClick = { viewModel.aplicarCliente(cliente) },
                                        label = { Text(cliente.nombre) }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        OutlinedTextField(
                            value = estado.nombreCliente,
                            onValueChange = viewModel::actualizarNombreCliente,
                            label = { Text("Nombres y Apellidos") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = estado.telefonoCliente,
                            onValueChange = viewModel::actualizarTelefonoCliente,
                            label = { Text("Celular (9 dígitos)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Text("Documento (opcional)", style = MaterialTheme.typography.labelLarge)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val opciones = listOf(
                                "Sin documento" to PedidoValidacion.SIN_DOCUMENTO,
                                "DNI" to PedidoValidacion.DNI,
                                "RUC" to PedidoValidacion.RUC
                            )
                            opciones.forEach { (etiqueta, valor) ->
                                FilterChip(
                                    selected = estado.tipoDocumento == valor,
                                    onClick = { viewModel.actualizarTipoDocumento(valor) },
                                    label = { Text(etiqueta) }
                                )
                            }
                        }

                        if (estado.tipoDocumento != PedidoValidacion.SIN_DOCUMENTO) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = estado.numeroDocumento,
                                onValueChange = viewModel::actualizarNumeroDocumento,
                                label = {
                                    Text(
                                        if (estado.tipoDocumento == PedidoValidacion.RUC) "RUC (11 dígitos)" else "DNI (8 dígitos)"
                                    )
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }

                        if (estado.tipoDocumento == PedidoValidacion.RUC) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = estado.razonSocial,
                                onValueChange = viewModel::actualizarRazonSocial,
                                label = { Text("Razón social") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Entrega",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(PedidoValidacion.ENTREGA_CLIENTE, PedidoValidacion.ENTREGA_CONSULTORA).forEach { tipo ->
                                FilterChip(
                                    selected = estado.tipoEntrega == tipo,
                                    onClick = { viewModel.actualizarTipoEntrega(tipo) },
                                    label = { Text(tipo) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        if (estado.tipoEntrega == PedidoValidacion.ENTREGA_CONSULTORA) {
                            Text(
                                "Se entrega en tu dirección de Perfil, Datos y dirección:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            if (estado.direccion.isBlank()) {
                                Text(
                                    "Sin dirección registrada todavía",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                            } else {
                                Text(
                                    PedidoValidacion.armarDireccion(estado.direccion, estado.distrito),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (estado.referencia.isNotBlank()) {
                                    Text(
                                        "Ref: ${estado.referencia}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            OutlinedTextField(
                                value = estado.direccion,
                                onValueChange = viewModel::actualizarDireccion,
                                label = { Text("Dirección de entrega del cliente") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = estado.distrito,
                                onValueChange = viewModel::actualizarDistrito,
                                label = { Text("Distrito") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = estado.referencia,
                                onValueChange = viewModel::actualizarReferencia,
                                label = { Text("Referencia (opcional)") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = estado.fechaEntrega,
                            onValueChange = viewModel::actualizarFechaEntrega,
                            label = { Text("Fecha de entrega dd/mm/aaaa (opcional)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = estado.fechaEntrega == hoy,
                                onClick = { viewModel.actualizarFechaEntrega(hoy) },
                                label = { Text("Hoy") }
                            )
                            FilterChip(
                                selected = estado.fechaEntrega == manana,
                                onClick = { viewModel.actualizarFechaEntrega(manana) },
                                label = { Text("Mañana") }
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = estado.indicaciones,
                            onValueChange = viewModel::actualizarIndicaciones,
                            label = { Text("Indicaciones de entrega (opcional)") },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }

            item {
                Text("Productos", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(estado.productosDisponibles) { producto ->
                FilaProductoPedido(
                    producto = producto,
                    cantidad = viewModel.cantidadEnCarritoDe(producto.id),
                    onCambiarCantidad = { cantidad -> viewModel.cambiarCantidadEnCarrito(producto, cantidad) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Método de Pago", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PedidoValidacion.METODOS_PAGO.forEach { metodo ->
                        FilterChip(
                            selected = estado.metodoPago == metodo,
                            onClick = { viewModel.actualizarMetodoPago(metodo) },
                            label = { Text(metodo) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }
        }

        estado.error?.let { mensajeError ->
            Text(
                mensajeError,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Productos: S/ ${"%.2f".format(viewModel.totalCarrito())}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            if (viewModel.costoEnvio() > 0.0) "Envío al cliente: S/ ${"%.2f".format(viewModel.costoEnvio())}" else "Envío: gratis (a la consultora)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Total a cobrar", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "S/ ${"%.2f".format(viewModel.totalConEnvio())}",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                }
                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { viewModel.crearPedido() },
                    enabled = !estado.guardando,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    if (estado.guardando) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    } else {
                        Text("Registrar pedido", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    }
}

@Composable
private fun FilaProductoPedido(
    producto: Producto,
    cantidad: Int,
    onCambiarCantidad: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(producto.nombre, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "S/ ${"%.2f".format(producto.precio)} x $cantidad  |  Stock: ${producto.stock}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { onCambiarCantidad(cantidad - 1) },
                    enabled = cantidad > 0,
                    modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Menos", modifier = Modifier.size(16.dp))
                }
                Text(
                    "$cantidad",
                    modifier = Modifier.padding(horizontal = 12.dp),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
                IconButton(
                    onClick = { onCambiarCantidad(cantidad + 1) },
                    enabled = cantidad < producto.stock,
                    modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Más", modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
