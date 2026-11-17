package com.fbadv.autoglass.ui.theme

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max
import kotlin.math.roundToInt

object Glass {
    val text = Color(0xFFF5F8FF)
    val textDim = Color(0xB8E3EAFA)
    val textFaint = Color(0x80D5DEF2)
    val accent = Color(0xFF4DA3FF)
    val accentSoft = Color(0x664DA3FF)
    val warm = Color(0xFFFFB86B)
    val danger = Color(0xFFFF6B6B)
    val fill = Color(0x1FFFFFFF)
    val fillStrong = Color(0x33FFFFFF)
    val stroke = Color(0x59FFFFFF)
    val darkVeil = Color(0x99070B16)
    val corner = 28.dp
    val shape = RoundedCornerShape(corner)
    val shapeSmall = RoundedCornerShape(18.dp)
}

/** Fundo desfocado compartilhado: cada painel de vidro desenha o trecho atrás dele. */
class Backdrop(val image: ImageBitmap, val rootSize: IntSize)

val LocalBackdrop = staticCompositionLocalOf<Backdrop?> { null }

/**
 * Efeito "liquid glass": recorta o fundo desfocado na posição do painel,
 * aplica um véu translúcido, brilho superior e borda em gradiente.
 * Funciona em qualquer versão do Android (não depende de RenderEffect).
 */
fun Modifier.glass(
    shape: Shape = Glass.shape,
    strength: Float = 1f,
    border: Boolean = true,
): Modifier = composed {
    val backdrop = LocalBackdrop.current
    var pos by remember { mutableStateOf(Offset.Zero) }
    val base = this
        .onGloballyPositioned { pos = it.positionInRoot() }
        .clip(shape)
        .drawBehind {
            if (backdrop != null) {
                val img = backdrop.image
                val rw = backdrop.rootSize.width.toFloat()
                val rh = backdrop.rootSize.height.toFloat()
                val scale = max(rw / img.width, rh / img.height)
                val dw = img.width * scale
                val dh = img.height * scale
                drawImage(
                    image = img,
                    srcOffset = IntOffset.Zero,
                    srcSize = IntSize(img.width, img.height),
                    dstOffset = IntOffset(((rw - dw) / 2f - pos.x).roundToInt(), ((rh - dh) / 2f - pos.y).roundToInt()),
                    dstSize = IntSize(dw.roundToInt(), dh.roundToInt()),
                    filterQuality = FilterQuality.Low,
                )
            } else {
                drawRect(Color(0xFF101828))
            }
            drawRect(
                Brush.verticalGradient(
                    0f to Color.White.copy(alpha = 0.20f * strength),
                    0.35f to Color.White.copy(alpha = 0.09f * strength),
                    1f to Color.White.copy(alpha = 0.05f * strength),
                ),
            )
            // reflexo diagonal sutil
            drawRect(
                Brush.linearGradient(
                    0f to Color.White.copy(alpha = 0.10f * strength),
                    0.25f to Color.Transparent,
                    start = Offset.Zero,
                    end = Offset(size.width * 0.6f, size.height),
                ),
            )
        }
    if (!border) base else base.border(
        width = 1.dp,
        brush = Brush.linearGradient(
            0f to Color.White.copy(alpha = 0.55f),
            0.45f to Color.White.copy(alpha = 0.08f),
            1f to Color.White.copy(alpha = 0.28f),
        ),
        shape = shape,
    )
}

private val scheme: ColorScheme = darkColorScheme(
    primary = Glass.accent,
    onPrimary = Color.White,
    secondary = Glass.warm,
    background = Color(0xFF070B16),
    surface = Color(0xFF111A2C),
    onSurface = Glass.text,
    onBackground = Glass.text,
    surfaceVariant = Color(0xFF1B2638),
    onSurfaceVariant = Glass.textDim,
)

private val typography = Typography(
    displayLarge = TextStyle(fontSize = 84.sp, fontWeight = FontWeight.Light, letterSpacing = (-2).sp),
    headlineMedium = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = 16.sp),
    bodyMedium = TextStyle(fontSize = 14.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
)

@Composable
fun AutoGlassTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, typography = typography, content = content)
}
