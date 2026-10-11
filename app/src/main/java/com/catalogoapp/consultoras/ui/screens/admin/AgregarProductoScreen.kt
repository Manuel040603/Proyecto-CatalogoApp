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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.catalogoapp.consultoras.util.bitmapABase64
import com.catalogoapp.consultoras.viewmodel.AgregarProductoUiState
import com.catalogoapp.consultoras.viewmodel.AgregarProductoViewModel

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
            label = { Text("Categoria") },
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
            label = { Text("Comision por unidad (S/, opcional)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = descripcion,
            onValueChange = { descripcion = it },
            label = { Text("Descripcion") },
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
