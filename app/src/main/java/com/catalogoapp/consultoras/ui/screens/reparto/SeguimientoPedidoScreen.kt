package com.catalogoapp.consultoras.ui.screens.reparto

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.catalogoapp.consultoras.data.model.RepartoResponse

@Composable
fun SeguimientoPedidoScreen(
    pedidoId: String,
    reparto: RepartoResponse?,
    onVolver: () -> Unit,
    onReportarProblema: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding() // Evita que la barra de navegación tape el botón inferior
            .padding(bottom = 32.dp)
    ) {
        // Barra Superior v3
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
            Spacer(modifier = Modifier.size(48.dp)) // Balance for the back button
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Mapa Placeholder v3
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Cargando mapa de ruta...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Línea de Tiempo de Estados v3 - Rediseñada sin "colitas"
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            EstadoTimelineItem(
                titulo = "Registrado",
                subtitulo = "Hoy, 9:12 a.m.",
                completado = true,
                esActual = false,
                color = MaterialTheme.colorScheme.outlineVariant // Neutro
            )
            EstadoTimelineItem(
                titulo = "En ruta",
                subtitulo = "Zona Surco",
                completado = true,
                esActual = true,
                color = MaterialTheme.colorScheme.primary // Rosado/Primario
            )
            EstadoTimelineItem(
                titulo = "Entregado",
                subtitulo = "Llega hoy entre 3 y 4 p.m.",
                completado = false,
                esActual = false,
                color = Color(0xFF4CAF50) // Verde éxito
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Tarjeta del Chofer v3
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
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
                        text = reparto?.chofer ?: "Luis Ramos",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "${reparto?.vehiculo ?: "ABC-123"} — Llega en 25 min",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Botón Reportar Problema
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            TextButton(
                onClick = onReportarProblema,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reportar un problema", color = MaterialTheme.colorScheme.error)
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
