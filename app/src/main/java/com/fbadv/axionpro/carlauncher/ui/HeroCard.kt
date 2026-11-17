package com.fbadv.axionpro.carlauncher.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.outlined.ScreenRotationAlt
import androidx.compose.material3.Icon
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.compose.animation.core.tween
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
import com.fbadv.axionpro.carlauncher.data.SceneMode
import com.fbadv.axionpro.carlauncher.ui.theme.Glass
import com.fbadv.axionpro.carlauncher.weather.Weather
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
 * Palco do veículo (estilo centrais premium), posicionado à direita da tela:
 * relógio fino e nome do modelo, o carro girável com o dedo sobre o piso do
 * ambiente e o seletor de ambiente.
 */
@Composable
fun VehicleStage(
    vehicle: ImageBitmap?,
    frames360: FrameSource?,
    topView: ImageBitmap?,
    sceneMode: SceneMode,
    vehicleName: String,
    vehicleTrim: String,
    clock24h: Boolean,
    night: Boolean,
    onSceneChange: (SceneMode) -> Unit,
    modifier: Modifier = Modifier,
    initialAngle: Float = 0f,
) {
    BoxWithConstraints(modifier) {
        val compact = maxHeight < 380.dp
        val carAspect = if (frames360 != null) frames360.aspect else vehicle?.let { it.width.toFloat() / it.height } ?: 1.75f
        val selectorSpace = if (compact) 44.dp else 56.dp
        val wideStage = maxWidth / maxHeight > 1.8f
        val carW = minOf(maxWidth * (if (wideStage) 0.70f else 0.94f), (maxHeight - selectorSpace) * (if (wideStage) 0.86f else 0.74f) * carAspect)
        val carH = carW / carAspect
        // palco à direita: o texto fica no canto direito e o carro um pouco à esquerda dele
        val carShiftX = if (wideStage) -maxWidth * 0.10f else 0.dp

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
            drawRect(
                Brush.horizontalGradient(
                    0f to Color.Transparent, 0.5f to Color.White.copy(alpha = 0.10f), 1f to Color.Transparent,
                ),
                topLeft = Offset(cx - carW.toPx() * 0.7f, floorY),
                size = Size(carW.toPx() * 1.4f, 1.5f),
            )
        }

        var showTop by remember { mutableStateOf(false) }
        if (showTop && topView != null) {
            TopViewSpinner(
                image = topView,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(x = carShiftX, y = -selectorSpace - 8.dp)
                    .width(carW * 0.92f)
                    .height((maxHeight - selectorSpace) * 0.78f),
            )
        } else if (vehicle != null || frames360 != null) {
            VehicleSpinner(
                base = vehicle,
                frames = frames360,
                night = night,
                vehicleName = vehicleName,
                initialAngle = initialAngle,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(x = carShiftX, y = -selectorSpace + carH * 0.02f)
                    .width(carW)
                    .height(carH),
            )
        }

        // relógio + nome (canto superior direito)
        val now = rememberNow()
        Column(Modifier.align(Alignment.TopEnd).padding(end = 8.dp, top = 4.dp), horizontalAlignment = Alignment.End) {
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
                VehicleName(vehicleName, vehicleTrim, compact, Alignment.End)
            }
        }
        if (!wideStage) Box(Modifier.align(Alignment.TopStart).padding(start = 8.dp, top = 14.dp)) {
            VehicleName(vehicleName, vehicleTrim, compact, Alignment.Start)
        }

        // seletor de ambiente + vista (360° / de cima)
        Row(
            Modifier.align(Alignment.BottomCenter).offset(x = carShiftX).padding(bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextTabs(
                items = SceneMode.entries.map { it.label },
                selected = SceneMode.entries.indexOf(sceneMode),
                onSelect = { onSceneChange(SceneMode.entries[it]) },
            )
            if (topView != null) {
                Spacer(Modifier.width(18.dp))
                Box(Modifier.width(1.dp).height(16.dp).background(Glass.text.copy(alpha = 0.25f)))
                Spacer(Modifier.width(18.dp))
                TextTabs(items = listOf("360°", "Topo"), selected = if (showTop) 1 else 0, onSelect = { showTop = it == 1 })
            }
        }
    }
}

