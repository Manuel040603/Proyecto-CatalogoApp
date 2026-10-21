package com.catalogoapp.consultoras.ui.screens.reparto

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.catalogoapp.consultoras.data.model.ResumenRepartoDto
import com.catalogoapp.consultoras.viewmodel.RepartoViewModel

@Composable
fun ListaRepartosScreen(
    viewModel: RepartoViewModel,
    onVolver: () -> Unit,
    onNuevoReparto: () -> Unit,
    onAbrirReparto: (Int) -> Unit
) {
    val estado by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.cargarRepartos()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Mis repartos", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = onVolver) { Text("Volver") }
        }
        Spacer(modifier = Modifier.height(12.dp))

        Button(onClick = onNuevoReparto, modifier = Modifier.fillMaxWidth()) {
            Text("+ Nuevo reparto")
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (estado.cargandoRepartos) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (estado.repartos.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Todavía no has creado ningún reparto")
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(estado.repartos) { reparto ->
                    FilaReparto(reparto = reparto, onClick = { onAbrirReparto(reparto.id) })
                }
            }
        }

        estado.error?.let { error ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(error, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun FilaReparto(
    reparto: ResumenRepartoDto,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Reparto #${reparto.id}", fontWeight = FontWeight.Bold)
                Text(reparto.estado, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("${reparto.zona} - ${reparto.chofer} - ${reparto.vehiculo}", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "${reparto.paradasEntregadas}/${reparto.totalParadas} paradas entregadas",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
