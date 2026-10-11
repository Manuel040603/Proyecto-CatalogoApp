package com.catalogoapp.consultoras.ui.screens.catalogo

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.catalogoapp.consultoras.util.BusquedaPorFoto
import com.catalogoapp.consultoras.util.Inventario
import kotlinx.coroutines.launch
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.catalogoapp.consultoras.data.model.Producto
import com.catalogoapp.consultoras.util.base64ABitmap
import com.catalogoapp.consultoras.viewmodel.CatalogoViewModel

@Composable
fun CatalogoScreen(
    viewModel: CatalogoViewModel,
    onVolver: () -> Unit,
    onProductoClick: (Producto) -> Unit
) {
    val estado by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var analizandoFoto by remember { mutableStateOf(false) }
    val contexto = LocalContext.current
    val alcance = rememberCoroutineScope()

    val lanzadorVoz = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { resultado ->
        val texto = resultado.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
        if (!texto.isNullOrBlank()) searchQuery = texto.trim()
    }

    val lanzadorCamara = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            analizandoFoto = true
            alcance.launch {
                try {
                    val deteccion = BusquedaPorFoto.analizar(bitmap)
                    if (deteccion.palabraClave != null) {
                        searchQuery = deteccion.palabraClave
                        Toast.makeText(contexto, "Detectado: ${deteccion.etiquetas.first()}", Toast.LENGTH_SHORT).show()
                    } else {
                        val detectado = if (deteccion.etiquetas.isEmpty()) "nada con claridad" else deteccion.etiquetas.take(3).joinToString(", ")
                        Toast.makeText(contexto, "No se reconocio un producto del catalogo. Detectado: $detectado", Toast.LENGTH_LONG).show()
                    }
                } catch (excepcion: Exception) {
                    Toast.makeText(contexto, "No se pudo analizar la foto", Toast.LENGTH_SHORT).show()
                } finally {
                    analizandoFoto = false
                }
            }
        }
    }

    val lanzadorPermisoCamara = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (concedido) lanzadorCamara.launch(null)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {
        // Barra Superior v3
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Se eliminó la flecha de volver para limpiar la interfaz según lo solicitado
        }

        // Buscador v3 con iconos de IA
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            placeholder = { Text("Buscar productos...", style = MaterialTheme.typography.bodyMedium) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            trailingIcon = {
                Row {
                    IconButton(
                        enabled = !analizandoFoto,
                        onClick = {
                            val permisoOk = ContextCompat.checkSelfPermission(
                                contexto, Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED
                            if (permisoOk) lanzadorCamara.launch(null) else lanzadorPermisoCamara.launch(Manifest.permission.CAMERA)
                        }
                    ) {
                        if (analizandoFoto) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.PhotoCamera, contentDescription = "Buscar por foto", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    IconButton(onClick = {
                        val intento = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-PE")
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Di el producto que buscas")
                        }
                        try {
                            lanzadorVoz.launch(intento)
                        } catch (excepcion: ActivityNotFoundException) {
                            Toast.makeText(contexto, "Este dispositivo no tiene reconocimiento de voz", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(Icons.Default.Mic, contentDescription = "Búsqueda por voz", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            },
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )

        if (estado.cargando) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            return@Column
        }

        // Chips de Categoría v3
        val categorias = viewModel.categoriasDisponibles()
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item {
                FilterChip(
                    selected = estado.categoriaSeleccionada == null,
                    onClick = { viewModel.filtrarPorCategoria(null) },
                    label = { Text("Todo") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
            items(categorias) { categoria ->
                FilterChip(
                    selected = estado.categoriaSeleccionada == categoria,
                    onClick = { viewModel.filtrarPorCategoria(categoria) },
                    label = { Text(categoria) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        }

        val filteredProducts = estado.productos.filter {
            it.nombre.contains(searchQuery, ignoreCase = true) ||
            it.categoria.contains(searchQuery, ignoreCase = true)
        }

        if (filteredProducts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No se encontraron productos", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filteredProducts) { producto ->
                    RetailProductCard(producto, onClick = { onProductoClick(producto) })
                }
            }
        }
    }
}

@Composable
fun RetailProductCard(producto: Producto, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(Modifier.clickable { onClick() }),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                // Badge de "NUEVO" v3
                if (producto.esNuevo) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            "NUEVO",
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }

                val etiquetaStock = Inventario.etiqueta(producto.stock)
                if (etiquetaStock != null) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                        color = MaterialTheme.colorScheme.error,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            etiquetaStock,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }

                when {
                    producto.imagenUrl.startsWith("http") -> {
                        AsyncImage(
                            model = producto.imagenUrl,
                            contentDescription = producto.nombre,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    producto.imagenUrl.isNotBlank() -> {
                        val bitmap = remember(producto.imagenUrl) { base64ABitmap(producto.imagenUrl) }
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = producto.nombre,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
                        }
                    }
                    else -> {
                        Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
                    }
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = producto.nombre,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "S/ ${"%.2f".format(producto.precio)}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (producto.comision > 0) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Gana S/ ${"%.2f".format(producto.comision)}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
