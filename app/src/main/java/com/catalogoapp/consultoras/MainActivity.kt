package com.catalogoapp.consultoras

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.catalogoapp.consultoras.navigation.CatalogoAppNavHost
import com.catalogoapp.consultoras.ui.theme.CatalogoAppTheme
import com.catalogoapp.consultoras.util.ThemeManager
import com.catalogoapp.consultoras.viewmodel.AuthViewModel

class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        val themeManager = ThemeManager(this)

        setContent {
            val currentTheme by themeManager.themeFlow.collectAsState(initial = "system")
            val view = LocalView.current
            if (!view.isInEditMode) {
                SideEffect {
                    val window = (view.context as Activity).window

                    // Ajustamos el color de la barra de estado según el tema
                    val isDark = currentTheme == "dark" || currentTheme == "black"

                    // Barra de estado (superior)
                    window.statusBarColor = if (isDark) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                    WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark

                    // Barra de navegación del sistema (inferior) - Forzamos color sólido
                    window.navigationBarColor = if (isDark) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                    WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !isDark

                    // Desactivar el modo Edge-to-Edge para la barra de navegación si es necesario
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        window.isNavigationBarContrastEnforced = true
                    }
                }
            }

            CatalogoAppTheme(theme = currentTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CatalogoAppNavHost(authViewModel = authViewModel)
                }
            }
        }
    }
}