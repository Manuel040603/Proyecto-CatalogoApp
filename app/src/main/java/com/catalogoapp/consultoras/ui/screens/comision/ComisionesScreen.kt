package com.catalogoapp.consultoras.ui.screens.comision

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.catalogoapp.consultoras.viewmodel.ComisionPedido
import com.catalogoapp.consultoras.viewmodel.ComisionesViewModel

@Composable
fun ComisionesScreen(
    viewModel: ComisionesViewModel,
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
            Text("Mis comisiones", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = onVolver) { Text("Volver") }
        }
        Spacer(modifier = Modifier.height(12.dp))

        if (estado.cargando) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Column
        }

        estado.error?.let { error ->
            Text(error, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { viewModel.cargar() }) { Text("Reintentar") }
            return@Column
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Comisión ganada (pedidos entregados)", style = MaterialTheme.typography.labelMedium)
                Text(
                    "S/ ${"%.2f".format(estado.ganadas)}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Por ganar (pedidos sin entregar): S/ ${"%.2f".format(estado.porGanar)}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "Ventas entregadas: S/ ${"%.2f".format(estado.ventasEntregadas)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        if (estado.sinComisionConfigurada) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Los productos de tus pedidos aún no tienen comisión configurada. La administradora puede definirla en Perfil, Administrar productos.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (estado.entregados.isEmpty() && estado.enCurso.isEmpty()) {
            Text("Aún no tienes pedidos registrados")
            return@Column
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (estado.entregados.isNotEmpty()) {
                item { Text("Entregados", style = MaterialTheme.typography.titleMedium) }
                items(estado.entregados) { TarjetaComision(it) }
            }
            if (estado.enCurso.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Pendientes de entrega", style = MaterialTheme.typography.titleMedium)
                }
                items(estado.enCurso) { TarjetaComision(it) }
            }
        }
    }
}

@Composable
private fun TarjetaComision(item: ComisionPedido) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Pedido #${item.pedido.id} - ${item.pedido.nombreCliente}", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Venta S/ ${"%.2f".format(item.pedido.total)} - ${item.pedido.estado}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                "S/ ${"%.2f".format(item.comision)}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
