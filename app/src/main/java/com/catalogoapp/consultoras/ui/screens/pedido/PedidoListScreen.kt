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
import com.catalogoapp.consultoras.data.model.PedidoDetalle
import com.catalogoapp.consultoras.data.model.PedidoResponse
import com.catalogoapp.consultoras.util.PedidoValidacion
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

        estado.aviso?.let { aviso ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(aviso, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                    TextButton(onClick = { viewModel.limpiarAviso() }) { Text("Entendido") }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

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
                Text("Aún no has registrado pedidos")
            }
            return@Column
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(estado.pedidos, key = { it.id }) { pedido ->
                TarjetaPedido(
                    pedido = pedido,
                    detalle = estado.detalles[pedido.id],
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
    detalle: PedidoDetalle?,
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

            if (detalle != null) {
                if (detalle.numeroDocumento.isNotBlank()) {
                    val textoDocumento = if (detalle.tipoDocumento == PedidoValidacion.RUC) {
                        "RUC ${detalle.numeroDocumento} - ${detalle.razonSocial}"
                    } else {
                        "DNI ${detalle.numeroDocumento}"
                    }
                    LineaDetalle(textoDocumento)
                }
                LineaDetalle("Entrega ${detalle.tipoEntrega.replaceFirstChar { it.lowercase() }}: ${PedidoValidacion.armarDireccion(detalle.direccion, detalle.distrito)}")
                if (detalle.referencia.isNotBlank()) LineaDetalle("Referencia: ${detalle.referencia}")
                if (detalle.fechaEntrega.isNotBlank()) LineaDetalle("Entrega: ${detalle.fechaEntrega}")
                if (detalle.indicaciones.isNotBlank()) LineaDetalle("Indicaciones: ${detalle.indicaciones}")
                if (detalle.costoEnvio > 0.0) {
                    LineaDetalle("Envío: S/ ${"%.2f".format(detalle.costoEnvio)} - Total a cobrar: S/ ${"%.2f".format(detalle.totalCobrado)}")
                }
                LineaDetalle("Pago: ${detalle.metodoPago}")
            } else if (pedido.direccion.isNotBlank()) {
                LineaDetalle("Dirección: ${pedido.direccion}")
            }

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
private fun LineaDetalle(texto: String) {
    Spacer(modifier = Modifier.height(2.dp))
    Text(texto, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun colorEstado(estado: String) = when (estado) {
    "PENDIENTE" -> MaterialTheme.colorScheme.error
    "ENVIADO" -> MaterialTheme.colorScheme.tertiary
    else -> MaterialTheme.colorScheme.primary
}
