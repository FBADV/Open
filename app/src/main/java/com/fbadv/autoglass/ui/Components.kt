package com.fbadv.autoglass.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fbadv.autoglass.AutoGlassApp
import com.fbadv.autoglass.data.AppEntry
import com.fbadv.autoglass.ui.theme.Glass
import com.fbadv.autoglass.ui.theme.glass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** Painel de vidro genérico. */
@Composable
fun GlassBox(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = Glass.shape,
    content: @Composable BoxScope.() -> Unit,
) = Box(modifier.glass(shape), content = content)

/** Botão de ícone em traço fino; ativo ganha halo na cor de destaque. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    tint: Color = Glass.text,
    active: Boolean = false,
    filled: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .then(
                when {
                    filled -> Modifier.background(Glass.text, CircleShape)
                    active -> Modifier.background(Glass.accentSoft, CircleShape)
                    else -> Modifier.background(Color.White.copy(alpha = 0.04f), CircleShape)
                },
            )
            .combinedClickable(
                interactionSource = interaction,
                indication = androidx.compose.material3.ripple(bounded = true),
                onLongClick = onLongClick,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon, contentDescription,
            tint = when { filled -> Color(0xFF0B0D11); active -> Glass.accent; else -> tint },
            modifier = Modifier.size(size * 0.46f),
        )
    }
}

/** Ícone de um app instalado (carregado em segundo plano). */
@Composable
fun AppIcon(entry: AppEntry?, size: Dp, modifier: Modifier = Modifier) {
    val repo = AutoGlassApp.instanceOrNull()?.apps
    val bmp by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, entry?.key) {
        value = entry?.let { e -> withContext(Dispatchers.IO) { repo?.icon(e) } }
    }
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        val b = bmp
        if (b != null) Image(b, entry?.label, Modifier.size(size))
        else if (entry != null && repo == null) LetterIcon(entry.label, size)
    }
}

@Composable
fun PackageIcon(pkg: String, size: Dp, modifier: Modifier = Modifier) {
    val repo = AutoGlassApp.instanceOrNull()?.apps
    val bmp by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, pkg) {
        value = withContext(Dispatchers.IO) { repo?.iconForPackage(pkg) }
    }
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        bmp?.let { Image(it, null, Modifier.size(size)) }
    }
}

/** Ícone genérico com a inicial do app (usado só quando não há ícone real). */
@Composable
fun LetterIcon(label: String, size: Dp) {
    val hue = (label.hashCode() and 0xFFFF) % 360
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.28f))
            .background(Color.hsv(hue.toFloat(), 0.55f, 0.75f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(label.take(1).uppercase(), color = Color.White, fontSize = (size.value * 0.45f).sp, fontWeight = FontWeight.Bold)
    }
}

/** Ladrilho de app (ícone + nome) com toque, toque longo. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppTile(
    entry: AppEntry,
    iconSize: Dp,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AppIcon(entry, iconSize)
        if (showLabel) {
            Spacer(Modifier.height(6.dp))
            Text(
                entry.label,
                style = MaterialTheme.typography.labelLarge,
                color = Glass.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(iconSize * 1.8f),
            )
        }
    }
}

/** Pílula discreta: selecionada fica clara com texto escuro (estilo premium). */
@Composable
fun GlassChip(
    text: String,
    icon: ImageVector? = null,
    selected: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val fg = if (selected) Color(0xFF0B0D11) else Glass.textDim
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) Glass.text else Color.White.copy(alpha = 0.05f), RoundedCornerShape(50))
            .pointerInput(onClick) { detectTapGestures(onTap = { onClick() }) }
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(16.dp))
            if (text.isNotEmpty()) Spacer(Modifier.width(6.dp))
        }
        if (text.isNotEmpty()) {
            Text(text, color = fg, style = MaterialTheme.typography.labelLarge, fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal)
        }
    }
}

/** Abas em texto com sublinhado na cor de destaque. */
@Composable
fun TextTabs(items: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
        items.forEachIndexed { i, t ->
            Column(
                Modifier.clip(RoundedCornerShape(8.dp)).pointerInput(i) { detectTapGestures(onTap = { onSelect(i) }) }.padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    t.uppercase(),
                    color = if (i == selected) Glass.text else Glass.text.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.labelMedium.copy(shadow = Glass.textShadow),
                )
                Spacer(Modifier.height(5.dp))
                Box(
                    Modifier.width(if (i == selected) 18.dp else 0.dp).height(2.dp)
                        .background(Glass.accent, RoundedCornerShape(1.dp)),
                )
            }
        }
    }
}

/** Detecta dois toques sem bloquear os demais gestos do conteúdo. */
fun Modifier.onDoubleTap(action: () -> Unit): Modifier = this.pointerInput(action) {
    detectTapGestures(onDoubleTap = { action() })
}

/** Hora atual, atualizada a cada segundo. */
@Composable
fun rememberNow(): Long {
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(1000L - (value % 1000L))
        }
    }
    return now
}

/** Pequena animação de pressão. */
@Composable
fun Modifier.pressScale(pressed: Boolean): Modifier = this.scale(if (pressed) 0.96f else 1f)

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = Glass.textFaint,
        letterSpacing = androidx.compose.ui.unit.TextUnit(1.6f, androidx.compose.ui.unit.TextUnitType.Sp),
        modifier = modifier,
    )
}
