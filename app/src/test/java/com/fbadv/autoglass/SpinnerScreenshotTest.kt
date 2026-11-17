package com.fbadv.autoglass

import android.graphics.BitmapFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalInspectionMode
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import com.android.resources.ScreenOrientation
import com.fbadv.autoglass.data.SceneMode
import com.fbadv.autoglass.ui.VehicleStage
import com.fbadv.autoglass.ui.theme.AutoGlassTheme
import org.junit.Rule
import org.junit.Test

/** Mostra o carro em quatro posições do giro (0°, 45°, 80°, 180°). */
class SpinnerScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.NEXUS_7.copy(screenWidth = 2400, screenHeight = 500, xdpi = 160, ydpi = 160, density = Density.MEDIUM, orientation = ScreenOrientation.LANDSCAPE, softButtons = false),
        showSystemUi = false,
    )

    @Test
    fun angles() {
        val car = BitmapFactory.decodeResource(paparazzi.context.resources, R.drawable.vehicle_compass_limited).asImageBitmap()
        paparazzi.snapshot {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                AutoGlassTheme {
                    Row(Modifier.fillMaxSize().background(Color(0xFF15181D))) {
                        listOf(0f, 45f, 80f, 180f).forEach { a ->
                            VehicleStage(car, emptyList(), SceneMode.ESTUDIO, "Compass", "Limited", true, false, {}, Modifier.weight(1f), initialAngle = a)
                        }
                    }
                }
            }
        }
    }
}
