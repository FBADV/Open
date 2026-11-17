package com.fbadv.autoglass.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import com.fbadv.autoglass.data.LightingMode
import com.fbadv.autoglass.weather.Weather
import kotlinx.coroutines.delay
import java.util.Calendar
import kotlin.random.Random

/** Chuva fina animada sobre o ambiente quando a previsão indica chuva. */
@Composable
fun RainOverlay(intensity: Float, modifier: Modifier = Modifier.fillMaxSize()) {
    val drops = remember {
        val r = Random(7)
        List(140) { floatArrayOf(r.nextFloat(), r.nextFloat(), 0.6f + r.nextFloat() * 0.8f, 0.25f + r.nextFloat() * 0.5f) }
    }
    val t by rememberInfiniteTransition(label = "rain").animateFloat(
        0f, 1f, infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart), label = "t",
    )
    Canvas(modifier) {
        val count = (drops.size * intensity.coerceIn(0.2f, 1f)).toInt()
        for (i in 0 until count) {
            val d = drops[i]
            val speed = d[2]
            val y = ((d[1] + t * speed) % 1f) * size.height
            val x = d[0] * size.width - y * 0.12f
            val len = 18f + 26f * speed
            drawLine(
                Color.White.copy(alpha = 0.10f * d[3] + 0.04f),
                Offset(x, y), Offset(x - len * 0.12f, y + len),
                strokeWidth = 1.2f, cap = StrokeCap.Round,
            )
        }
    }
}

fun isRaining(w: Weather?): Boolean = w?.code in setOf(51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82, 95, 96, 99)

fun rainIntensity(w: Weather?): Float = when (w?.code) {
    51, 53, 56, 61, 80 -> 0.35f
    55, 57, 63, 66, 81 -> 0.65f
    else -> 1f
}

/**
 * Claro ou escuro. Padrão: pelos faróis (farol aceso = escuro).
 * Dia ou noite para escolher a versão do ambiente: usa o "é dia" da
 * previsão (nascer/pôr do sol reais) e, sem internet, o relógio (6h–18h).
 */
@Composable
fun rememberIsDay(mode: LightingMode, weather: Weather?, headlightsOn: Boolean? = null): Boolean {
    val hour by produceState(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        while (true) { value = Calendar.getInstance().get(Calendar.HOUR_OF_DAY); delay(60_000) }
    }
    return when (mode) {
        LightingMode.DIA -> true
        LightingMode.NOITE -> false
        LightingMode.SOL -> weather?.isDay ?: (hour in 6..17)
        // farol aceso = modo escuro; apagado = claro; sem informação da central, usa o sol
        LightingMode.FAROL -> headlightsOn?.let { !it } ?: (weather?.isDay ?: (hour in 6..17))
    }
}
