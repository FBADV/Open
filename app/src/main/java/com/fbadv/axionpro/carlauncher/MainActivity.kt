package com.fbadv.axionpro.carlauncher

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fbadv.axionpro.carlauncher.launch.AppLauncher
import com.fbadv.axionpro.carlauncher.overlay.HomeBubbleService
import com.fbadv.axionpro.carlauncher.ui.LauncherRoot
import com.fbadv.axionpro.carlauncher.ui.Screen
import com.fbadv.axionpro.carlauncher.ui.theme.AxionTheme

class MainActivity : ComponentActivity() {
    private val app get() = AxionApp.instance
    private val screen = mutableStateOf(Screen.HOME)

    private val locationRequest = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        app.weather.start()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            // Launcher: "voltar" nunca fecha a tela inicial, só volta ao painel.
            override fun handleOnBackPressed() { screen.value = Screen.HOME }
        })

        if (!app.weather.hasLocationPermission()) {
            locationRequest.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
        HomeBubbleService.refresh(this)
        askToBeDefaultHome()

        setContent {
            AxionTheme {
                val settings by app.settings.state.collectAsStateWithLifecycle()
                LaunchedEffect(settings.immersive) { applyImmersive(settings.immersive) }
                LauncherRoot(screen)
            }
        }
    }

    private val homeRole = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {}

    /**
     * Na primeira abertura, pede ao Android para o Axion Pro ser a tela inicial
     * padrão: assim ele aparece sozinho ao ligar a central e ao tocar em Home.
     */
    private fun askToBeDefaultHome() {
        val prefs = getSharedPreferences("axionpro_state", MODE_PRIVATE)
        if (AppLauncher.isDefaultHome(this) || prefs.getBoolean("askedHome", false)) return
        prefs.edit().putBoolean("askedHome", true).apply()
        runCatching { homeRole.launch(AppLauncher.defaultHomeRequest(this)) }
    }

    private fun applyImmersive(enabled: Boolean) {
        val c = WindowInsetsControllerCompat(window, window.decorView)
        if (enabled) {
            c.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            c.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            c.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Botão Home pressionado com o launcher já aberto: volta ao painel principal.
        screen.value = Screen.HOME
    }

    override fun onStart() {
        super.onStart()
        app.widgets.startListening()
    }

    override fun onResume() {
        super.onResume()
        app.media.attach()
        app.weather.start()
        app.apps.refresh()
        HomeBubbleService.instance?.setVisible(false)
        if (app.settings.value.immersive) applyImmersive(true)
    }

    override fun onPause() {
        super.onPause()
        HomeBubbleService.instance?.setVisible(true)
    }

    override fun onStop() {
        super.onStop()
        app.widgets.stopListening()
    }
}
