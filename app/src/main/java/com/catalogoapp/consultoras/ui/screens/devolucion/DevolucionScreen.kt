package com.catalogoapp.consultoras.ui.screens.devolucion

import android.Manifest
import android.content.pm.PackageManager
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil3.compose.AsyncImage
import com.catalogoapp.consultoras.data.model.Devolucion
import com.catalogoapp.consultoras.data.model.Producto
import com.catalogoapp.consultoras.util.HoraLima
import com.catalogoapp.consultoras.viewmodel.DevolucionViewModel

val CoralColor = Color(0xFFE87D6E)
val BeigeBackground = Color(0xFFFDF8F0)
val DarkText = Color(0xFF2C2C2C)
val SurfaceCard = Color(0xFFFFFFFF)
val GreenAptoBg = Color(0xFFE8F5E9)
val GreenAptoText = Color(0xFF2E7D32)
val RedNoAptoBg = Color(0xFFFFEBEE)
val RedNoAptoText = Color(0xFFC62828)
val RedBannerBg = Color(0xFFB71C1C)

object DevolucionTypography {
    val topBarTitle = TextStyle(
        fontFamily = FontFamily.Serif,
        fontSize = 20.sp,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center,
        color = DarkText
    )
    val productName = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = DarkText
    )
    val category = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        color = Color.Gray
    )
    val metadata = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        color = Color.Gray
    )
    val mainButton = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color.White
    )
    val guideText = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = DarkText
    )
    val guideSubtext = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 13.sp,
        fontWeight = FontWeight.Normal,
        color = Color.Gray
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevolucionScreen(
    viewModel: DevolucionViewModel,
    onVolver: () -> Unit
) {
    val estado by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val lanzadorCamara = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap -> bitmap?.let { viewModel.registrarFoto(it) } }

    val lanzadorPermiso = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { permisoConcedido ->
        if (permisoConcedido) {
            lanzadorCamara.launch(null)
        }
    }

    var selectedTabIndex by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        containerColor = BeigeBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        if (estado.productoSeleccionado == null) "Lista de Devoluciones" else "Registrar Devolución", 
                        style = DevolucionTypography.topBarTitle
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = DarkText)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = BeigeBackground
                )
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
        ) {
            if (estado.productoSeleccionado == null) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Barra de búsqueda (Mock visual)
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Buscar producto o código CUV", color = Color.Gray, fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceCard,
                            unfocusedContainerColor = SurfaceCard,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                        ),
                        singleLine = true
                    )

                    TabRow(
                        selectedTabIndex = selectedTabIndex,
                        containerColor = BeigeBackground,
                        contentColor = CoralColor,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                                color = CoralColor,
                                height = 3.dp
                            )
                        }
                    ) {
                        Tab(
                            selected = selectedTabIndex == 0,
                            onClick = { selectedTabIndex = 0 },
                            text = { Text("Mis Devoluciones", fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Normal, color = if(selectedTabIndex==0) DarkText else Color.Gray) }
                        )
                        Tab(
                            selected = selectedTabIndex == 1,
                            onClick = { selectedTabIndex = 1 },
                            text = { Text("Historial", fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Normal, color = if(selectedTabIndex==1) DarkText else Color.Gray) }
                        )
                    }

                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (selectedTabIndex == 0) {
                            items(estado.productos) { producto ->
                                ProductoDevolucionCard(
                                    producto = producto,
                                    onDevolver = { viewModel.seleccionarProducto(producto) }
                                )
                            }
                        } else {
                            if (estado.cargandoHistorial) {
                                item {
                                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = CoralColor)
                                    }
                                }
                            } else if (estado.historial.isEmpty()) {
                                item {
                                    Text(
                                        "Aún no hay devoluciones registradas",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray,
                                        modifier = Modifier.padding(vertical = 16.dp).fillMaxWidth(),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            } else {
                                items(estado.historial) { devolucion ->
                                    TarjetaDevolucion(devolucion)
                                }
                            }
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (estado.productoSeleccionado?.imagenUrl.isNullOrEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFE0E0E0)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Inventory, contentDescription = null, tint = Color.Gray)
                                }
                            } else {
                                val url = estado.productoSeleccionado?.imagenUrl ?: ""
                                val imageModel = remember(url) {
                                    if (url.startsWith("http")) url
                                    else try {
                                        Base64.decode(url, Base64.DEFAULT)
                                    } catch (e: Exception) {
                                        url
                                    }
                                }
                                AsyncImage(
                                    model = imageModel,
                                    contentDescription = "Imagen producto",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFE0E0E0))
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "PRODUCTO",
                                    style = TextStyle(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    ),
                                    color = Color.Gray
                                )
                                Text(
                                    estado.productoSeleccionado?.nombre ?: "",
                                    style = DevolucionTypography.productName
                                )
                            }
                        }
                    }

                    if (estado.foto == null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp)
                                .drawBehind {
                                    val strokeWidth = 1.5.dp.toPx()
                                    val path = Path().apply {
                                        addRoundRect(
                                            RoundRect(
                                                left = 0f,
                                                top = 0f,
                                                right = size.width,
                                                bottom = size.height,
                                                cornerRadius = CornerRadius(16.dp.toPx())
                                            )
                                        )
                                    }
                                    drawPath(
                                        path = path,
                                        color = Color(0xFFBCAAA4), // un color gris/marrón claro para los dashes
                                        style = Stroke(
                                            width = strokeWidth,
                                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                        )
                                    )
                                }
                                .background(Color(0xFFEFEBE4), RoundedCornerShape(16.dp))
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    "Toma una foto clara donde se vea el\nenvase completo del producto",
                                    style = DevolucionTypography.guideText,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(bottom = 24.dp),
                                    lineHeight = 18.sp
                                )
                                Icon(
                                    Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = Color(0xFFD7CCC8)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "No hay foto registrada",
                                    style = DevolucionTypography.guideSubtext
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        Button(
                            onClick = {
                                val tienePermiso = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.CAMERA
                                ) == PackageManager.PERMISSION_GRANTED

                                if (tienePermiso) lanzadorCamara.launch(null)
                                else lanzadorPermiso.launch(Manifest.permission.CAMERA)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .padding(bottom = 16.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CoralColor),
                            contentPadding = PaddingValues(horizontal = 16.dp)
                        ) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Tomar Foto",
                                style = DevolucionTypography.mainButton
                            )
                        }
                    } else {
                        // Foto tomada
                        Image(
                            bitmap = estado.foto!!.asImageBitmap(),
                            contentDescription = "Foto tomada",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(16.dp))
                        )
                        
                        Button(
                            onClick = {
                                val tienePermiso = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.CAMERA
                                ) == PackageManager.PERMISSION_GRANTED

                                if (tienePermiso) lanzadorCamara.launch(null)
                                else lanzadorPermiso.launch(Manifest.permission.CAMERA)
                            },
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .height(40.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD7CCC8).copy(alpha=0.6f)),
                            contentPadding = PaddingValues(horizontal = 16.dp)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp), tint = DarkText)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Cambiar Foto", style = DevolucionTypography.mainButton.copy(color = DarkText, fontSize = 13.sp))
                        }

                        if (estado.resultadoApto == null) {
                            Button(
                                onClick = { viewModel.clasificarProducto() },
                                enabled = !estado.clasificando,
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CoralColor)
                            ) {
                                if (estado.clasificando) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                                } else {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Clasificar con IA", style = DevolucionTypography.mainButton)
                                }
                            }
                        }

                        estado.resultadoApto?.let { apto ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (apto) GreenAptoBg else RedNoAptoBg
                                ),
                                border = if (apto) null else BorderStroke(1.dp, Color(0xFFFFCDD2))
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (!apto) {
                                            Icon(Icons.Default.Warning, contentDescription = null, tint = RedNoAptoText, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                        }
                                        Text(
                                            if (apto) "APTO" else "NO APTO",
                                            style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (apto) GreenAptoText else RedNoAptoText)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (apto) GreenAptoText else RedBannerBg)
                                            .padding(vertical = 8.dp, horizontal = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            if (apto) "Resultado: Producto reconocido correctamente" 
                                            else "Resultado: No pudimos reconocer el producto",
                                            color = Color.White,
                                            style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                                            textAlign = TextAlign.Center
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        if (apto) "El producto coincide con ${estado.productoSeleccionado?.nombre}. Puedes proceder con la devolución."
                                        else "No coincide con ${estado.productoSeleccionado?.nombre} (Detectado: ${estado.etiquetaDetectada}). Por favor, intenta tomar la foto con mejor luz sobre una superficie plana.",
                                        style = TextStyle(fontSize = 12.sp, color = DarkText),
                                        textAlign = TextAlign.Center
                                    )

                                    if (apto) {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Button(
                                            onClick = { viewModel.confirmarActualizacionInventario() },
                                            enabled = !estado.actualizando,
                                            modifier = Modifier.fillMaxWidth().height(48.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = CoralColor)
                                        ) {
                                            if (estado.actualizando) {
                                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                                            } else {
                                                Text("Confirmar Devolución", style = DevolucionTypography.mainButton)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        estado.mensaje?.let { mensaje ->
                            Text(mensaje, color = GreenAptoText, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top=8.dp))
                            Button(onClick = { viewModel.reiniciar() }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), colors = ButtonDefaults.buttonColors(containerColor = CoralColor)) {
                                Text("Registrar otra devolución")
                            }
                        }

                        estado.error?.let { error ->
                            Text(error, color = RedNoAptoText, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProductoDevolucionCard(producto: Producto, onDevolver: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (producto.imagenUrl.isEmpty()) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFE0E0E0)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, tint = Color.Gray)
                }
            } else {
                val imageModel = remember(producto.imagenUrl) {
                    if (producto.imagenUrl.startsWith("http")) producto.imagenUrl
                    else try {
                        Base64.decode(producto.imagenUrl, Base64.DEFAULT)
                    } catch (e: Exception) {
                        producto.imagenUrl
                    }
                }
                AsyncImage(
                    model = imageModel,
                    contentDescription = producto.nombre,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFE0E0E0))
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(producto.nombre, style = DevolucionTypography.productName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(producto.categoria, style = DevolucionTypography.category)
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onDevolver,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CoralColor),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text("Devolver", style = DevolucionTypography.mainButton.copy(fontSize = 13.sp))
            }
        }
    }
}

@Composable
private fun TarjetaDevolucion(devolucion: Devolucion) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Un box simulando imagen para el historial ya que Devolucion no tiene imagen
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFE0E0E0)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Inventory, contentDescription = null, tint = Color.Gray)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(devolucion.productoNombre, style = DevolucionTypography.productName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(HoraLima.formatear(devolucion.fecha), style = DevolucionTypography.category)
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (devolucion.apto) GreenAptoBg else RedNoAptoBg)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (devolucion.apto) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = GreenAptoText, modifier = Modifier.size(14.dp))
                    } else {
                        Icon(Icons.Default.Close, contentDescription = null, tint = RedNoAptoText, modifier = Modifier.size(14.dp))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        if (devolucion.apto) "APTO" else "NO APTO",
                        style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (devolucion.apto) GreenAptoText else RedNoAptoText)
                    )
                }
            }
        }
    }
}