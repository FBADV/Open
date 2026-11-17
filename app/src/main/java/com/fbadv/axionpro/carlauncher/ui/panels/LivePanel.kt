package com.fbadv.axionpro.carlauncher.ui.panels

import android.graphics.Rect
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.OpenInFull
import androidx.compose.material.icons.outlined.PictureInPicture
import androidx.compose.material.icons.outlined.SmartDisplay
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Text
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import com.fbadv.axionpro.carlauncher.ui.theme.Glass
import com.fbadv.axionpro.carlauncher.data.LivePanelMode
import com.fbadv.axionpro.carlauncher.ui.TextTabs
import com.fbadv.axionpro.carlauncher.ui.GlassIconButton
import com.fbadv.axionpro.carlauncher.ui.theme.glass

/**
 * Painel "ao vivo": mapa, YouTube ou widget funcionando dentro do launcher.
 * Dois toques (ou o botão ampliar) abrem o app correspondente em tela cheia;
 * com o modo janela ativo, o app real abre exatamente sobre o painel.
 */
@Composable
fun LivePanel(
    mode: LivePanelMode,
    widgetId: Int,
    darkMap: Boolean,
    freeform: Boolean,
    onModeChange: (LivePanelMode) -> Unit,
    onExpand: (LivePanelMode, Rect?) -> Unit,
    onPickWidget: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var bounds by remember { mutableStateOf<Rect?>(null) }
    Column(modifier.glass().padding(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextTabs(
                items = listOf("Mapa", "Vídeo", "Widget"),
                selected = LivePanelMode.entries.indexOf(mode),
                onSelect = { onModeChange(LivePanelMode.entries[it]) },
                modifier = Modifier.weight(1f).padding(start = 6.dp),
            )
            Spacer(Modifier.width(6.dp))
            if (mode == LivePanelMode.WIDGET && widgetId >= 0) {
                GlassIconButton(Icons.Outlined.Edit, "Trocar widget", size = 36.dp, onClick = onPickWidget)
                Spacer(Modifier.width(6.dp))
            }
            if (freeform && mode != LivePanelMode.WIDGET) {
                GlassIconButton(Icons.Outlined.PictureInPicture, "Abrir app em janela", size = 36.dp) {
                    onExpand(mode, bounds)
                }
                Spacer(Modifier.width(6.dp))
            }
            GlassIconButton(Icons.Outlined.OpenInFull, "Tela cheia", size = 36.dp) { onExpand(mode, null) }
        }
        Spacer(Modifier.padding(top = 8.dp))
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .onGloballyPositioned {
                    val b = it.boundsInWindow()
                    bounds = Rect(b.left.toInt(), b.top.toInt(), b.right.toInt(), b.bottom.toInt())
                },
        ) {
            if (LocalInspectionMode.current) {
                PreviewPlaceholder(mode)
            } else when (mode) {
                LivePanelMode.MAPA -> MapPanel(darkMap, onDoubleTap = { onExpand(mode, null) }, modifier = Modifier.fillMaxSize())
                LivePanelMode.VIDEO -> VideoPanel("https://m.youtube.com", onDoubleTap = { onExpand(mode, null) }, modifier = Modifier.fillMaxSize())
                LivePanelMode.WIDGET -> WidgetPanel(widgetId, onRequestPick = onPickWidget, modifier = Modifier.fillMaxSize())
            }
        }
    }
}

/** Simulação do conteúdo (somente nas imagens de pré-visualização geradas no build). */
@Composable
private fun PreviewPlaceholder(mode: LivePanelMode) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(Color(0xFF1A2233), Color(0xFF0D1320)))),
    ) {
        if (mode == LivePanelMode.MAPA) {
            Canvas(Modifier.fillMaxSize()) {
                val road = Color(0xFF3A4458)
                for (i in 0..8) {
                    val y = size.height * i / 8f
                    drawLine(road, Offset(0f, y), Offset(size.width, y + size.height * 0.15f), strokeWidth = 3f)
                }
                for (i in 0..10) {
                    val x = size.width * i / 10f
                    drawLine(road, Offset(x, 0f), Offset(x - size.width * 0.1f, size.height), strokeWidth = 3f)
                }
                drawLine(Glass.accent, Offset(size.width * 0.2f, size.height), Offset(size.width * 0.55f, size.height * 0.45f), strokeWidth = 10f)
                drawLine(Glass.accent, Offset(size.width * 0.55f, size.height * 0.45f), Offset(size.width * 0.9f, size.height * 0.3f), strokeWidth = 10f)
                drawCircle(Color.White, 14f, Offset(size.width * 0.55f, size.height * 0.45f))
                drawCircle(Glass.accent, 9f, Offset(size.width * 0.55f, size.height * 0.45f))
            }
        }
        Text(
            when (mode) {
                LivePanelMode.MAPA -> "Mapa ao vivo (GPS)"
                LivePanelMode.VIDEO -> "YouTube"
                LivePanelMode.WIDGET -> "Widget"
            },
            color = Glass.textDim,
            modifier = Modifier.align(Alignment.BottomStart).padding(12.dp),
        )
    }
}
