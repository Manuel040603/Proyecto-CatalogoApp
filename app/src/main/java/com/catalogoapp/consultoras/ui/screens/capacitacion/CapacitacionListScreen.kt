package com.catalogoapp.consultoras.ui.screens.capacitacion

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.catalogoapp.consultoras.data.model.Capacitacion
import com.catalogoapp.consultoras.viewmodel.CapacitacionViewModel

@Composable
fun CapacitacionListScreen(
    viewModel: CapacitacionViewModel,
    esAdmin: Boolean = false,
    onVolver: () -> Unit,
    onAbrirCapacitacion: (String) -> Unit,
    onNuevaCapacitacion: () -> Unit
) {
    val estado by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Capacitaciones", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = onVolver) { Text("Volver") }
        }
        Spacer(modifier = Modifier.height(8.dp))

        if (esAdmin) {
            OutlinedButton(onClick = onNuevaCapacitacion, modifier = Modifier.fillMaxWidth()) {
                Text("Nueva capacitación")
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (estado.cargando) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Column
        }

        if (estado.capacitaciones.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Aún no hay capacitaciones publicadas")
            }
            return@Column
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(estado.capacitaciones) { capacitacion ->
                TarjetaCapacitacion(capacitacion = capacitacion, onClick = { onAbrirCapacitacion(capacitacion.id) })
            }
        }
    }
}

@Composable
private fun TarjetaCapacitacion(capacitacion: Capacitacion, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(capacitacion.titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(capacitacion.descripcion, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "${capacitacion.preguntas.size} preguntas de evaluación",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
