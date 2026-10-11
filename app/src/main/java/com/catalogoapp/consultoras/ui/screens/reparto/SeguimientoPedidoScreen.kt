package com.catalogoapp.consultoras.ui.screens.reparto

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.catalogoapp.consultoras.data.model.PedidoDetalle
import com.catalogoapp.consultoras.data.model.PedidoResponse
import com.catalogoapp.consultoras.util.PedidoValidacion
import com.catalogoapp.consultoras.viewmodel.SeguimientoViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun fechaCorta(texto: String): String {
    return if (texto.isBlank()) "" else texto.replace('T', ' ').take(16)
}

private fun fechaHora(milisegundos: Long): String {
    return SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US).format(Date(milisegundos))
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
                Text("No se encontro el pedido #$pedidoId", style = MaterialTheme.typography.bodyMedium)
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
                        "Envio: S/ ${"%.2f".format(detalle.costoEnvio)}",
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
                    else -> "Pendiente de envio"
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
                            text = "Vehiculo ${reparto.vehiculo}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (parada != null) {
                            val textoParada = "Parada ${parada.orden} de ${reparto.paradas.size}" +
                                if (parada.entregada) " - entregada" else ""
                            Text(
                                text = textoParada,
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
                        estado.error ?: "Este pedido aun no esta en un reparto. Cuando se asigne, aqui veras chofer, vehiculo y la ruta.",
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
