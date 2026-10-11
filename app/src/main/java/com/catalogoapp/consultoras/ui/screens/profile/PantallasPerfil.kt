package com.catalogoapp.consultoras.ui.screens.profile

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.catalogoapp.consultoras.util.AdminConfig
import com.catalogoapp.consultoras.util.PedidoValidacion
import com.catalogoapp.consultoras.util.base64ABitmap
import com.catalogoapp.consultoras.util.bitmapABase64
import com.catalogoapp.consultoras.viewmodel.ProfileUiState
import com.catalogoapp.consultoras.viewmodel.ProfileViewModel
import com.google.firebase.auth.FirebaseUser

@Composable
fun ProfileScreen(
    usuario: FirebaseUser,
    viewModel: ProfileViewModel,
    onCerrarSesion: () -> Unit,
    onIrAEditarPerfil: () -> Unit = {},
    onVerCatalogo: () -> Unit = {},
    onIrACapacitaciones: () -> Unit = {},
    onIrAAdminConsultoras: () -> Unit = {},
    onIrAPedidos: () -> Unit = {},
    onIrAReparto: () -> Unit = {},
    onIrAAgregarProducto: () -> Unit = {},
    onIrAAdminPedidos: () -> Unit = {},
    onIrAComisiones: () -> Unit = {},
    onIrADevolucion: () -> Unit = {},
    onIrAProgreso: () -> Unit = {},
    onIrAAjustesIA: () -> Unit = {},
    onIrAApariencia: () -> Unit = {}
) {
    val context = LocalContext.current
    val estado by viewModel.uiState.collectAsState()
    val perfilCargado by viewModel.perfil.collectAsState()

    LaunchedEffect(usuario.uid) {
        viewModel.cargarPerfil(usuario.uid)
        viewModel.registrarTokenFcm()
        viewModel.programarRecordatorioPedidos(context)
        AdminConfig.cargarRol(usuario.email)
    }

    val administradores by AdminConfig.administradores.collectAsState()
    val esAdmin = remember(usuario.email, administradores) { AdminConfig.esAdmin(usuario.email) }

    val fotoGuardada = remember(perfilCargado) {
        perfilCargado?.fotoBase64?.takeIf { it.isNotBlank() }?.let { base64ABitmap(it) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            // Cabecera v3
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (fotoGuardada != null) {
                        Image(
                            bitmap = fotoGuardada.asImageBitmap(),
                            contentDescription = "Foto de perfil",
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(20.dp))

                Column {
                    Text(
                        text = perfilCargado?.nombre?.takeIf { it.isNotBlank() }
                            ?: usuario.email?.substringBefore('@')
                            ?: "Consultora",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = if (esAdmin) "Administrador" else "${perfilCargado?.rol ?: "Consultora"} · Nivel ${perfilCargado?.nivel ?: "Plata"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Menú de Opciones v3 (Filas con subtítulos)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ProfileMenuOption(
                    label = "Datos y dirección",
                    sublabel = "DNI, celular, entrega y comprobante",
                    icon = Icons.Default.Person,
                    onClick = onIrAEditarPerfil
                )
                ProfileMenuOption(
                    label = "Mi progreso",
                    sublabel = "${perfilCargado?.nivel ?: "Plata"} · ${"%.0f".format((perfilCargado?.progreso ?: 0.0) * 100)}%",
                    icon = Icons.Default.TrendingUp,
                    onClick = onIrAProgreso
                )
                ProfileMenuOption(
                    label = "Capacitación",
                    sublabel = "Lecciones y Quizzes",
                    icon = Icons.Default.School,
                    onClick = onIrACapacitaciones
                )
                ProfileMenuOption(
                    label = "Asistente IA",
                    sublabel = "Círculo y funciones",
                    icon = Icons.Default.AutoAwesome,
                    onClick = onIrAAjustesIA
                )
                ProfileMenuOption(
                    label = "Apariencia",
                    sublabel = "Claro, oscuro o negro",
                    icon = Icons.Default.Palette,
                    onClick = onIrAApariencia
                )
                ProfileMenuOption(
                    label = "Comisiones",
                    sublabel = "Lo que ganas por tus pedidos entregados",
                    icon = Icons.Default.AccountBalanceWallet,
                    onClick = onIrAComisiones
                )
                ProfileMenuOption(
                    label = "Devoluciones",
                    sublabel = "Registrar un producto devuelto y ver el historial",
                    icon = Icons.Default.Replay,
                    onClick = onIrADevolucion
                )
                if (esAdmin) {
                    ProfileMenuOption(
                        label = "Repartos y rutas",
                        sublabel = "Repartidores, repartos y mapa",
                        icon = Icons.Default.LocalShipping,
                        onClick = onIrAReparto
                    )
                    ProfileMenuOption(
                        label = "Administrar productos",
                        sublabel = "Agregar, editar, eliminar y ver stock",
                        icon = Icons.Default.AddBox,
                        onClick = onIrAAgregarProducto
                    )
                    ProfileMenuOption(
                        label = "Todos los pedidos",
                        sublabel = "Pedidos de todas las consultoras",
                        icon = Icons.Default.ListAlt,
                        onClick = onIrAAdminPedidos
                    )
                    ProfileMenuOption(
                        label = "Consultoras",
                        sublabel = "Ver, editar y activar o desactivar cuentas",
                        icon = Icons.Default.Group,
                        onClick = onIrAAdminConsultoras
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            OutlinedButton(
                onClick = onCerrarSesion,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cerrar Sesión", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Feedback de guardado
            when (val actual = estado) {
                is ProfileUiState.Guardado -> {
                    Text(actual.mensaje, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
                is ProfileUiState.Error -> {
                    Text(actual.mensaje, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
                else -> {}
            }
        }
    }
}

@Composable
fun ProfileMenuOption(
    label: String,
    sublabel: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
            Text(sublabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(
            Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun EditProfileScreen(
    usuario: FirebaseUser,
    viewModel: ProfileViewModel,
    onVolver: () -> Unit
) {
    val context = LocalContext.current
    var nombre by remember { mutableStateOf("") }
    var dni by remember { mutableStateOf("") }
    var celular by remember { mutableStateOf("") }
    var direccion by remember { mutableStateOf("") }
    var distrito by remember { mutableStateOf("") }
    var referencia by remember { mutableStateOf("") }
    var tipoComprobante by remember { mutableStateOf("Boleta") }
    var preferencias by remember { mutableStateOf("") }
    var errorFormulario by remember { mutableStateOf<String?>(null) }
    var fotoNueva by remember { mutableStateOf<Bitmap?>(null) }
    val estado by viewModel.uiState.collectAsState()
    val perfilCargado by viewModel.perfil.collectAsState()

    LaunchedEffect(usuario.uid) {
        viewModel.cargarPerfil(usuario.uid)
    }

    LaunchedEffect(perfilCargado) {
        perfilCargado?.let { perfil ->
            nombre = perfil.nombre
            preferencias = perfil.preferencias
            dni = perfil.dni
            celular = perfil.celular
            direccion = perfil.direccionEntrega
            distrito = perfil.distrito
            referencia = perfil.referencia
            tipoComprobante = perfil.tipoComprobante
        }
    }

    val fotoGuardada = remember(perfilCargado) {
        perfilCargado?.fotoBase64?.takeIf { it.isNotBlank() }?.let { base64ABitmap(it) }
    }

    val lanzadorCamara = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap -> fotoNueva = bitmap }

    val lanzadorPermisoCamara = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { concedido -> if (concedido) lanzadorCamara.launch(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
            IconButton(onClick = onVolver) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "Datos y Dirección",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Foto de Perfil
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .clickable {
                    val permisoOk = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED
                    if (permisoOk) lanzadorCamara.launch(null) else lanzadorPermisoCamara.launch(Manifest.permission.CAMERA)
                },
            contentAlignment = Alignment.Center
        ) {
            val fotoAMostrar = fotoNueva ?: fotoGuardada
            if (fotoAMostrar != null) {
                Image(
                    bitmap = fotoAMostrar.asImageBitmap(),
                    contentDescription = "Foto de perfil",
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            } else {
                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(60.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .border(2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text("Información Personal", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), modifier = Modifier.align(Alignment.Start), color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = { Text("Nombres y Apellidos") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = dni,
                onValueChange = { dni = PedidoValidacion.soloDigitos(it, 8) },
                label = { Text("DNI") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = celular,
                onValueChange = { celular = PedidoValidacion.soloDigitos(it, 9) },
                label = { Text("Celular") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = preferencias,
            onValueChange = { preferencias = it },
            label = { Text("Preferencias (categorías o productos que más vendes)") },
            minLines = 2,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text("Dirección de Entrega", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), modifier = Modifier.align(Alignment.Start), color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = direccion,
            onValueChange = { direccion = it },
            label = { Text("Dirección completa") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = distrito,
                onValueChange = { distrito = it },
                label = { Text("Distrito") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = referencia,
                onValueChange = { referencia = it },
                label = { Text("Referencia") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Tipo de Comprobante", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), modifier = Modifier.align(Alignment.Start), color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf("Boleta", "Factura").forEach { tipo ->
                FilterChip(
                    selected = tipoComprobante == tipo,
                    onClick = { tipoComprobante = tipo },
                    label = { Text(tipo) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        errorFormulario?.let { mensajeError ->
            Text(mensajeError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(12.dp))
        }

        Button(
            onClick = {
                val problema = when {
                    dni.isNotEmpty() && dni.length != 8 -> "El DNI debe tener 8 dígitos"
                    celular.isNotEmpty() && PedidoValidacion.validarCelular(celular) != null ->
                        "El celular debe tener 9 dígitos y empezar con 9"
                    else -> null
                }
                if (problema != null) {
                    errorFormulario = problema
                } else {
                    errorFormulario = null
                    val fotoParaGuardar = fotoNueva?.let { bitmapABase64(it) } ?: perfilCargado?.fotoBase64 ?: ""
                    viewModel.guardarPerfil(
                        context = context,
                        uid = usuario.uid,
                        email = usuario.email ?: "",
                        nombre = nombre,
                        preferencias = preferencias.trim(),
                        fotoBase64 = fotoParaGuardar,
                        dni = dni,
                        celular = celular,
                        direccionEntrega = direccion,
                        distrito = distrito,
                        referencia = referencia,
                        tipoComprobante = tipoComprobante
                    )
                    onVolver()
                }
            },
            enabled = estado !is ProfileUiState.Guardando && nombre.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            if (estado is ProfileUiState.Guardando) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
            } else {
                Text("Guardar Cambios", fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun AppearanceScreen(
    currentTheme: String,
    onThemeSelected: (String) -> Unit,
    onVolver: () -> Unit
) {
    val themes = listOf(
        ThemeOption("Claro", "Rosa tonal", "light"),
        ThemeOption("Oscuro", "Fondo oscuro, rosa suave", "dark"),
        ThemeOption("Negro", "Monocromático, el color lo ponen los productos", "black"),
        ThemeOption("Automático", "Sigue el teléfono", "system")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Barra Superior v3
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onVolver) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                }
                Text(
                    text = "Apariencia",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Text(
                "Selecciona un tema",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                themes.forEach { theme ->
                    ThemeCard(
                        option = theme,
                        isSelected = currentTheme == theme.id,
                        onClick = { onThemeSelected(theme.id) }
                    )
                }
            }
        }
    }
}

data class ThemeOption(val label: String, val description: String, val id: String)

@Composable
fun ThemeCard(
    option: ThemeOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
        ),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Vista previa del tema (Simulada con un círculo de color)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(getThemePreviewColor(option.id)),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = option.label,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = option.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun getThemePreviewColor(id: String): Color {
    return when (id) {
        "light" -> Color(0xFFFFF8FA)
        "dark" -> Color(0xFF1B1317)
        "black" -> Color.Black
        else -> Color.Gray
    }
}
