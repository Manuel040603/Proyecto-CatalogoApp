package com.catalogoapp.consultoras.ui.screens.profile

import android.Manifest
import android.graphics.Bitmap
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.catalogoapp.consultoras.util.AdminConfig
import com.catalogoapp.consultoras.util.bitmapABase64
import com.catalogoapp.consultoras.util.base64ABitmap
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
    onIrADevolucion: () -> Unit = {}
) {
    val context = LocalContext.current
    val estado by viewModel.uiState.collectAsState()
    val perfilCargado by viewModel.perfil.collectAsState()
    val estadisticas by viewModel.estadisticas.collectAsState()

    LaunchedEffect(usuario.uid) {
        viewModel.cargarPerfil(usuario.uid)
        viewModel.cargarEstadisticas()
        viewModel.registrarTokenFcm()
        viewModel.programarRecordatorioPedidos(context)
    }

    val esAdmin = remember(usuario.email) { AdminConfig.esAdmin(usuario.email) }

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

            // Header Minimalista Asimétrico
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
                        text = if (perfilCargado?.nombre.isNullOrBlank()) "Consultora" else perfilCargado?.nombre ?: "",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = usuario.email ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            when {
                                esAdmin -> "Administrador ⭐"
                                perfilCargado?.activo == false -> "Inactiva"
                                else -> "Consultora activa"
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Stats Section (Flat Design)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FlatStatItem(
                    modifier = Modifier.weight(1f),
                    label = "Ventas",
                    value = if (estadisticas.cargando) "..." else "S/ ${"%.2f".format(estadisticas.ventasTotales)}",
                    icon = Icons.Default.AttachMoney
                )
                FlatStatItem(
                    modifier = Modifier.weight(1f),
                    label = "Pedidos",
                    value = if (estadisticas.cargando) "..." else "${estadisticas.totalPedidos}",
                    icon = Icons.Default.List
                )
                FlatStatItem(
                    modifier = Modifier.weight(1f),
                    label = "Entregados",
                    value = if (estadisticas.cargando) "..." else "${estadisticas.pedidosEntregados}",
                    icon = Icons.Default.Star
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Botón Editar Perfil
            OutlinedButton(
                onClick = onIrAEditarPerfil,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Editar Perfil", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Soporte y Ajustes (Sustituye a Gestión)
            Text(
                "Soporte y Ajustes",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            )

            ProfileActionItem(
                label = "Centro de Ayuda",
                icon = Icons.Default.Help,
                onClick = { /* Implementar ayuda */ }
            )
            ProfileActionItem(
                label = "Términos y Condiciones",
                icon = Icons.Default.Description,
                onClick = { /* Implementar términos */ }
            )

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
fun FlatStatItem(modifier: Modifier = Modifier, label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
fun ProfileActionItem(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(20.dp))
    }
}
