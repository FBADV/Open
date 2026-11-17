package com.fbadv.autoglass.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Radio
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SignalCellularAlt
import androidx.compose.material.icons.outlined.SmartDisplay
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fbadv.autoglass.data.AppEntry
import com.fbadv.autoglass.data.AppRole
import com.fbadv.autoglass.ui.theme.Glass
import com.fbadv.autoglass.ui.theme.glass
import com.fbadv.autoglass.weather.Weather
import kotlin.math.roundToInt

enum class NetworkState { WIFI, CELLULAR, OFFLINE }

data class RailItem(
    val icon: ImageVector,
    val label: String,
    val role: AppRole?,
    val active: Boolean = false,
    val onClick: () -> Unit,
    val onLongClick: (() -> Unit)? = null,
)

fun roleIcon(role: AppRole): ImageVector = when (role) {
    AppRole.NAVIGATION -> Icons.Outlined.Navigation
    AppRole.MUSIC -> Icons.Outlined.MusicNote
    AppRole.RADIO -> Icons.Outlined.Radio
    AppRole.PLAYER -> Icons.Outlined.MusicNote
    AppRole.VIDEO -> Icons.Outlined.SmartDisplay
    AppRole.PHONE -> Icons.Outlined.Phone
}

val HomeIcon = Icons.Outlined.Home
val AppsIcon = Icons.Outlined.Apps
val SettingsIcon = Icons.Outlined.Settings

/** Barra de status superior: tempo à esquerda, conectividade à direita. */
@Composable
fun StatusBar(weather: Weather?, network: NetworkState, modifier: Modifier = Modifier) {
    Row(modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(weatherIcon(weather), null, tint = Glass.text, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            weather?.let { "${it.temperature.roundToInt()}°" } ?: "--°",
            color = Glass.text, style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.width(10.dp))
        Text(
            weather?.let { "${it.city.uppercase()}  ·  ${it.description.uppercase()}  ·  ${it.max.roundToInt()}° / ${it.min.roundToInt()}°" }
                ?: "BUSCANDO LOCALIZAÇÃO",
            color = Glass.textDim, style = MaterialTheme.typography.labelMedium,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Icon(Icons.Outlined.NearMe, null, tint = Glass.textDim, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(14.dp))
        Icon(
            when (network) {
                NetworkState.WIFI -> Icons.Outlined.Wifi
                NetworkState.CELLULAR -> Icons.Outlined.SignalCellularAlt
                NetworkState.OFFLINE -> Icons.Outlined.CloudOff
            },
            null, tint = if (network == NetworkState.OFFLINE) Glass.textFaint else Glass.text, modifier = Modifier.size(17.dp),
        )
    }
}

/**
 * Dock inferior: funções do carro à esquerda, até 4 apps favoritos no
 * centro e acesso a todos os apps/configurações à direita.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Dock(
    items: List<RailItem>,
    favorites: List<AppEntry?>,
    trailing: List<RailItem>,
    onOpenFavorite: (AppEntry) -> Unit,
    onPickFavorite: (Int) -> Unit,
    onRemoveFavorite: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier.glass(RoundedCornerShape(28.dp))) {
        val tight = maxWidth < 720.dp
        val btn: Dp = if (tight) 42.dp else 50.dp
        val fav: Dp = if (tight) 34.dp else 40.dp
        Row(
            Modifier.fillMaxWidth().fillMaxHeight().padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            items.forEach { DockButton(it, btn) }
            DockDivider()
            favorites.forEachIndexed { slot, entry ->
                var menu by remember { mutableStateOf(false) }
                Box {
                    Box(
                        Modifier
                            .size(btn)
                            .clip(RoundedCornerShape(14.dp))
                            .combinedClickable(
                                onClick = { if (entry != null) onOpenFavorite(entry) else onPickFavorite(slot) },
                                onLongClick = { if (entry != null) menu = true else onPickFavorite(slot) },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (entry != null) AppIcon(entry, fav)
                        else Box(
                            Modifier.size(fav).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.05f)),
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Outlined.Add, "Adicionar app", tint = Glass.textFaint, modifier = Modifier.size(fav * 0.5f)) }
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("Trocar app") }, onClick = { menu = false; onPickFavorite(slot) })
                        DropdownMenuItem(text = { Text("Remover do dock") }, onClick = { menu = false; onRemoveFavorite(slot) })
                    }
                }
            }
            DockDivider()
            trailing.forEach { DockButton(it, btn) }
        }
    }
}

@Composable
private fun DockDivider() {
    Box(Modifier.width(1.dp).height(28.dp).background(Glass.hairline))
}

@Composable
private fun DockButton(item: RailItem, size: Dp) {
    Box(contentAlignment = Alignment.BottomCenter) {
        GlassIconButton(
            icon = item.icon,
            contentDescription = item.label,
            size = size,
            active = item.active,
            onLongClick = item.onLongClick,
            onClick = item.onClick,
        )
        if (item.active) Box(Modifier.padding(bottom = 2.dp).size(4.dp).background(Glass.accent, CircleShape))
    }
}
