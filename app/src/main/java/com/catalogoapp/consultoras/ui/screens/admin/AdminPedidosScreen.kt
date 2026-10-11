package com.catalogoapp.consultoras.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.catalogoapp.consultoras.data.model.PedidoDetalle
import com.catalogoapp.consultoras.util.PedidoValidacion
import com.catalogoapp.consultoras.viewmodel.AdminPedidosViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AdminPedidosScreen(
    viewModel: AdminPedidosViewModel,
    onVolver: () -> Unit
) {
    val estado by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.cargar()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Todos los pedidos", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = onVolver) { Text("Volver") }
        }
        Spacer(modifier = Modifier.height(8.dp))

        if (estado.cargando) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Column
        }

        estado.error?.let { error ->
            Text(error, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { viewModel.cargar() }) { Text("Reintentar") }
            return@Column
        }

        Text(
            "${estado.pedidos.size} pedido(s) - Ventas S/ ${"%.2f".format(estado.ventasTotales)}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = estado.filtroEstado == null,
                    onClick = { viewModel.filtrarPorEstado(null) },
                    label = { Text("Todos") }
                )
            }
            items(estado.estadosPresentes) { nombreEstado ->
                FilterChip(
                    selected = estado.filtroEstado == nombreEstado,
                    onClick = { viewModel.filtrarPorEstado(nombreEstado) },
                    label = { Text("$nombreEstado (${estado.pedidos.count { it.estado == nombreEstado }})") }
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        if (estado.visibles.isEmpty()) {
            Text("No hay pedidos para mostrar")
            return@Column
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(estado.visibles) { pedido ->
                TarjetaPedidoAdmin(
                    pedido = pedido,
                    consultora = estado.nombresConsultoras[pedido.uidConsultora] ?: "Consultora no registrada"
                )
            }
        }
    }
}

@Composable
private fun TarjetaPedidoAdmin(pedido: PedidoDetalle, consultora: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Pedido #${pedido.pedidoId} - ${pedido.nombreCliente}", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Text(pedido.estado, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
            Text("Consultora: $consultora", style = MaterialTheme.typography.bodySmall)
            if (pedido.fechaCreacion > 0L) {
                Text(
                    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US).format(Date(pedido.fechaCreacion)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            val unidades = pedido.items.sumOf { it.cantidad }
            Text(
                "$unidades unidad(es) - S/ ${"%.2f".format(pedido.total)}" +
                    if (pedido.costoEnvio > 0.0) " + envio S/ ${"%.2f".format(pedido.costoEnvio)}" else "",
                style = MaterialTheme.typography.bodySmall
            )
            val direccion = PedidoValidacion.armarDireccion(pedido.direccion, pedido.distrito)
            if (direccion.isNotBlank()) {
                Text(
                    "${pedido.tipoEntrega}: $direccion",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text("Pago: ${pedido.metodoPago}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
