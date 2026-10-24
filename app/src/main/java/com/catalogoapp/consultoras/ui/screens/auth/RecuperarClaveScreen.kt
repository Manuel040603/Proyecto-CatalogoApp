package com.catalogoapp.consultoras.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.catalogoapp.consultoras.viewmodel.AuthUiState
import com.catalogoapp.consultoras.viewmodel.AuthViewModel

// Pantalla de recuperación de clave
@Composable
fun RecuperarClaveScreen(
    viewModel: AuthViewModel,
    onVolver: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    val estado by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.resetEstado()
    }
    DisposableEffect(Unit) {
        onDispose { viewModel.resetEstado() }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Recuperar contraseña", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Tu correo registrado") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = { viewModel.enviarCorreoRecuperacion(email.trim()) },
            enabled = estado !is AuthUiState.Cargando && email.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Enviar correo de recuperación")
        }

        TextButton(onClick = onVolver) {
            Text("Volver al inicio de sesión")
        }

        when (val actual = estado) {
            is AuthUiState.Exito -> Text(actual.mensaje, color = MaterialTheme.colorScheme.primary)
            is AuthUiState.Error -> Text(actual.mensaje, color = MaterialTheme.colorScheme.error)
            else -> {}
        }
    }
}
