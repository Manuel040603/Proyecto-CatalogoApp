package com.catalogoapp.consultoras.ui.screens.capacitacion

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.catalogoapp.consultoras.viewmodel.NuevaCapacitacionViewModel
import com.catalogoapp.consultoras.viewmodel.PreguntaEditable

@Composable
fun NuevaCapacitacionScreen(
    viewModel: NuevaCapacitacionViewModel,
    onVolver: () -> Unit
) {
    val estado by viewModel.uiState.collectAsState()

    if (estado.mensaje != null) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(estado.mensaje ?: "", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onVolver) { Text("Volver a capacitaciones") }
        }
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Nueva capacitación", style = MaterialTheme.typography.headlineSmall)
                TextButton(onClick = onVolver) { Text("Volver") }
            }
        }

        item {
            OutlinedTextField(
                value = estado.titulo,
                onValueChange = { viewModel.actualizarTitulo(it) },
                label = { Text("Título") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = estado.descripcion,
                onValueChange = { viewModel.actualizarDescripcion(it) },
                label = { Text("Descripción") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = estado.contenidoNarracion,
                onValueChange = { viewModel.actualizarContenidoNarracion(it) },
                label = { Text("Contenido a narrar (se leerá en voz alta)") },
                modifier = Modifier.fillMaxWidth().height(140.dp)
            )
        }

        itemsIndexed(estado.preguntas) { indice, pregunta ->
            TarjetaPreguntaEditable(
                numero = indice + 1,
                pregunta = pregunta,
                onCambiar = { viewModel.actualizarPregunta(indice, it) }
            )
        }

        item {
            if (estado.error != null) {
                Text(estado.error ?: "", color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(8.dp))
            }
            Button(
                onClick = { viewModel.guardarCapacitacion() },
                enabled = !estado.guardando,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (estado.guardando) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Text("Guardar capacitación")
                }
            }
        }
    }
}

@Composable
private fun TarjetaPreguntaEditable(
    numero: Int,
    pregunta: PreguntaEditable,
    onCambiar: (PreguntaEditable) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("Pregunta $numero", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = pregunta.enunciado,
                onValueChange = { onCambiar(pregunta.copy(enunciado = it)) },
                label = { Text("Enunciado") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            val opciones = listOf(pregunta.opcion1, pregunta.opcion2, pregunta.opcion3)
            opciones.forEachIndexed { indice, valor ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = pregunta.respuestaCorrecta == indice,
                            onClick = { onCambiar(pregunta.copy(respuestaCorrecta = indice)) }
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = pregunta.respuestaCorrecta == indice,
                        onClick = { onCambiar(pregunta.copy(respuestaCorrecta = indice)) }
                    )
                    OutlinedTextField(
                        value = valor,
                        onValueChange = { nuevoValor ->
                            onCambiar(
                                when (indice) {
                                    0 -> pregunta.copy(opcion1 = nuevoValor)
                                    1 -> pregunta.copy(opcion2 = nuevoValor)
                                    else -> pregunta.copy(opcion3 = nuevoValor)
                                }
                            )
                        },
                        label = { Text("Opción ${indice + 1}") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Text(
                "Marca el círculo de la opción correcta",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
