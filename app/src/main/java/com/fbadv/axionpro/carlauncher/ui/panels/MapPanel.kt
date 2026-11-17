package com.fbadv.axionpro.carlauncher.ui.panels

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
import com.fbadv.axionpro.carlauncher.AxionApp
import com.fbadv.axionpro.carlauncher.ui.GlassIconButton
import com.fbadv.axionpro.carlauncher.ui.theme.Glass
import com.fbadv.axionpro.carlauncher.ui.theme.glass
import androidx.compose.foundation.shape.RoundedCornerShape
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

/**
 * Mapa base do OpenStreetMap: gratuito e sem chave de API.
 * O modo escuro é feito com um filtro de cor (inverte e esfria o mapa claro).
 */
private val OSM: ITileSource = TileSourceFactory.MAPNIK

private fun darkFilter(): ColorMatrixColorFilter {
    val invert = ColorMatrix(floatArrayOf(
        -1f, 0f, 0f, 0f, 255f,
        0f, -1f, 0f, 0f, 255f,
        0f, 0f, -1f, 0f, 255f,
        0f, 0f, 0f, 1f, 0f,
    ))
    val desat = ColorMatrix().apply { setSaturation(0.25f) }
    val tone = ColorMatrix(floatArrayOf(
        0.80f, 0f, 0f, 0f, 2f,
        0f, 0.86f, 0f, 0f, 6f,
        0f, 0f, 0.98f, 0f, 14f,
        0f, 0f, 0f, 1f, 0f,
    ))
    invert.postConcat(desat)
    invert.postConcat(tone)
    return ColorMatrixColorFilter(invert)
}

private fun MapView.applyStyle(dark: Boolean) {
    overlayManager.tilesOverlay.setColorFilter(if (dark) darkFilter() else null)
    overlayManager.tilesOverlay.loadingBackgroundColor = if (dark) android.graphics.Color.rgb(14, 17, 22) else android.graphics.Color.rgb(230, 230, 230)
    overlayManager.tilesOverlay.loadingLineColor = android.graphics.Color.TRANSPARENT
    invalidate()
}

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
    val settings = AxionApp.instance.settings
    val holder = remember { arrayOfNulls<MyLocationNewOverlay>(1) }

    val map = remember {
        MapView(context).apply {
            setTileSource(OSM)
            applyStyle(dark)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            isTilesScaledToDpi = true
            minZoomLevel = 3.0
            maxZoomLevel = 19.0
            controller.setZoom(settings.value.mapZoom)
            // começa já na última posição conhecida (antes do primeiro fix do GPS)
            AxionApp.instance.weather.location.value?.let { controller.setCenter(GeoPoint(it.latitude, it.longitude)) }
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
            update = { it.applyStyle(dark) },
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
            "© OpenStreetMap",
            color = Glass.textFaint,
            fontSize = 9.sp,
            modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp),
        )
    }
}
