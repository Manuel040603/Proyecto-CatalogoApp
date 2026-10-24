package com.catalogoapp.consultoras

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.catalogoapp.consultoras.navigation.CatalogoAppNavHost
import com.catalogoapp.consultoras.ui.theme.CatalogoAppTheme
import com.catalogoapp.consultoras.util.GestorTema
import com.catalogoapp.consultoras.viewmodel.AuthViewModel

class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()

    private val permisoNotificaciones = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            permisoNotificaciones.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val themeManager = GestorTema(this)

        setContent {
            val currentTheme by themeManager.flujoTema.collectAsState(initial = "system")
            val view = LocalView.current
            if (!view.isInEditMode) {
                SideEffect {
                    val window = (view.context as Activity).window

                    val isDark = currentTheme == "dark" || currentTheme == "black"

                    window.statusBarColor = if (isDark) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                    WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark

                    window.navigationBarColor = if (isDark) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                    WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !isDark

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
