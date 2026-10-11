package com.catalogoapp.consultoras.ui.screens.pedido

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.catalogoapp.consultoras.data.model.PedidoResponse
import com.catalogoapp.consultoras.viewmodel.PedidoViewModel

@Composable
fun PedidoListScreen(
    viewModel: PedidoViewModel,
    onVolver: () -> Unit,
    onNuevoPedido: () -> Unit,
    onRastrearPedido: (String) -> Unit
) {
    val estado by viewModel.uiState.collectAsState()


    LaunchedEffect(Unit) {
        viewModel.cargarPedidos()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Pedidos", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = onVolver) { Text("Volver") }
        }
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(onClick = onNuevoPedido, modifier = Modifier.fillMaxWidth()) {
            Text("Nuevo pedido")
        }
        Spacer(modifier = Modifier.height(12.dp))

        estado.error?.let { error ->
            Text(error, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (estado.cargandoLista) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Column
        }

        if (estado.pedidos.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Aun no has registrado pedidos")
            }
            return@Column
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(estado.pedidos) { pedido ->
                TarjetaPedido(
                    pedido = pedido,
                    siguienteEstado = viewModel.siguienteEstadoDe(pedido.estado),
                    actualizando = estado.actualizandoId == pedido.id,
                    onAvanzarEstado = { viewModel.avanzarEstado(pedido) },
                    onRastrear = { onRastrearPedido(pedido.id.toString()) }
                )
            }
        }
    }
}

@Composable
private fun TarjetaPedido(
    pedido: PedidoResponse,
    siguienteEstado: String?,
    actualizando: Boolean,
    onAvanzarEstado: () -> Unit,
    onRastrear: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(pedido.nombreCliente, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(pedido.estado, color = colorEstado(pedido.estado))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(pedido.telefonoCliente, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(4.dp))
            val totalUnidades = pedido.items.sumOf { it.cantidad }
            val textoProductos = if (pedido.items.size == 1) {
                "$totalUnidades unidad(es) de 1 producto"
            } else {
                "$totalUnidades unidad(es) de ${pedido.items.size} productos"
            }
            Text("$textoProductos - S/ ${"%.2f".format(pedido.total)}", style = MaterialTheme.typography.bodyMedium)

            // BOTÓN DE RASTREO: Solo aparece si el pedido está ENVIADO o EN RUTA
            if (pedido.estado == "ENVIADO" || pedido.estado == "EN RUTA") {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onRastrear,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Rastrear Pedido", style = MaterialTheme.typography.bodySmall)
                }
            }

            if (siguienteEstado != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onAvanzarEstado,
                    enabled = !actualizando,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (actualizando) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp))
                    } else {
                        Text("Marcar como $siguienteEstado")
                    }
                }
            }
        }
    }
}

@Composable
private fun colorEstado(estado: String) = when (estado) {
    "PENDIENTE" -> MaterialTheme.colorScheme.error
    "ENVIADO" -> MaterialTheme.colorScheme.tertiary
    else -> MaterialTheme.colorScheme.primary
}
