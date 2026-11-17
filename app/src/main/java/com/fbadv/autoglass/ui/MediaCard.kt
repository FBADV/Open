package com.fbadv.autoglass.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fbadv.autoglass.data.AppEntry
import com.fbadv.autoglass.media.NowPlaying
import com.fbadv.autoglass.ui.theme.Glass
import com.fbadv.autoglass.ui.theme.glass
import kotlinx.coroutines.delay

/**
 * Cartão de mídia: mostra e controla o que estiver tocando (Spotify,
 * YouTube Music, Deezer, Amazon Music, rádio ou player da central).
 */
@Composable
fun MediaCard(
    nowPlaying: NowPlaying?,
    hasAccess: Boolean,
    shortcuts: List<AppEntry>,
    radio: AppEntry?,
    player: AppEntry?,
    onOpenApp: (AppEntry) -> Unit,
    onOpenSource: (String) -> Unit,
    onGrantAccess: () -> Unit,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier.glass()) {
        val compact = maxHeight < 210.dp || maxWidth < 380.dp
        val artSize = minOf(maxHeight - (if (compact) 84.dp else 100.dp), maxWidth * 0.30f).coerceAtLeast(48.dp)
        Column(Modifier.fillMaxSize().padding(if (compact) 12.dp else 16.dp)) {
            Row(Modifier.weight(1f).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                // capa
                Box(
                    Modifier
                        .size(artSize)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFF2A3D66), Color(0xFF0E1628))))
                        .clickable { nowPlaying?.let { onOpenSource(it.packageName) } },
                    contentAlignment = Alignment.Center,
                ) {
                    val art = nowPlaying?.art
                    if (art != null) Image(art, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    else if (nowPlaying != null) PackageIcon(nowPlaying.packageName, 56.dp)
                    else Icon(Icons.Rounded.MusicNote, null, tint = Glass.textDim, modifier = Modifier.size(44.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    if (nowPlaying != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (nowPlaying.isPlaying) "Tocando agora" else "Pausado",
                                style = MaterialTheme.typography.labelMedium,
                                color = Glass.textDim,
                            )
                        }
                    }
                    Text(
                        nowPlaying?.title?.ifBlank { null } ?: "Nada tocando",
                        style = MaterialTheme.typography.titleLarge,
                        color = Glass.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        nowPlaying?.artist?.ifBlank { null } ?: if (hasAccess) "Abra um app de música ou o rádio" else "Toque em “Permitir” para ver as músicas",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Glass.textDim,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(8.dp))
                    ProgressBar(nowPlaying)
                    Spacer(Modifier.height(if (compact) 4.dp else 8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GlassIconButton(Icons.Rounded.SkipPrevious, "Anterior", size = if (compact) 40.dp else 48.dp) { onPrevious() }
                        GlassIconButton(
                            if (nowPlaying?.isPlaying == true) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            "Tocar/Pausar",
                            size = if (compact) 48.dp else 58.dp,
                            active = true,
                        ) { onPlayPause() }
                        GlassIconButton(Icons.Rounded.SkipNext, "Próxima", size = if (compact) 40.dp else 48.dp) { onNext() }
                    }
                }
            }
            Spacer(Modifier.height(if (compact) 8.dp else 12.dp))
            // fontes de mídia
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (!hasAccess) {
                    GlassChip("Permitir", Icons.Rounded.LockOpen, selected = true, onClick = onGrantAccess)
                }
                radio?.let { GlassChip("Rádio", Icons.Rounded.Radio, onClick = { onOpenApp(it) }) }
                player?.let { GlassChip("Player", Icons.Rounded.LibraryMusic, onClick = { onOpenApp(it) }) }
                shortcuts.take(if (compact) 4 else 6).forEach { e ->
                    Box(
                        Modifier.size(36.dp).clip(CircleShape).clickable { onOpenApp(e) },
                        contentAlignment = Alignment.Center,
                    ) { AppIcon(e, 32.dp) }
                }
            }
        }
    }
}

@Composable
private fun ProgressBar(np: NowPlaying?) {
    val progress by produceState(0f, np) {
        while (true) {
            value = if (np == null || np.durationMs <= 0) 0f else np.currentPosition().toFloat() / np.durationMs
            delay(500)
        }
    }
    Box(
        Modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(Color.White.copy(alpha = 0.18f)),
    ) {
        Box(
            Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(4.dp)
                .background(Brush.horizontalGradient(listOf(Glass.accent, Color(0xFF9AD0FF)))),
        )
    }
}
