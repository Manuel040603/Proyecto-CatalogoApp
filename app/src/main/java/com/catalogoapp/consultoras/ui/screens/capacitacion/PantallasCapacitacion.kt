package com.catalogoapp.consultoras.ui.screens.capacitacion

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.catalogoapp.consultoras.data.model.Capacitacion
import com.catalogoapp.consultoras.viewmodel.CapacitacionDetalleViewModel
import com.catalogoapp.consultoras.viewmodel.CapacitacionViewModel
import com.catalogoapp.consultoras.viewmodel.NuevaCapacitacionViewModel
import com.catalogoapp.consultoras.viewmodel.PreguntaEditable

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
