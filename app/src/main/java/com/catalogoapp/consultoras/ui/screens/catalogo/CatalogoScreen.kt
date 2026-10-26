package com.catalogoapp.consultoras.ui.screens.catalogo

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.catalogoapp.consultoras.data.model.Producto
import com.catalogoapp.consultoras.util.BusquedaPorFoto
import com.catalogoapp.consultoras.viewmodel.CatalogoViewModel
import kotlinx.coroutines.launch

@Composable
fun CatalogoScreen(
    viewModel: CatalogoViewModel,
    onVolver: () -> Unit,
    onProductoClick: (Producto) -> Unit,
    cantidadCarrito: Int,
    onAbrirCarrito: () -> Unit
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
                        Toast.makeText(contexto, "No se reconoció un producto del catálogo. Detectado: $detectado", Toast.LENGTH_LONG).show()
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Catálogo",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            BotonCarrito(cantidad = cantidadCarrito, onClick = onAbrirCarrito)
        }

        // BARRA DE BÚSQUEDA: Aquí el usuario escribe el producto que desea
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
                    //  BOTÓN DE CÁMARA: Permite buscar productos tomando una foto
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
                    //  BOTÓN DEL MICRÓFONO: Abre el reconocimiento de voz de Google
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

        val categorias = viewModel.categoriasDisponibles()
        
        //  CHIPS DE CATEGORÍAS: Filtra los productos rápidamente por categoría
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
        // filtro por texto
        val filteredProducts = remember(estado.productos, searchQuery) {
            estado.productos.filter {
                it.nombre.contains(searchQuery, ignoreCase = true) ||
                it.categoria.contains(searchQuery, ignoreCase = true)
            }
        }

        if (filteredProducts.isEmpty()) {
            //  MENSAJE DE VACÍO: Muestra feedback cuando la búsqueda no encuentra nada
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No se encontraron productos", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            // GRILLA DE PRODUCTOS: Lista en 2 columnas usando LazyVerticalGrid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filteredProducts, key = { it.id }) { producto ->
                    RetailProductCard(producto, onClick = { onProductoClick(producto) })
                }
            }
        }
    }
}