/** Vista de cima: arraste para girar o carro em torno de si mesmo. */
@Composable
private fun TopViewSpinner(image: ImageBitmap, modifier: Modifier) {
    val angle = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var lastInteraction by remember { mutableStateOf(0L) }
    LaunchedEffect(lastInteraction) {
        if (lastInteraction == 0L) return@LaunchedEffect
        delay(6000)
        angle.animateTo((angle.value / 360f).roundToInt() * 360f, tween(1400, easing = FastOutSlowInEasing))
    }
    val density = LocalDensity.current
    val degPerPx = with(density) { 0.45f / 1.dp.toPx() }
    Box(
        modifier.pointerInput(Unit) {
            val tracker = VelocityTracker()
            detectHorizontalDragGestures(
                onDragStart = { tracker.resetTracking(); scope.launch { angle.stop() } },
                onDragEnd = {
                    val v = tracker.calculateVelocity().x * degPerPx
                    scope.launch { angle.animateDecay(v, exponentialDecay(frictionMultiplier = 1.6f)); lastInteraction = System.currentTimeMillis() }
                },
            ) { change, dx ->
                tracker.addPosition(change.uptimeMillis, change.position)
                scope.launch { angle.snapTo(angle.value + dx * degPerPx) }
            }
        },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawOval(
                Brush.radialGradient(listOf(Color.Black.copy(alpha = 0.45f), Color.Transparent), center = center, radius = size.minDimension * 0.62f),
                topLeft = Offset(size.width * 0.08f, size.height * 0.18f), size = Size(size.width * 0.84f, size.height * 0.64f),
            )
        }
        Image(
            image, "Vista de cima",
            Modifier.fillMaxSize().graphicsLayer { rotationZ = angle.value },
            contentScale = ContentScale.Fit,
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

/**
 * Carro girável com o dedo (arrastar para os lados, com inércia).
 *  - Com uma sequência de fotos ao redor do carro (8+ quadros): giro 360° real.
 *  - Só com a foto padrão: giro 3D em perspectiva entre os dois lados
 *    (frente-esquerda ↔ frente-direita) com brilho percorrendo a lataria.
 * Depois de alguns segundos parado, volta suavemente à posição original.
 */
@Composable
private fun VehicleSpinner(
    base: ImageBitmap?,
    frames: FrameSource?,
    night: Boolean,
    vehicleName: String,
    initialAngle: Float,
    modifier: Modifier,
) {
    val angle = remember { Animatable(initialAngle) }
    val scope = rememberCoroutineScope()
    var touched by remember { mutableStateOf(false) }
    var lastInteraction by remember { mutableStateOf(0L) }
    val density = LocalDensity.current
    val degPerPx = with(density) { 0.45f / 1.dp.toPx() }

    // volta à posição de exibição depois de 6 s sem toque
    LaunchedEffect(lastInteraction) {
        if (lastInteraction == 0L) return@LaunchedEffect
        delay(6000)
        val target = (angle.value / 360f).roundToInt() * 360f
        angle.animateTo(target, tween(1400, easing = FastOutSlowInEasing))
    }

    val tint = if (frames != null) null else ColorFilter.colorMatrix(ColorMatrix().apply {
        if (night) setToScale(0.80f, 0.84f, 0.93f, 1f) else setToScale(1f, 1f, 1f, 1f)
    })

    Box(
        modifier.pointerInput(Unit) {
            val tracker = VelocityTracker()
            detectHorizontalDragGestures(
                onDragStart = { tracker.resetTracking(); touched = true; scope.launch { angle.stop() } },
                onDragEnd = {
                    val v = tracker.calculateVelocity().x * degPerPx
                    scope.launch {
                        angle.animateDecay(v, exponentialDecay(frictionMultiplier = 1.6f))
                        lastInteraction = System.currentTimeMillis()
                    }
                },
            ) { change, dx ->
                tracker.addPosition(change.uptimeMillis, change.position)
                scope.launch { angle.snapTo(angle.value + dx * degPerPx) }
            }
        },
    ) {
        val a = angle.value
        if (frames != null && frames.count > 0) {
            val n = frames.count
            val idx = Math.floorMod((a / (360f / n)).roundToInt(), n)
            var shown by remember(frames) { mutableStateOf(frames.cached(0)) }
            LaunchedEffect(frames, idx) {
                frames.frame(idx)?.let { shown = it }
                // pré-carrega os vizinhos para o giro ficar fluido
                for (d in 1..3) { frames.frame(idx + d); frames.frame(idx - d) }
            }
            shown?.let { Image(it, vehicleName, Modifier.fillMaxSize(), contentScale = ContentScale.Fit) }
        } else if (base != null) {
            // posição no círculo: lado A (foto) em 0°, lado B (espelho) em 180°
            val norm = (((a + 180f) % 360f) + 360f) % 360f - 180f          // -180..180
            val sideB = abs(norm) > 90f
            val local = if (sideB) (if (norm > 0) norm - 180f else norm + 180f) else norm   // -90..90
            val turn = abs(local) / 90f                                                    // 0 de frente, 1 de perfil
            val squeeze = 1f - 0.38f * turn * turn
            val fade = (1f - ((turn - 0.78f) / 0.22f).coerceIn(0f, 1f))
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        cameraDistance = 14f * density.density
                        rotationY = local * 0.38f
                        scaleX = squeeze * (if (sideB) -1f else 1f)
                        alpha = 0.9f + 0.1f * fade
                    },
            ) {
                ContactShadow(night)
                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                        .drawWithContent {
                            drawContent()
                            // brilho percorrendo a lataria conforme o giro
                            val x = size.width * (0.5f + local / 140f)
                            drawRect(
                                Brush.linearGradient(
                                    0f to Color.Transparent, 0.5f to Color.White.copy(alpha = 0.10f + 0.12f * turn), 1f to Color.Transparent,
                                    start = Offset(x - size.width * 0.25f, 0f), end = Offset(x + size.width * 0.25f, size.height * 0.3f),
                                ),
                                blendMode = BlendMode.SrcAtop,
                            )
                        },
                ) {
                    Image(base, vehicleName, Modifier.fillMaxSize(), contentScale = ContentScale.Fit, colorFilter = tint)
                }
            }
        }
        // dica de interação (some depois do primeiro toque)
        if (!touched) {
            Row(
                Modifier.align(Alignment.TopCenter).offset(y = (-6).dp)
                    .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.ScreenRotationAlt, null, tint = Glass.text, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text("ARRASTE PARA GIRAR", color = Glass.text.copy(alpha = 0.85f), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

/** Sombra de contato seguindo a perspectiva da foto (¾ de frente). */
@Composable
private fun ContactShadow(night: Boolean) {
    Canvas(Modifier.fillMaxSize()) {
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
}
