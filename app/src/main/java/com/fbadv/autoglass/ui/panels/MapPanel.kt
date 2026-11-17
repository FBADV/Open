package com.fbadv.autoglass.ui.panels

import android.view.MotionEvent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.fbadv.autoglass.AutoGlassApp
import com.fbadv.autoglass.ui.GlassIconButton
import com.fbadv.autoglass.ui.theme.Glass
import com.fbadv.autoglass.ui.theme.glass
import androidx.compose.foundation.shape.RoundedCornerShape
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

private val DARK: ITileSource = XYTileSource(
    "CartoDark", 0, 19, 256, ".png",
    arrayOf(
        "https://a.basemaps.cartocdn.com/dark_all/",
        "https://b.basemaps.cartocdn.com/dark_all/",
        "https://c.basemaps.cartocdn.com/dark_all/",
    ),
    "© OpenStreetMap © CARTO",
)

private val LIGHT: ITileSource = XYTileSource(
    "CartoVoyager", 0, 19, 256, ".png",
    arrayOf(
        "https://a.basemaps.cartocdn.com/rastertiles/voyager/",
        "https://b.basemaps.cartocdn.com/rastertiles/voyager/",
        "https://c.basemaps.cartocdn.com/rastertiles/voyager/",
    ),
    "© OpenStreetMap © CARTO",
)

/**
 * Mapa interativo dentro do painel (arrastar, pinça para zoom, seguir GPS).
 * Dois toques abrem o app de navegação escolhido (Waze / Google Maps) em tela cheia.
 */
@Composable
fun MapPanel(
    dark: Boolean,
    onDoubleTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val settings = AutoGlassApp.instance.settings
    val holder = remember { arrayOfNulls<MyLocationNewOverlay>(1) }

    val map = remember {
        MapView(context).apply {
            setTileSource(if (dark) DARK else LIGHT)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            isTilesScaledToDpi = true
            minZoomLevel = 3.0
            maxZoomLevel = 19.0
            controller.setZoom(settings.value.mapZoom)
            val loc = MyLocationNewOverlay(GpsMyLocationProvider(context), this).apply {
                runCatching { enableMyLocation() }
                enableFollowLocation()
                isDrawAccuracyEnabled = true
            }
            holder[0] = loc
            overlays.add(loc)
            overlays.add(object : Overlay() {
                override fun onDoubleTap(e: MotionEvent?, mapView: MapView?): Boolean {
                    onDoubleTap(); return true
                }
            })
        }
    }

    DisposableEffect(lifecycle) {
        val obs = LifecycleEventObserver { _, ev ->
            when (ev) {
                Lifecycle.Event.ON_RESUME -> { map.onResume(); runCatching { holder[0]?.enableMyLocation() } }
                Lifecycle.Event.ON_PAUSE -> { map.onPause(); holder[0]?.disableMyLocation() }
                else -> Unit
            }
        }
        lifecycle.addObserver(obs)
        map.onResume()
        onDispose {
            lifecycle.removeObserver(obs)
            settings.update { it.copy(mapZoom = map.zoomLevelDouble) }
            holder[0]?.disableMyLocation()
            map.onPause()
            map.onDetach()
        }
    }

    Box(modifier) {
        AndroidView(
            factory = { map },
            update = { it.setTileSource(if (dark) DARK else LIGHT) },
            modifier = Modifier.fillMaxSize(),
        )
        Column(
            Modifier.align(Alignment.CenterEnd).padding(10.dp).glass(RoundedCornerShape(24.dp)).padding(4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GlassIconButton(Icons.Outlined.Add, "Aproximar", size = 38.dp) { map.controller.zoomIn() }
            GlassIconButton(Icons.Outlined.Remove, "Afastar", size = 38.dp) { map.controller.zoomOut() }
            GlassIconButton(Icons.Outlined.MyLocation, "Centralizar", size = 38.dp) {
                holder[0]?.let { o ->
                    o.enableFollowLocation()
                    o.myLocation?.let { map.controller.animateTo(it) }
                }
            }
        }
        Text(
            "© OpenStreetMap © CARTO",
            color = Glass.textFaint,
            fontSize = 9.sp,
            modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp),
        )
    }
}
