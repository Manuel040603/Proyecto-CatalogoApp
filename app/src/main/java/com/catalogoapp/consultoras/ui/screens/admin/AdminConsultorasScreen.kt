package com.catalogoapp.consultoras.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.catalogoapp.consultoras.data.model.ConsultoraProfile
import com.catalogoapp.consultoras.util.ConfiguracionAdmin
import com.catalogoapp.consultoras.util.PedidoValidacion
import com.catalogoapp.consultoras.viewmodel.AdminConsultorasViewModel

@Composable
fun AdminConsultorasScreen(
    viewModel: AdminConsultorasViewModel,
    onVolver: () -> Unit
) {
    val estado by viewModel.uiState.collectAsState()
    var consultoraEditando by remember { mutableStateOf<ConsultoraProfile?>(null) }
    var creandoNueva by remember { mutableStateOf(false) }

    consultoraEditando?.let { consultora ->
        DialogoEditarConsultora(
            consultora = consultora,
            guardando = estado.actualizando,
            error = estado.errorEdicion,
            onGuardar = { nombre, dni, celular, preferencias, direccion, distrito, referencia ->
                viewModel.guardarEdicion(
                    uid = consultora.uid,
                    nombre = nombre,
                    dni = dni,
                    celular = celular,
                    preferencias = preferencias,
                    direccionEntrega = direccion,
                    distrito = distrito,
                    referencia = referencia,
                    alTerminar = { consultoraEditando = null }
                )
            },
            onCancelar = {
                viewModel.limpiarErrorEdicion()
                consultoraEditando = null
            }
        )
    }

    if (creandoNueva) {
        DialogoNuevaConsultora(
            guardando = estado.actualizando,
            error = estado.error,
            onGuardar = { nombre, email, dni, celular, preferencias, direccion, distrito, referencia ->
                viewModel.registrarNuevaConsultora(
                    nombre = nombre,
                    email = email,
                    dni = dni,
                    celular = celular,
                    preferencias = preferencias,
                    direccionEntrega = direccion,
                    distrito = distrito,
                    referencia = referencia,
                    alTerminar = { creandoNueva = false }
                )
            },
            onCancelar = { creandoNueva = false }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Administrar consultoras", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = onVolver) { Text("Volver") }
        }
        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { creandoNueva = true },
            modifier = Modifier.fillMaxWidth(),
            enabled = !estado.actualizando
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Agregar nueva consultora")
        }
        Spacer(modifier = Modifier.height(12.dp))

        estado.mensaje?.let { mensaje ->
            Text(mensaje, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(8.dp))
        }
        estado.error?.let { error ->
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (estado.cargando) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Column
        }

        if (estado.consultoras.isEmpty()) {
            Text("Aún no hay consultoras registradas")
            return@Column
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(estado.consultoras) { consultora ->
                TarjetaConsultora(
                    consultora = consultora,
                    actualizando = estado.actualizando,
                    mostrarRol = estado.esSuperAdmin && !ConfiguracionAdmin.esSuperAdmin(consultora.email),
                    esAdministradora = estado.administradores.contains(consultora.email.trim().lowercase()),
                    onCambiarRol = { esAdmin -> viewModel.cambiarAdministrador(consultora.email, if (esAdmin) "DIRECTORA_COMERCIAL" else "CONSULTORA") },
                    onEditar = {
                        viewModel.limpiarMensaje()
                        consultoraEditando = consultora
                    },
                    onCambiarEstado = { activo -> viewModel.cambiarEstado(consultora.uid, activo) }
                )
            }
        }
    }
}

