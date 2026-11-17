package com.fbadv.autoglass.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SmartDisplay
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fbadv.autoglass.data.AppEntry
import com.fbadv.autoglass.data.AppRole
import com.fbadv.autoglass.ui.theme.Glass
import com.fbadv.autoglass.ui.theme.glass

data class RailItem(
    val icon: ImageVector,
    val label: String,
    val role: AppRole?,
    val active: Boolean = false,
    val onClick: () -> Unit,
    val onLongClick: (() -> Unit)? = null,
)

/** Barra de atalhos (vertical na paisagem, horizontal no retrato). */
@Composable
fun SideRail(
    items: List<RailItem>,
    bottomItems: List<RailItem>,
    vertical: Boolean,
    modifier: Modifier = Modifier,
) {
    val btn = 52.dp
    if (vertical) {
        Column(
            modifier.glass(RoundedCornerShape(36.dp)).padding(vertical = 14.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items.forEach { RailButton(it, btn) }
            Spacer(Modifier.weight(1f))
            bottomItems.forEach { RailButton(it, btn) }
        }
    } else {
        Row(
            modifier.glass(RoundedCornerShape(36.dp)).padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            (items + bottomItems).forEach { RailButton(it, btn) }
        }
    }
}

@Composable
private fun RailButton(item: RailItem, size: androidx.compose.ui.unit.Dp) {
    GlassIconButton(
        icon = item.icon,
        contentDescription = item.label,
        size = size,
        active = item.active,
        onLongClick = item.onLongClick,
        onClick = item.onClick,
    )
}

fun roleIcon(role: AppRole): ImageVector = when (role) {
    AppRole.NAVIGATION -> Icons.Rounded.Navigation
    AppRole.MUSIC -> Icons.Rounded.MusicNote
    AppRole.RADIO -> Icons.Rounded.Radio
    AppRole.PLAYER -> Icons.Rounded.MusicNote
    AppRole.VIDEO -> Icons.Rounded.SmartDisplay
    AppRole.PHONE -> Icons.Rounded.Phone
}

val HomeIcon = Icons.Rounded.Home
val AppsIcon = Icons.Rounded.Apps
val SettingsIcon = Icons.Rounded.Settings

/** Até 4 apps fixados na tela, escolhidos pelo usuário. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FavoritesCard(
    favorites: List<AppEntry?>,
    columns: Int,
    onOpen: (AppEntry) -> Unit,
    onPick: (slot: Int) -> Unit,
    onRemove: (slot: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier.glass().padding(10.dp)) {
        val rows = (favorites.size + columns - 1) / columns
        val cellH = maxHeight / rows
        val cellW = maxWidth / columns
        val icon = (minOf(cellH, cellW) * 0.46f).coerceIn(36.dp, 88.dp)
        Column(Modifier.fillMaxSize()) {
            for (r in 0 until rows) {
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    for (c in 0 until columns) {
                        val slot = r * columns + c
                        if (slot >= favorites.size) { Spacer(Modifier.weight(1f)); continue }
                        val entry = favorites[slot]
                        var menu by remember { mutableStateOf(false) }
                        Box(Modifier.weight(1f).fillMaxHeight().padding(4.dp)) {
                            Column(
                                Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(22.dp))
                                    .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.06f))
                                    .combinedClickable(
                                        onClick = { if (entry != null) onOpen(entry) else onPick(slot) },
                                        onLongClick = { if (entry != null) menu = true else onPick(slot) },
                                    )
                                    .padding(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                if (entry != null) {
                                    AppIcon(entry, icon)
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        entry.label,
                                        color = Glass.text,
                                        style = MaterialTheme.typography.labelLarge,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center,
                                    )
                                } else {
                                    Icon(Icons.Rounded.Add, "Adicionar app", tint = Glass.textDim, modifier = Modifier.size(icon * 0.7f))
                                    Text("Adicionar", color = Glass.textFaint, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                DropdownMenuItem(text = { Text("Trocar app") }, onClick = { menu = false; onPick(slot) })
                                DropdownMenuItem(text = { Text("Remover da tela") }, onClick = { menu = false; onRemove(slot) })
                            }
                        }
                    }
                }
            }
        }
    }
}

