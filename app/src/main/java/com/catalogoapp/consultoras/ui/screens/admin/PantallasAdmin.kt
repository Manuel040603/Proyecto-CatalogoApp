package com.catalogoapp.consultoras.ui.screens.admin

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.catalogoapp.consultoras.data.model.ConsultoraProfile
import com.catalogoapp.consultoras.data.model.PedidoDetalle
import com.catalogoapp.consultoras.data.model.Producto
import com.catalogoapp.consultoras.util.AdminConfig
import com.catalogoapp.consultoras.util.HoraLima
import com.catalogoapp.consultoras.util.Inventario
import com.catalogoapp.consultoras.util.PedidoValidacion
import com.catalogoapp.consultoras.util.bitmapABase64
import com.catalogoapp.consultoras.viewmodel.AdminConsultorasViewModel
import com.catalogoapp.consultoras.viewmodel.AdminPedidosViewModel
import com.catalogoapp.consultoras.viewmodel.AdminProductosViewModel
import com.catalogoapp.consultoras.viewmodel.AgregarProductoUiState
import com.catalogoapp.consultoras.viewmodel.AgregarProductoViewModel

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
                    mostrarRol = estado.esSuperAdmin && !AdminConfig.esSuperAdmin(consultora.email),
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

@Composable
fun AgregarProductoScreen(
    viewModel: AgregarProductoViewModel,
    onVolver: () -> Unit
) {
    val context = LocalContext.current
    val estado by viewModel.uiState.collectAsState()

    var nombre by remember { mutableStateOf("") }
    var categoria by remember { mutableStateOf("") }
    var precio by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var stock by remember { mutableStateOf("") }
    var comision by remember { mutableStateOf("") }
    var foto by remember { mutableStateOf<Bitmap?>(null) }

    val lanzadorCamara = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap -> foto = bitmap }

    val lanzadorPermisoCamara = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { concedido -> if (concedido) lanzadorCamara.launch(null) }

    val formularioValido = nombre.isNotBlank() && categoria.isNotBlank() &&
        precio.toDoubleOrNull() != null && stock.toIntOrNull() != null &&
        (comision.isBlank() || comision.toDoubleOrNull() != null)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Agregar producto", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = onVolver) { Text("Volver") }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable {
                    val permisoOk = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED
                    if (permisoOk) {
                        lanzadorCamara.launch(null)
                    } else {
                        lanzadorPermisoCamara.launch(Manifest.permission.CAMERA)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (foto != null) {
                Image(
                    bitmap = foto!!.asImageBitmap(),
                    contentDescription = "Foto del producto",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Tocar para tomar foto", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = { Text("Nombre del producto") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = categoria,
            onValueChange = { categoria = it },
            label = { Text("Categoría") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = precio,
            onValueChange = { precio = it },
            label = { Text("Precio (S/)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = stock,
            onValueChange = { stock = it },
            label = { Text("Stock") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = comision,
            onValueChange = { comision = it },
            label = { Text("Comisión por unidad (S/, opcional)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = descripcion,
            onValueChange = { descripcion = it },
            label = { Text("Descripción") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                val imagenBase64 = foto?.let { bitmapABase64(it) } ?: ""
                viewModel.guardarProducto(
                    nombre = nombre,
                    categoria = categoria,
                    precio = precio.toDoubleOrNull() ?: 0.0,
                    descripcion = descripcion,
                    stock = stock.toIntOrNull() ?: 0,
                    imagenBase64 = imagenBase64,
                    comision = comision.toDoubleOrNull() ?: 0.0
                )
            },
            enabled = formularioValido && estado !is AgregarProductoUiState.Guardando,
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            if (estado is AgregarProductoUiState.Guardando) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
            } else {
                Text("Guardar producto")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (val actual = estado) {
            is AgregarProductoUiState.Guardado -> {
                Text(actual.mensaje, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                LaunchedEffect(Unit) {
                    nombre = ""
                    categoria = ""
                    precio = ""
                    descripcion = ""
                    stock = ""
                    comision = ""
                    foto = null
                }
            }
            is AgregarProductoUiState.Error -> {
                Text(actual.mensaje, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            }
            else -> {}
        }
    }
}

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
                    HoraLima.formatear(pedido.fechaCreacion),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            val unidades = pedido.items.sumOf { it.cantidad }
            Text(
                "$unidades unidad(es) - S/ ${"%.2f".format(pedido.total)}" +
                    if (pedido.costoEnvio > 0.0) " + envío S/ ${"%.2f".format(pedido.costoEnvio)}" else "",
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

@Composable
fun AdminProductosScreen(
    viewModel: AdminProductosViewModel,
    onVolver: () -> Unit,
    onAgregar: () -> Unit
) {
    val estado by viewModel.uiState.collectAsState()
    var editando by remember { mutableStateOf<Producto?>(null) }
    var porEliminar by remember { mutableStateOf<Producto?>(null) }

    editando?.let { producto ->
        DialogoEditarProducto(
            producto = producto,
            guardando = estado.trabajando,
            error = estado.errorEdicion,
            onGuardar = { nombre, categoria, precio, stock, comision, descripcion ->
                viewModel.guardar(
                    id = producto.id,
                    nombre = nombre,
                    categoria = categoria,
                    precio = precio,
                    stock = stock,
                    comision = comision,
                    descripcion = descripcion,
                    alTerminar = { editando = null }
                )
            },
            onCancelar = {
                viewModel.limpiarErrorEdicion()
                editando = null
            }
        )
    }

    porEliminar?.let { producto ->
        AlertDialog(
            onDismissRequest = { porEliminar = null },
            title = { Text("Eliminar producto") },
            text = { Text("Se eliminará ${producto.nombre} del catálogo. Los pedidos ya hechos no se modifican. Esta acción no se puede deshacer.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.eliminar(producto.id)
                        porEliminar = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { porEliminar = null }) { Text("Cancelar") } }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Administrar productos", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = onVolver) { Text("Volver") }
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(onClick = onAgregar, modifier = Modifier.fillMaxWidth()) { Text("Agregar producto nuevo") }
        Spacer(modifier = Modifier.height(8.dp))

        estado.mensaje?.let { Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall) }
        estado.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

        if (estado.cargando) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Column
        }
        if (estado.productos.isEmpty()) {
            Text("Aún no hay productos en el catálogo")
            return@Column
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(estado.productos) { producto ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(producto.nombre, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(producto.categoria, style = MaterialTheme.typography.bodySmall)
                            Text(
                                "S/ ${"%.2f".format(producto.precio)} - Comisión S/ ${"%.2f".format(producto.comision)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            val etiqueta = Inventario.etiqueta(producto.stock)
                            Text(
                                "Stock: ${producto.stock}" + if (etiqueta != null) " ($etiqueta)" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (etiqueta != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { editando = producto }, enabled = !estado.trabajando) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { porEliminar = producto }, enabled = !estado.trabajando) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogoEditarProducto(
    producto: Producto,
    guardando: Boolean,
    error: String?,
    onGuardar: (String, String, String, String, String, String) -> Unit,
    onCancelar: () -> Unit
) {
    var nombre by remember(producto.id) { mutableStateOf(producto.nombre) }
    var categoria by remember(producto.id) { mutableStateOf(producto.categoria) }
    var precio by remember(producto.id) { mutableStateOf(producto.precio.toString()) }
    var stock by remember(producto.id) { mutableStateOf(producto.stock.toString()) }
    var comision by remember(producto.id) { mutableStateOf(producto.comision.toString()) }
    var descripcion by remember(producto.id) { mutableStateOf(producto.descripcion) }

    AlertDialog(
        onDismissRequest = { if (!guardando) onCancelar() },
        title = { Text("Editar producto") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = nombre, onValueChange = { nombre = it }, label = { Text("Nombre") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = categoria, onValueChange = { categoria = it }, label = { Text("Categoría") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = precio,
                    onValueChange = { precio = it },
                    label = { Text("Precio (S/)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = stock,
                    onValueChange = { stock = it.filter { c -> c.isDigit() } },
                    label = { Text("Stock") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = comision,
                    onValueChange = { comision = it },
                    label = { Text("Comisión por unidad (S/)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(value = descripcion, onValueChange = { descripcion = it }, label = { Text("Descripción") }, modifier = Modifier.fillMaxWidth())
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(onClick = { onGuardar(nombre, categoria, precio, stock, comision, descripcion) }, enabled = !guardando) {
                if (guardando) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp) else Text("Guardar")
            }
        },
        dismissButton = { TextButton(onClick = onCancelar, enabled = !guardando) { Text("Cancelar") } }
    )
}