@Composable
private fun DialogoNuevaConsultora(
    guardando: Boolean,
    error: String?,
    onGuardar: (String, String, String, String, String, String, String, String) -> Unit,
    onCancelar: () -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var dni by remember { mutableStateOf("") }
    var celular by remember { mutableStateOf("") }
    var preferencias by remember { mutableStateOf("") }
    var direccion by remember { mutableStateOf("") }
    var distrito by remember { mutableStateOf("") }
    var referencia by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!guardando) onCancelar() },
        title = { Text("Registrar consultora") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombres y apellidos") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Correo electrónico") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = dni,
                    onValueChange = { dni = it },
                    label = { Text("DNI") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = celular,
                    onValueChange = { celular = it },
                    label = { Text("Celular") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = preferencias,
                    onValueChange = { preferencias = it },
                    label = { Text("Preferencias") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = direccion,
                    onValueChange = { direccion = it },
                    label = { Text("Dirección de entrega") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = distrito,
                    onValueChange = { distrito = it },
                    label = { Text("Distrito") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = referencia,
                    onValueChange = { referencia = it },
                    label = { Text("Referencia") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onGuardar(nombre, email, dni, celular, preferencias, direccion, distrito, referencia) },
                enabled = !guardando
            ) {
                if (guardando) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Registrar")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar, enabled = !guardando) { Text("Cancelar") }
        }
    )
}

@Composable
private fun TarjetaConsultora(

    consultora: ConsultoraProfile,
    actualizando: Boolean,
    mostrarRol: Boolean,
    esAdministradora: Boolean,
    onCambiarRol: (Boolean) -> Unit,
    onEditar: () -> Unit,
    onCambiarEstado: (Boolean) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    consultora.nombre.ifBlank { "Sin nombre registrado" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(consultora.email, style = MaterialTheme.typography.bodySmall)
                val contacto = listOf(
                    if (consultora.dni.isNotBlank()) "DNI ${consultora.dni}" else "",
                    if (consultora.celular.isNotBlank()) "Cel. ${consultora.celular}" else ""
                ).filter { it.isNotBlank() }.joinToString(" - ")
                if (contacto.isNotBlank()) {
                    Text(contacto, style = MaterialTheme.typography.bodySmall)
                }
                val direccion = PedidoValidacion.armarDireccion(consultora.direccionEntrega, consultora.distrito)
                if (direccion.isNotBlank()) {
                    Text(direccion, style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    "Nivel ${consultora.nivel} - ${"%.0f".format(consultora.progreso * 100)}%",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    if (consultora.activo) "Activa" else "Inactiva",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (consultora.activo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
            IconButton(onClick = onEditar, enabled = !actualizando) {
                Icon(Icons.Default.Edit, contentDescription = "Editar datos", tint = MaterialTheme.colorScheme.primary)
            }
            Switch(
                checked = consultora.activo,
                onCheckedChange = onCambiarEstado,
                enabled = !actualizando
            )
        }
        if (mostrarRol) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Es administradora", style = MaterialTheme.typography.bodySmall)
                Switch(
                    checked = esAdministradora,
                    onCheckedChange = onCambiarRol,
                    enabled = !actualizando
                )
            }
        }
        }
    }
}

@Composable
private fun DialogoEditarConsultora(
    consultora: ConsultoraProfile,
    guardando: Boolean,
    error: String?,
    onGuardar: (String, String, String, String, String, String, String) -> Unit,
    onCancelar: () -> Unit
) {
    var nombre by remember(consultora.uid) { mutableStateOf(consultora.nombre) }
    var dni by remember(consultora.uid) { mutableStateOf(consultora.dni) }
    var celular by remember(consultora.uid) { mutableStateOf(consultora.celular) }
    var preferencias by remember(consultora.uid) { mutableStateOf(consultora.preferencias) }
    var direccion by remember(consultora.uid) { mutableStateOf(consultora.direccionEntrega) }
    var distrito by remember(consultora.uid) { mutableStateOf(consultora.distrito) }
    var referencia by remember(consultora.uid) { mutableStateOf(consultora.referencia) }

    AlertDialog(
        onDismissRequest = { if (!guardando) onCancelar() },
        title = { Text("Editar consultora") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(consultora.email, style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombres y apellidos") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = dni,
                    onValueChange = { dni = PedidoValidacion.soloDigitos(it, 8) },
                    label = { Text("DNI") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = celular,
                    onValueChange = { celular = PedidoValidacion.soloDigitos(it, 9) },
                    label = { Text("Celular") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = preferencias,
                    onValueChange = { preferencias = it },
                    label = { Text("Preferencias") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = direccion,
                    onValueChange = { direccion = it },
                    label = { Text("Dirección de entrega") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = distrito,
                    onValueChange = { distrito = it },
                    label = { Text("Distrito") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = referencia,
                    onValueChange = { referencia = it },
                    label = { Text("Referencia") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onGuardar(nombre, dni, celular, preferencias, direccion, distrito, referencia) },
                enabled = !guardando
            ) {
                if (guardando) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Guardar")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar, enabled = !guardando) { Text("Cancelar") }
        }
    )
}
