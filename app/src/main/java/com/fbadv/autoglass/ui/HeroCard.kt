package com.fbadv.autoglass.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Grain
import androidx.compose.material.icons.rounded.LocationCity
import androidx.compose.material.icons.rounded.NearMe
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Terrain
import androidx.compose.material.icons.rounded.Thunderstorm
import androidx.compose.material.icons.rounded.Umbrella
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbCloudy
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.AddRoad
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fbadv.autoglass.data.SceneMode
import com.fbadv.autoglass.weather.Weather
import com.fbadv.autoglass.ui.theme.Glass
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

fun sceneIcon(mode: SceneMode): ImageVector = when (mode) {
    SceneMode.RUA -> Icons.Rounded.LocationCity
    SceneMode.DUNAS -> Icons.Rounded.Terrain
    SceneMode.ESTRADA -> Icons.Rounded.AddRoad
    SceneMode.LAMA -> Icons.Rounded.WaterDrop
}

fun weatherIcon(w: Weather?): ImageVector = when (w?.code) {
    null -> Icons.Rounded.WbCloudy
    0, 1 -> if (w.isDay) Icons.Rounded.WbSunny else Icons.Rounded.NightsStay
    2 -> Icons.Rounded.WbCloudy
    3, 45, 48 -> Icons.Rounded.Cloud
    51, 53, 55, 56, 57 -> Icons.Rounded.Grain
    61, 63, 65, 66, 67, 80, 81, 82 -> Icons.Rounded.Umbrella
    71, 73, 75, 77, 85, 86 -> Icons.Rounded.AcUnit
    95, 96, 99 -> Icons.Rounded.Thunderstorm
    else -> Icons.Rounded.WbCloudy
}

/**
 * Cartão principal: o veículo sobre o ambiente escolhido (rua, dunas,
 * estrada, lama), com relógio, data e previsão do tempo.
 */
@Composable
fun VehicleHero(
    scene: ImageBitmap?,
    sceneMode: SceneMode,
    vehicle: ImageBitmap?,
    vehicleName: String,
    vehicleTrim: String,
    weather: Weather?,
    clock24h: Boolean,
    onSceneChange: (SceneMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier
            .clip(Glass.shape)
            .border(1.dp, Brush.linearGradient(listOf(Color.White.copy(0.5f), Color.White.copy(0.06f), Color.White.copy(0.25f))), Glass.shape),
    ) {
        val compact = maxHeight < 340.dp || maxWidth < 480.dp
        val narrow = maxWidth < 560.dp
        val wide = maxWidth / maxHeight >= 1.9f
        // o carro fica à direita e acima do seletor de ambiente, sem cobrir o relógio
        val carAspect = vehicle?.let { it.width.toFloat() / it.height } ?: 1.75f
        val chipsSpace = if (compact) 58.dp else 72.dp
        val carW = minOf(maxWidth * (if (wide) 0.62f else 0.58f), (maxHeight - chipsSpace) * 0.70f * carAspect)
        val textW = if (wide) maxWidth * 0.40f else maxWidth * 0.46f
        Crossfade(scene, animationSpec = tween(600), label = "scene") { img ->
            if (img != null) Image(img, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else Box(Modifier.fillMaxSize())
        }
        // véus para leitura do texto
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(0f to Color(0xCC050914), 0.45f to Color(0x55050914), 1f to Color.Transparent),
            ),
        )
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.6f to Color.Transparent, 1f to Color(0x99050914))))

        // veículo
        AnimatedContent(
            targetState = sceneMode to vehicle,
            transitionSpec = {
                (slideInHorizontally(tween(700)) { it / 3 } + fadeIn(tween(500))) togetherWith fadeOut(tween(250))
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = maxWidth * 0.02f, bottom = chipsSpace - 8.dp)
                .width(carW),
            label = "vehicle",
        ) { (_, v) ->
            if (v != null) {
                Box(Modifier.fillMaxWidth().aspectRatio(v.width.toFloat() / v.height)) {
                    Canvas(Modifier.fillMaxSize()) {
                        // sombra de contato sob o carro
                        val h = size.height * 0.16f
                        drawOval(
                            brush = Brush.radialGradient(
                                listOf(Color.Black.copy(alpha = 0.75f), Color.Transparent),
                                center = Offset(size.width * 0.5f, size.height * 0.93f),
                                radius = size.width * 0.5f,
                            ),
                            topLeft = Offset(size.width * 0.02f, size.height * 0.93f - h / 2),
                            size = Size(size.width * 0.96f, h),
                        )
                    }
                    Image(v, vehicleName, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                }
            }
        }

        // relógio + tempo
        val now = rememberNow()
        Column(Modifier.align(Alignment.TopStart).padding(if (compact) 16.dp else 24.dp).width(textW)) {
            val time = SimpleDateFormat(if (clock24h) "HH:mm" else "h:mm", Locale("pt", "BR")).format(Date(now))
            Text(
                time,
                style = MaterialTheme.typography.displayLarge.copy(fontSize = if (compact) 56.sp else 84.sp),
                color = Glass.text,
            )
            Text(
                SimpleDateFormat(if (compact) "EEE, d 'de' MMM" else "EEEE, d 'de' MMMM", Locale("pt", "BR")).format(Date(now))
                    .replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleMedium,
                color = Glass.textDim,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(if (compact) 8.dp else 16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(weatherIcon(weather), null, tint = Glass.warm, modifier = Modifier.size(if (compact) 30.dp else 40.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    weather?.let { "${it.temperature.roundToInt()}°" } ?: "--°",
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = if (compact) 28.sp else 36.sp, fontWeight = FontWeight.Light),
                    color = Glass.text,
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            weather?.city ?: "Buscando localização…",
                            style = MaterialTheme.typography.labelLarge,
                            color = Glass.text,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Icon(Icons.Rounded.NearMe, null, tint = Glass.textDim, modifier = Modifier.padding(start = 4.dp).size(14.dp))
                    }
                    Text(
                        weather?.let { "${it.description} · Máx ${it.max.roundToInt()}° Mín ${it.min.roundToInt()}°" } ?: "Previsão indisponível",
                        style = MaterialTheme.typography.labelMedium,
                        color = Glass.textDim,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        // nome do veículo
        Column(
            Modifier.align(Alignment.TopEnd).padding(if (compact) 16.dp else 24.dp),
            horizontalAlignment = Alignment.End,
        ) {
            Text(
                vehicleName.uppercase(),
                color = Glass.text,
                fontSize = if (compact) 22.sp else 30.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(26.dp).height(3.dp).background(Brush.horizontalGradient(listOf(Glass.accent, Glass.warm))))
                Spacer(Modifier.width(8.dp))
                Text(vehicleTrim.uppercase(), color = Glass.textDim, fontSize = 13.sp, letterSpacing = 3.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        // seletor de ambiente
        Row(
            Modifier.align(Alignment.BottomStart).padding(if (compact) 12.dp else 18.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SceneMode.entries.forEach { m ->
                GlassChip(
                    text = if (narrow && m != sceneMode) "" else m.label,
                    icon = sceneIcon(m),
                    selected = m == sceneMode,
                    onClick = { onSceneChange(m) },
                )
            }
        }
    }
}
