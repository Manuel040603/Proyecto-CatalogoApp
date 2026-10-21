package com.catalogoapp.consultoras.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.catalogoapp.consultoras.data.model.Producto
import com.catalogoapp.consultoras.util.Inventario
import com.catalogoapp.consultoras.viewmodel.AdminProductosViewModel

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
