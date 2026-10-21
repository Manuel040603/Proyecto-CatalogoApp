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
import androidx.core.content.ContextCompat
import com.catalogoapp.consultoras.util.PedidoValidacion
import com.catalogoapp.consultoras.util.base64ABitmap
import com.catalogoapp.consultoras.util.bitmapABase64
import com.catalogoapp.consultoras.viewmodel.ProfileUiState
import com.catalogoapp.consultoras.viewmodel.ProfileViewModel
import com.google.firebase.auth.FirebaseUser

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
            .navigationBarsPadding()
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
