package com.catalogoapp.consultoras.ui.screens.capacitacion

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.catalogoapp.consultoras.viewmodel.CapacitacionDetalleViewModel

@Composable
fun CapacitacionDetalleScreen(
    capacitacionId: String,
    uid: String,
    viewModel: CapacitacionDetalleViewModel,
    onVolver: () -> Unit
) {
    val estado by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(capacitacionId) {
        viewModel.cargar(capacitacionId)
        viewModel.iniciarNarrador(context)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Capacitación", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = onVolver) { Text("Volver") }
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (estado.cargando || estado.capacitacion == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Column
        }

        val capacitacion = estado.capacitacion!!

        if (!estado.enEvaluacion) {
            Text(capacitacion.titulo, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(capacitacion.descripcion, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.reproducirPausar() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Reproducir")
                }
                Button(
                    onClick = { viewModel.pausarNarracion() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Pausar")
                }
                Button(
                    onClick = { viewModel.detenerNarracion() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Detener")
                }
            }
            Spacer(modifier = Modifier.height(20.dp))

            if (capacitacion.preguntas.isNotEmpty()) {
                Button(onClick = { viewModel.iniciarEvaluacion() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Comenzar evaluación")
                }
            }
            return@Column
        }

        if (estado.enEvaluacion && !estado.evaluacionTerminada) {
            val pregunta = capacitacion.preguntas[estado.preguntaActual]
            Text(
                "Pregunta ${estado.preguntaActual + 1} de ${capacitacion.preguntas.size}",
                style = MaterialTheme.typography.labelLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(pregunta.enunciado, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))

            val seleccionActual = estado.respuestasSeleccionadas.getOrNull(estado.preguntaActual)
            pregunta.opciones.forEachIndexed { indice, opcion ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = seleccionActual == indice,
                            onClick = { viewModel.seleccionarRespuesta(indice) }
                        )
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = seleccionActual == indice, onClick = { viewModel.seleccionarRespuesta(indice) })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(opcion)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.siguientePregunta() },
                enabled = seleccionActual != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (estado.preguntaActual < capacitacion.preguntas.size - 1) "Siguiente" else "Finalizar")
            }
            return@Column
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text("Evaluación completada", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "${estado.puntaje} de ${capacitacion.preguntas.size} respuestas correctas",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(20.dp))

            if (estado.mensaje == null) {
                Button(
                    onClick = { viewModel.guardarResultado(uid) },
                    enabled = !estado.guardando,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (estado.guardando) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    } else {
                        Text("Guardar resultado")
                    }
                }
            } else {
                Text(estado.mensaje ?: "", color = MaterialTheme.colorScheme.primary)
            }

            estado.error?.let { error ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(error, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
