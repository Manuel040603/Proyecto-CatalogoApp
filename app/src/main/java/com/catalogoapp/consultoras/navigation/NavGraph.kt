package com.catalogoapp.consultoras.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.catalogoapp.consultoras.ui.screens.admin.AdminConsultorasScreen
import com.catalogoapp.consultoras.ui.screens.admin.AgregarProductoScreen
import com.catalogoapp.consultoras.ui.screens.auth.ForgotPasswordScreen
import com.catalogoapp.consultoras.ui.screens.auth.LoginScreen
import com.catalogoapp.consultoras.ui.screens.auth.RegisterScreen
import com.catalogoapp.consultoras.ui.screens.capacitacion.CapacitacionDetalleScreen
import com.catalogoapp.consultoras.ui.screens.capacitacion.CapacitacionListScreen
import com.catalogoapp.consultoras.ui.screens.capacitacion.NuevaCapacitacionScreen
import com.catalogoapp.consultoras.ui.screens.catalogo.CatalogoScreen
import com.catalogoapp.consultoras.ui.screens.catalogo.DetalleProductoScreen
import com.catalogoapp.consultoras.ui.screens.reparto.SeguimientoPedidoScreen
import com.catalogoapp.consultoras.ui.screens.devolucion.DevolucionScreen
import com.catalogoapp.consultoras.ui.screens.home.HomeScreen
import com.catalogoapp.consultoras.ui.screens.main.MainScreen
import com.catalogoapp.consultoras.ui.screens.pedido.NuevoPedidoScreen
import com.catalogoapp.consultoras.ui.screens.pedido.PedidoListScreen
import com.catalogoapp.consultoras.ui.screens.profile.AppearanceScreen
import com.catalogoapp.consultoras.ui.screens.profile.ProfileScreen
import com.catalogoapp.consultoras.util.ThemeManager
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.getValue

import com.catalogoapp.consultoras.ui.screens.profile.EditProfileScreen
import com.catalogoapp.consultoras.ui.screens.reparto.CrearRepartoScreen
import com.catalogoapp.consultoras.ui.screens.reparto.ListaRepartosScreen
import com.catalogoapp.consultoras.ui.screens.reparto.RepartoDetalleScreen
import com.catalogoapp.consultoras.util.AdminConfig
import com.catalogoapp.consultoras.viewmodel.*

object Rutas {
    const val LOGIN = "login"
    const val REGISTRO = "registro"
    const val RECUPERAR = "recuperar"
    const val HOME = "home"
    const val PERFIL = "perfil"
    const val EDITAR_PERFIL = "editar_perfil"
    const val APARIENCIA = "apariencia"
    const val SEGUIMIENTO_PEDIDO = "seguimiento_pedido"

    const val CATALOGO = "catalogo"
    const val DETALLE_PRODUCTO = "detalle_producto"
    const val DEVOLUCION = "devolucion"
    const val CAPACITACIONES = "capacitaciones"
    const val NUEVA_CAPACITACION = "nueva_capacitacion"
    const val CAPACITACION_DETALLE = "capacitacion_detalle"
    const val ADMIN_CONSULTORAS = "admin_consultoras"
    const val AGREGAR_PRODUCTO = "agregar_producto"
    const val PEDIDOS = "pedidos"
    const val NUEVO_PEDIDO = "nuevo_pedido"
    const val LISTA_REPARTOS = "lista_repartos"
    const val CREAR_REPARTO = "crear_reparto"
    const val REPARTO_DETALLE = "reparto_detalle"
}

@Composable
private fun PantallaConEspacioSuperior(contenido: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        contenido()
    }
}

