package com.fbadv.autoglass.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.AddRoad
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Grain
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.LocationCity
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Terrain
import androidx.compose.material.icons.outlined.Thunderstorm
import androidx.compose.material.icons.outlined.Umbrella
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.WbCloudy
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.Garage
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fbadv.autoglass.data.SceneMode
import com.fbadv.autoglass.ui.theme.Glass
import com.fbadv.autoglass.weather.Weather
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun sceneIcon(mode: SceneMode): ImageVector = when (mode) {
    SceneMode.ESTUDIO -> Icons.Outlined.Garage
    SceneMode.RUA -> Icons.Outlined.LocationCity
    SceneMode.DUNAS -> Icons.Outlined.Terrain
    SceneMode.ESTRADA -> Icons.Outlined.AddRoad
    SceneMode.LAMA -> Icons.Outlined.WaterDrop
}

fun weatherIcon(w: Weather?): ImageVector = when (w?.code) {
    null -> Icons.Outlined.WbCloudy
    0, 1 -> if (w.isDay) Icons.Outlined.WbSunny else Icons.Outlined.NightsStay
    2 -> Icons.Outlined.WbCloudy
    3, 45, 48 -> Icons.Outlined.Cloud
    51, 53, 55, 56, 57 -> Icons.Outlined.Grain
    61, 63, 65, 66, 67, 80, 81, 82 -> Icons.Outlined.Umbrella
    71, 73, 75, 77, 85, 86 -> Icons.Outlined.AcUnit
    95, 96, 99 -> Icons.Outlined.Thunderstorm
    else -> Icons.Outlined.LightMode
}

/**
 * Palco do veículo (estilo centrais premium): relógio fino, nome do modelo,
 * o carro sobre o piso do ambiente com reflexo, sombra de contato e luz de
 * destaque, e o seletor de ambiente.
 */
