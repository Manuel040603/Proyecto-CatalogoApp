package com.catalogoapp.consultoras.ui.screens.profile

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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.catalogoapp.consultoras.util.AdminConfig
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

    val rolUsuario = perfilCargado?.rol ?: "CONSULTORA"
    val esSuperAdmin = rolUsuario == "SUPER_ADMIN"
    val esLogistica = rolUsuario == "LOGISTICA" || esSuperAdmin

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
                        text = if (esSuperAdmin) "Super Administrador" else "${perfilCargado?.rol ?: "Consultora"} · Nivel ${perfilCargado?.nivel ?: "Plata"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

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
                if (esLogistica) {
                    ProfileMenuOption(
                        label = "Devoluciones",
                        sublabel = "Registrar un producto devuelto y ver el historial",
                        icon = Icons.Default.Replay,
                        onClick = onIrADevolucion
                    )
                }
                if (esLogistica) {
                    ProfileMenuOption(
                        label = "Repartos y rutas",
                        sublabel = "Repartidores, repartos y mapa",
                        icon = Icons.Default.LocalShipping,
                        onClick = onIrAReparto
                    )
                }
                if (esSuperAdmin) {
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