@Composable
fun CatalogoAppNavHost(authViewModel: AuthViewModel) {
    val navController: NavHostController = rememberNavController()
    val context = LocalContext.current
    val profileViewModel = remember { ProfileViewModel() }
    val pedidoViewModel = remember { PedidoViewModel() }
    val repartoViewModel = remember { RepartoViewModel() }
    val homeViewModel = remember { HomeViewModel() }
    // Simplificamos el manejo del tema para evitar bloqueos al inicio
    var currentTheme by remember { mutableStateOf("system") }
    val scope = rememberCoroutineScope()
    val themeManager = remember { ThemeManager(context) }

    LaunchedEffect(Unit) {
        themeManager.themeFlow.collect { theme ->
            currentTheme = theme
        }
    }


    val destinoInicial = if (authViewModel.usuarioActual != null) Rutas.HOME else Rutas.LOGIN

    NavHost(navController = navController, startDestination = destinoInicial) {

        composable(Rutas.LOGIN) {
            PantallaConEspacioSuperior {
                LoginScreen(
                    viewModel = authViewModel,
                    onLoginExitoso = {
                        navController.navigate(Rutas.HOME) {
                            popUpTo(Rutas.LOGIN) { inclusive = true }
                        }
                    },
                    onIrARegistro = { navController.navigate(Rutas.REGISTRO) },
                    onIrARecuperar = { navController.navigate(Rutas.RECUPERAR) }
                )
            }
        }

        composable(Rutas.REGISTRO) {
            PantallaConEspacioSuperior {
                RegisterScreen(
                    viewModel = authViewModel,
                    onRegistroExitoso = { navController.popBackStack() },
                    onIrALogin = { navController.popBackStack() }
                )
            }
        }

        composable(Rutas.RECUPERAR) {
            PantallaConEspacioSuperior {
                ForgotPasswordScreen(
                    viewModel = authViewModel,
                    onVolver = { navController.popBackStack() }
                )
            }
        }

        composable(Rutas.HOME) {
            val uid = authViewModel.usuarioActual?.uid ?: ""
            MainScreen(navController = navController) {
                HomeScreen(
                    uid = uid,
                    viewModel = homeViewModel,
                    onNavegarACatalogo = { navController.navigate(Rutas.CATALOGO) },
                    onNavegarAPedidos = { navController.navigate(Rutas.PEDIDOS) },
                    onNavegarACapacitacion = { navController.navigate(Rutas.CAPACITACIONES) },
                    onNavegarAPerfil = { navController.navigate(Rutas.PERFIL) }
                )
            }
        }

        composable(Rutas.PERFIL) {
            val usuario = authViewModel.usuarioActual
            if (usuario != null) {
                MainScreen(navController = navController) {
                    ProfileScreen(
                        usuario = usuario,
                        viewModel = profileViewModel,
                        onCerrarSesion = {
                            authViewModel.cerrarSesion()
                            navController.navigate(Rutas.LOGIN) {
                                popUpTo(0)
                            }
                        },
                        onIrAEditarPerfil = { navController.navigate(Rutas.EDITAR_PERFIL) },
                        onVerCatalogo = { navController.navigate(Rutas.CATALOGO) },
                        onIrACapacitaciones = { navController.navigate(Rutas.CAPACITACIONES) },
                        onIrAAdminConsultoras = { navController.navigate(Rutas.ADMIN_CONSULTORAS) },
                        onIrAPedidos = { navController.navigate(Rutas.PEDIDOS) },
                        onIrAReparto = { navController.navigate(Rutas.LISTA_REPARTOS) },
                        onIrAAgregarProducto = { navController.navigate(Rutas.AGREGAR_PRODUCTO) },
                        onIrADevolucion = { navController.navigate(Rutas.DEVOLUCION) },
                        onIrAApariencia = { navController.navigate(Rutas.APARIENCIA) }
                    )
                }
            }
        }

        composable(Rutas.EDITAR_PERFIL) {
            val usuario = authViewModel.usuarioActual
            if (usuario != null) {
                PantallaConEspacioSuperior {
                    EditProfileScreen(
                        usuario = usuario,
                        viewModel = profileViewModel,
                        onVolver = { navController.popBackStack() }
                    )
                }
            } else {
                navController.navigate(Rutas.LOGIN)
            }
        }

        composable(Rutas.APARIENCIA) {
            PantallaConEspacioSuperior {
                AppearanceScreen(
                    currentTheme = currentTheme,
                    onThemeSelected = { tema ->
                        scope.launch {
                            themeManager.setTheme(tema)
                        }
                    },
                    onVolver = { navController.popBackStack() }
                )
            }
        }

        composable(Rutas.AGREGAR_PRODUCTO) {
            val agregarProductoViewModel = remember { AgregarProductoViewModel() }
            PantallaConEspacioSuperior {
                AgregarProductoScreen(
                    viewModel = agregarProductoViewModel,
                    onVolver = { navController.popBackStack() }
                )
            }
        }

        composable(Rutas.PEDIDOS) {
            MainScreen(navController = navController) {
                PedidoListScreen(
                    viewModel = pedidoViewModel,
                    onVolver = { navController.popBackStack() },
                    onNuevoPedido = { navController.navigate(Rutas.NUEVO_PEDIDO) },
                    onRastrearPedido = { pedidoId ->
                        navController.navigate("${Rutas.SEGUIMIENTO_PEDIDO}/$pedidoId")
                    }
                )
            }
        }

        composable(Rutas.NUEVO_PEDIDO) {
            PantallaConEspacioSuperior {
                NuevoPedidoScreen(
                    viewModel = pedidoViewModel,
                    onVolver = { navController.popBackStack() },
                    onPedidoCreado = {
                        // Navegamos al seguimiento del pedido recién creado
                        // Por ahora usamos un ID genérico "1048" para demo
                        navController.navigate("${Rutas.SEGUIMIENTO_PEDIDO}/1048") {
                            popUpTo(Rutas.NUEVO_PEDIDO) { inclusive = true }
                        }
                    }
                )
            }
        }

        composable(Rutas.LISTA_REPARTOS) {
            PantallaConEspacioSuperior {
                ListaRepartosScreen(
                    viewModel = repartoViewModel,
                    onVolver = { navController.popBackStack() },
                    onNuevoReparto = { navController.navigate(Rutas.CREAR_REPARTO) },
                    onAbrirReparto = { idReparto -> navController.navigate("${Rutas.REPARTO_DETALLE}/$idReparto") }
                )
            }
        }

        composable(Rutas.CREAR_REPARTO) {
            PantallaConEspacioSuperior {
                CrearRepartoScreen(
                    viewModel = repartoViewModel,
                    onVolver = { navController.popBackStack() },
                    onRepartoCreado = { idReparto ->
                        navController.navigate("${Rutas.REPARTO_DETALLE}/$idReparto") {
                            popUpTo(Rutas.CREAR_REPARTO) { inclusive = true }
                        }
                    }
                )
            }
        }

        composable(
            route = "${Rutas.REPARTO_DETALLE}/{idReparto}",
            arguments = listOf(navArgument("idReparto") { type = NavType.IntType })
        ) { backStackEntry ->
            val idReparto = backStackEntry.arguments?.getInt("idReparto") ?: 0
            PantallaConEspacioSuperior {
                RepartoDetalleScreen(
                    idReparto = idReparto,
                    viewModel = repartoViewModel,
                    onVolver = { navController.popBackStack() }
                )
            }
        }

        composable(Rutas.CATALOGO) {
            val catalogoViewModel = remember { CatalogoViewModel() }
            MainScreen(navController = navController) {
                CatalogoScreen(
                    viewModel = catalogoViewModel,
                    onVolver = { navController.popBackStack() },
                    onProductoClick = { producto ->
                        navController.navigate("${Rutas.DETALLE_PRODUCTO}/${producto.id}")
                    }
                )
            }
        }

        composable(
            route = "${Rutas.DETALLE_PRODUCTO}/{productoId}",
            arguments = listOf(navArgument("productoId") { type = NavType.StringType })
        ) { backStackEntry ->
            val productoId = backStackEntry.arguments?.getString("productoId") ?: ""
            val catalogoViewModel = remember { CatalogoViewModel() }
            // Obtenemos el producto por ID desde el ViewModel
            val producto = remember(productoId) {
                catalogoViewModel.obtenerProductoPorId(productoId)
            }

            if (producto != null) {
                PantallaConEspacioSuperior {
                    DetalleProductoScreen(
                        producto = producto,
                        onVolver = { navController.popBackStack() },
                        onAgregarAlPedido = { prod, vol ->
                            // Aquí conectaremos con el PedidoViewModel más adelante
                            println("Agregar al pedido: ${prod.nombre} - $vol")
                        }
                    )
                }
            } else {
                // Pantalla de carga o error si no encuentra el producto
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }

        composable(
            route = "${Rutas.SEGUIMIENTO_PEDIDO}/{pedidoId}",
            arguments = listOf(navArgument("pedidoId") { type = NavType.StringType })
        ) { backStackEntry ->
            val pedidoId = backStackEntry.arguments?.getString("pedidoId") ?: ""
            MainScreen(navController = navController) {
                SeguimientoPedidoScreen(
                    pedidoId = pedidoId,
                    reparto = null,
                    onVolver = { navController.popBackStack() },
                    onReportarProblema = { /* Abrir formulario de reclamo */ }
                )
            }
        }

        composable(Rutas.DEVOLUCION) {
            val devolucionViewModel = remember { DevolucionViewModel() }
            PantallaConEspacioSuperior {
                DevolucionScreen(
                    viewModel = devolucionViewModel,
                    onVolver = { navController.popBackStack() }
                )
            }
        }

        composable(Rutas.CAPACITACIONES) {
            val capacitacionViewModel = remember { CapacitacionViewModel() }
            val esAdmin = AdminConfig.esAdmin(authViewModel.usuarioActual?.email)
            MainScreen(navController = navController) {
                CapacitacionListScreen(
                    viewModel = capacitacionViewModel,
                    esAdmin = esAdmin,
                    onVolver = { navController.popBackStack() },
                    onAbrirCapacitacion = { id -> navController.navigate("${Rutas.CAPACITACION_DETALLE}/$id") },
                    onNuevaCapacitacion = { navController.navigate(Rutas.NUEVA_CAPACITACION) }
                )
            }
        }

        composable(Rutas.NUEVA_CAPACITACION) {
            val nuevaCapacitacionViewModel = remember { NuevaCapacitacionViewModel() }
            PantallaConEspacioSuperior {
                NuevaCapacitacionScreen(
                    viewModel = nuevaCapacitacionViewModel,
                    onVolver = { navController.popBackStack() }
                )
            }
        }

        composable(
            route = "${Rutas.CAPACITACION_DETALLE}/{capacitacionId}",
            arguments = listOf(navArgument("capacitacionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val capacitacionId = backStackEntry.arguments?.getString("capacitacionId") ?: ""
            val uid = authViewModel.usuarioActual?.uid ?: ""
            val capacitacionDetalleViewModel = remember { CapacitacionDetalleViewModel() }
            PantallaConEspacioSuperior {
                CapacitacionDetalleScreen(
                    capacitacionId = capacitacionId,
                    uid = uid,
                    viewModel = capacitacionDetalleViewModel,
                    onVolver = { navController.popBackStack() }
                )
            }
        }

        composable(Rutas.ADMIN_CONSULTORAS) {
            val adminConsultorasViewModel = remember { AdminConsultorasViewModel() }
            PantallaConEspacioSuperior {
                AdminConsultorasScreen(
                    viewModel = adminConsultorasViewModel,
                    onVolver = { navController.popBackStack() }
                )
            }
        }
    }
}
