package com.catalogoapp.consultoras.ui.screens.reparto

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.catalogoapp.consultoras.data.model.PedidoResponse
import com.catalogoapp.consultoras.viewmodel.RepartoViewModel

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
