package com.fbadv.autoglass.ui.theme

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
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

/**
 * Paleta premium: grafite quase preto, textos em branco/cinza frio e uma
 * única cor de destaque escolhida pelo usuário.
 */
object Glass {
    val text = Color(0xFFF3F5F8)
    val textDim = Color(0xFF9BA3AE)
    val textFaint = Color(0xFF5D6570)
    val hairline = Color(0x14FFFFFF)
    val surface = Color(0xFF0B0D11)
    val warm = Color(0xFFE8C48F)
    val danger = Color(0xFFFF6B6B)
    val stroke = Color(0x1FFFFFFF)

    private val accentState = mutableStateOf(Color(0xFF8ED1FF))
    /** Cor de destaque atual (lida como estado: a interface recompõe ao trocar). */
    var accent: Color
        get() = accentState.value
        set(v) { accentState.value = v }
    val accentSoft: Color get() = accent.copy(alpha = 0.16f)

    val corner = 22.dp
    val shape = RoundedCornerShape(corner)
    val shapeSmall = RoundedCornerShape(14.dp)
}

/** Fundo desfocado compartilhado: cada painel desenha o trecho atrás dele. */
class Backdrop(val image: ImageBitmap, val rootSize: IntSize)

val LocalBackdrop = staticCompositionLocalOf<Backdrop?> { null }

/**
 * Painel "vidro fumê": recorta o fundo desfocado na posição do painel,
 * escurece bastante (visual premium), aplica brilho fino no topo e
 * borda hairline.
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
            }
            // fumê escuro
            drawRect(Color(0xFF07090C).copy(alpha = (0.62f / strength.coerceAtLeast(0.6f)).coerceIn(0.35f, 0.8f)))
            drawRect(
                Brush.verticalGradient(
                    0f to Color.White.copy(alpha = 0.055f * strength),
                    0.5f to Color.White.copy(alpha = 0.015f * strength),
                    1f to Color.Transparent,
                ),
            )
        }
    if (!border) base else base.border(
        width = 1.dp,
        brush = Brush.verticalGradient(
            0f to Color.White.copy(alpha = 0.16f),
            0.4f to Color.White.copy(alpha = 0.06f),
            1f to Color.White.copy(alpha = 0.03f),
        ),
        shape = shape,
    )
}

private val typography = Typography(
    displayLarge = TextStyle(fontSize = 96.sp, fontWeight = FontWeight.Thin, letterSpacing = (-1).sp),
    headlineMedium = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Light),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Normal),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal),
    bodyLarge = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Light),
    bodyMedium = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Light),
    labelLarge = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.4.sp),
    labelMedium = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.4.sp),
)

@Composable
fun AutoGlassTheme(content: @Composable () -> Unit) {
    val scheme = darkColorScheme(
        primary = Glass.accent,
        onPrimary = Color.Black,
        secondary = Glass.warm,
        background = Glass.surface,
        surface = Color(0xFF13161B),
        onSurface = Glass.text,
        onBackground = Glass.text,
        surfaceVariant = Color(0xFF1A1E24),
        onSurfaceVariant = Glass.textDim,
    )
    MaterialTheme(colorScheme = scheme, typography = typography, content = content)
}
