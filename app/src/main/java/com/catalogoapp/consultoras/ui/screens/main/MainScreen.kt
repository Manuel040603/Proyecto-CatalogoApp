package com.catalogoapp.consultoras.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.catalogoapp.consultoras.navigation.Rutas

@Composable
fun MainScreen(
    navController: NavHostController,
    content: @Composable () -> Unit
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = currentRoute == Rutas.HOME,
                    onClick = {
                        navController.navigate(Rutas.HOME) {
                            popUpTo(Rutas.HOME) { inclusive = true }
                        }
                    },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Inicio") },
                    label = { Text("Inicio") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.outline,
                        unselectedTextColor = MaterialTheme.colorScheme.outline
                    )
                )
                NavigationBarItem(
                    selected = currentRoute == Rutas.CATALOGO,
                    onClick = { navController.navigate(Rutas.CATALOGO) { launchSingleTop = true } },
                    icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "Catálogo") },
                    label = { Text("Catálogo") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.outline,
                        unselectedTextColor = MaterialTheme.colorScheme.outline
                    )
                )
                NavigationBarItem(
                    selected = currentRoute == Rutas.PEDIDOS,
                    onClick = { navController.navigate(Rutas.PEDIDOS) { launchSingleTop = true } },
                    icon = { Icon(Icons.Default.ListAlt, contentDescription = "Pedidos") },
                    label = { Text("Pedidos") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.outline,
                        unselectedTextColor = MaterialTheme.colorScheme.outline
                    )
                )
                NavigationBarItem(
                    selected = currentRoute == Rutas.PERFIL,
                    onClick = { navController.navigate(Rutas.PERFIL) { launchSingleTop = true } },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Perfil") },
                    label = { Text("Perfil") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.outline,
                        unselectedTextColor = MaterialTheme.colorScheme.outline
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            content()
        }
    }
}