@Composable
fun VehicleStage(
    vehicle: ImageBitmap?,
    sceneMode: SceneMode,
    vehicleName: String,
    vehicleTrim: String,
    clock24h: Boolean,
    night: Boolean,
    onSceneChange: (SceneMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier) {
        val compact = maxHeight < 380.dp
        val carAspect = vehicle?.let { it.width.toFloat() / it.height } ?: 1.75f
        val selectorSpace = if (compact) 44.dp else 56.dp
        val wideStage = maxWidth / maxHeight > 1.8f
        val carW = minOf(maxWidth * (if (wideStage) 0.62f else 0.9f), (maxHeight - selectorSpace) * (if (wideStage) 0.80f else 0.68f) * carAspect)
        val carShiftX = if (wideStage) maxWidth * 0.10f else 0.dp
        val carH = carW / carAspect

        // luz de destaque atrás do carro
        Canvas(Modifier.fillMaxSize()) {
            val cx = size.width * 0.5f + carShiftX.toPx()
            val floorY = size.height - selectorSpace.toPx()
            val r = carW.toPx() * 0.75f
            val c = Offset(cx, floorY - carH.toPx() * 0.40f)
            drawCircle(
                Brush.radialGradient(
                    0f to Glass.accent.copy(alpha = if (night) 0.12f else 0.05f), 0.55f to Glass.accent.copy(alpha = if (night) 0.035f else 0.012f), 1f to Color.Transparent,
                    center = c, radius = r,
                ),
                radius = r, center = c,
            )
            // linha de luz no piso
            drawRect(
                Brush.horizontalGradient(
                    0f to Color.Transparent, 0.5f to Color.White.copy(alpha = 0.10f), 1f to Color.Transparent,
                ),
                topLeft = Offset(cx - carW.toPx() * 0.7f, floorY),
                size = Size(carW.toPx() * 1.4f, 1.5f),
            )
        }

        // carro + reflexo
        AnimatedContent(
            targetState = vehicle,
            transitionSpec = { (slideInHorizontally(tween(800)) { it / 4 } + fadeIn(tween(600))) togetherWith fadeOut(tween(200)) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(x = carShiftX, y = -selectorSpace + carH * 0.02f)
                .width(carW)
                .height(carH),
            label = "vehicle",
        ) { v ->
            if (v != null) Box(Modifier.fillMaxSize()) {
                // sombra de contato seguindo a perspectiva da foto (¾ de frente):
                // faixa diagonal sob a carroceria + sombra concentrada em cada pneu
                Canvas(Modifier.fillMaxWidth().height(carH).align(Alignment.TopCenter)) {
                    val w = size.width; val h = size.height
                    val a0 = Offset(w * 0.10f, h * 0.885f); val a1 = Offset(w * 0.93f, h * 0.715f)
                    val steps = 14
                    for (i in 0..steps) {
                        val t = i / steps.toFloat()
                        val c = Offset(a0.x + (a1.x - a0.x) * t, a0.y + (a1.y - a0.y) * t)
                        val r = w * (0.13f - 0.05f * t)
                        drawOval(
                            Brush.radialGradient(listOf(Color.Black.copy(alpha = if (night) 0.30f else 0.22f), Color.Transparent), center = c, radius = r),
                            topLeft = Offset(c.x - r, c.y - r * 0.35f), size = Size(r * 2, r * 0.7f),
                        )
                    }
                    for ((c, r) in listOf(Offset(w * 0.52f, h * 0.965f) to w * 0.085f, Offset(w * 0.915f, h * 0.705f) to w * 0.055f)) {
                        drawOval(
                            Brush.radialGradient(listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent), center = c, radius = r),
                            topLeft = Offset(c.x - r, c.y - r * 0.25f), size = Size(r * 2, r * 0.5f),
                        )
                    }
                }
                Image(
                    v, vehicleName,
                    Modifier.fillMaxWidth().height(carH).align(Alignment.TopCenter),
                    contentScale = ContentScale.Fit,
                    // o carro recebe a luz do ambiente: mais frio/escuro à noite
                    colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply {
                        if (night) setToScale(0.80f, 0.84f, 0.93f, 1f) else setToScale(1f, 1f, 1f, 1f)
                    }),
                )
            }
        }

        // relógio
        val now = rememberNow()
        Column(Modifier.align(Alignment.TopStart).padding(start = 8.dp, top = 4.dp)) {
            Text(
                SimpleDateFormat(if (clock24h) "HH:mm" else "h:mm", Locale("pt", "BR")).format(Date(now)),
                style = MaterialTheme.typography.displayLarge.copy(fontSize = if (compact) 64.sp else 92.sp, shadow = Glass.textShadow),
                color = Glass.text,
            )
            Text(
                SimpleDateFormat("EEEE · d 'de' MMMM", Locale("pt", "BR")).format(Date(now)).uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(shadow = Glass.textShadow),
                color = Glass.text.copy(alpha = 0.8f),
            )
            if (wideStage) {
                Spacer(Modifier.height(18.dp))
                VehicleName(vehicleName, vehicleTrim, compact, Alignment.Start)
            }
        }

        if (!wideStage) Box(Modifier.align(Alignment.TopEnd).padding(end = 8.dp, top = 14.dp)) {
            VehicleName(vehicleName, vehicleTrim, compact, Alignment.End)
        }

        // seletor de ambiente
        TextTabs(
            items = SceneMode.entries.map { it.label },
            selected = SceneMode.entries.indexOf(sceneMode),
            onSelect = { onSceneChange(SceneMode.entries[it]) },
            modifier = Modifier.align(Alignment.BottomCenter).offset(x = carShiftX).padding(bottom = 4.dp),
        )
    }
}

@Composable
private fun VehicleName(vehicleName: String, vehicleTrim: String, compact: Boolean, align: Alignment.Horizontal) {
        Column(horizontalAlignment = align) {
            Text(
                vehicleName.uppercase(),
                color = Glass.text,
                fontSize = if (compact) 18.sp else 24.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 9.sp,
                style = androidx.compose.ui.text.TextStyle(shadow = Glass.textShadow),
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(18.dp).height(1.dp).background(Glass.accent))
                Spacer(Modifier.width(8.dp))
                Text(vehicleTrim.uppercase(), color = Glass.text.copy(alpha = 0.75f), fontSize = 11.sp, letterSpacing = 4.sp, style = androidx.compose.ui.text.TextStyle(shadow = Glass.textShadow))
            }
        }
}
