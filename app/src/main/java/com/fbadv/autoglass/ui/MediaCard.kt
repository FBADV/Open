package com.fbadv.autoglass.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Radio
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
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
 * A capa do álbum vira o fundo desfocado do cartão.
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
    val art = nowPlaying?.art
    val ambient = remember(art) {
        art?.let { runCatching { android.graphics.Bitmap.createScaledBitmap(it.asAndroidBitmap(), 12, 12, true).asImageBitmap() }.getOrNull() }
    }
    BoxWithConstraints(modifier.glass()) {
        val compact = maxHeight < 210.dp || maxWidth < 380.dp
        val artSize = minOf(maxHeight - (if (compact) 92.dp else 108.dp), maxWidth * 0.28f).coerceAtLeast(52.dp)
        if (ambient != null) {
            Image(ambient, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alpha = 0.45f, filterQuality = FilterQuality.Low)
            Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0x99000000), Color(0xE6000000)))))
        }
        Column(Modifier.fillMaxSize().padding(if (compact) 14.dp else 18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (nowPlaying?.isPlaying == true) "TOCANDO AGORA" else "MÍDIA",
                    style = MaterialTheme.typography.labelMedium,
                    color = Glass.textFaint,
                    modifier = Modifier.weight(1f),
                )
                nowPlaying?.let { PackageIcon(it.packageName, 18.dp) }
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.weight(1f).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(artSize)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFF1C2028), Color(0xFF0E1014))))
                        .clickable { nowPlaying?.let { onOpenSource(it.packageName) } },
                    contentAlignment = Alignment.Center,
                ) {
                    if (art != null) Image(art, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    else Icon(Icons.Outlined.MusicNote, null, tint = Glass.textFaint, modifier = Modifier.size(artSize * 0.4f))
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        nowPlaying?.title?.ifBlank { null } ?: "Nada tocando",
                        style = MaterialTheme.typography.titleLarge,
                        color = Glass.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        nowPlaying?.artist?.ifBlank { null } ?: if (hasAccess) "Escolha uma fonte abaixo" else "Permita o acesso às mídias",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Glass.textDim,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(12.dp))
                    Progress(nowPlaying)
                }
            }
            Spacer(Modifier.height(if (compact) 8.dp else 12.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                GlassIconButton(Icons.Rounded.SkipPrevious, "Anterior", size = 42.dp, onClick = onPrevious)
                Spacer(Modifier.width(10.dp))
                GlassIconButton(
                    if (nowPlaying?.isPlaying == true) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    "Tocar/Pausar", size = 50.dp, filled = true, onClick = onPlayPause,
                )
                Spacer(Modifier.width(10.dp))
                GlassIconButton(Icons.Rounded.SkipNext, "Próxima", size = 42.dp, onClick = onNext)
                Spacer(Modifier.width(14.dp))
                Row(
                    Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (!hasAccess) GlassChip("Permitir", Icons.Outlined.LockOpen, selected = true, onClick = onGrantAccess)
                    radio?.let { GlassIconButton(Icons.Outlined.Radio, "Rádio", size = 38.dp) { onOpenApp(it) } }
                    player?.let { GlassIconButton(Icons.Outlined.LibraryMusic, "Player", size = 38.dp) { onOpenApp(it) } }
                    shortcuts.take(if (compact) 2 else 4).forEach { e ->
                        Box(Modifier.size(38.dp).clip(CircleShape).clickable { onOpenApp(e) }, contentAlignment = Alignment.Center) {
                            AppIcon(e, 30.dp)
                        }
                    }
                }
            }
        }
    }
}

private fun fmt(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}

@Composable
private fun Progress(np: NowPlaying?) {
    val pos by produceState(0L, np) {
        while (true) { value = np?.currentPosition() ?: 0L; delay(500) }
    }
    val dur = np?.durationMs ?: 0L
    val frac = if (dur > 0) (pos.toFloat() / dur).coerceIn(0f, 1f) else 0f
    Column {
        Box(Modifier.fillMaxWidth().height(2.dp).background(Color.White.copy(alpha = 0.12f))) {
            Box(Modifier.fillMaxWidth(frac).height(2.dp).background(Glass.accent))
        }
        if (dur > 0) {
            Spacer(Modifier.height(6.dp))
            Row {
                Text(fmt(pos), color = Glass.textFaint, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Normal, modifier = Modifier.weight(1f))
                Text(fmt(dur), color = Glass.textFaint, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Normal)
            }
        }
    }
}
