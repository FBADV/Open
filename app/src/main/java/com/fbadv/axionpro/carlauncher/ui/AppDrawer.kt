package com.fbadv.axionpro.carlauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.fbadv.axionpro.carlauncher.data.AppEntry
import com.fbadv.axionpro.carlauncher.data.AppRole
import com.fbadv.axionpro.carlauncher.ui.theme.Glass
import com.fbadv.axionpro.carlauncher.ui.theme.glass

/** Todos os apps instalados, com busca e ações de toque longo. */
@Composable
fun AppDrawer(
    apps: List<AppEntry>,
    onOpen: (AppEntry) -> Unit,
    onPin: (AppEntry) -> Unit,
    onSetRole: (AppRole, AppEntry) -> Unit,
    onInfo: (AppEntry) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(apps, query) {
        if (query.isBlank()) apps else apps.filter { it.label.contains(query.trim(), ignoreCase = true) }
    }
    Column(modifier.fillMaxSize().glass(strength = 1.6f).padding(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SearchField(query, { query = it }, Modifier.weight(1f))
            Spacer(Modifier.width(12.dp))
            GlassIconButton(Icons.Outlined.Close, "Fechar", onClick = onClose)
        }
        Spacer(Modifier.size(14.dp))
        LazyVerticalGrid(
            columns = GridCells.Adaptive(116.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(filtered, key = { it.key }) { e ->
                var menu by remember { mutableStateOf(false) }
                Box {
                    AppTile(e, 60.dp, onLongClick = { menu = true }, onClick = { onOpen(e) })
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("Fixar na tela inicial") }, onClick = { menu = false; onPin(e) })
                        AppRole.entries.forEach { r ->
                            DropdownMenuItem(text = { Text("Usar como ${r.label}") }, onClick = { menu = false; onSetRole(r, e) })
                        }
                        DropdownMenuItem(text = { Text("Informações do app") }, onClick = { menu = false; onInfo(e) })
                    }
                }
            }
        }
    }
}

@Composable
fun SearchField(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .glass(RoundedCornerShape(50), strength = 1.4f)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Search, null, tint = Glass.textDim)
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text("Buscar apps", color = Glass.textFaint, fontSize = 17.sp)
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                textStyle = TextStyle(color = Glass.text, fontSize = 17.sp),
                cursorBrush = SolidColor(Glass.accent),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Diálogo para escolher um app (favoritos ou funções como Navegação/Rádio). */
@Composable
fun AppPickerDialog(
    title: String,
    apps: List<AppEntry>,
    suggested: List<AppEntry>,
    onPick: (AppEntry?) -> Unit,
    onDismiss: () -> Unit,
    allowClear: Boolean = true,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(apps, suggested, query) {
        val base = if (query.isBlank()) suggested + apps.filter { it !in suggested } else apps
        base.filter { query.isBlank() || it.label.contains(query.trim(), true) }
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .fillMaxWidth(0.8f)
                .heightIn(max = 560.dp)
                .glass(strength = 3f)
                .background(Color(0x66070B16), Glass.shape)
                .padding(18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = Glass.text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                if (allowClear) GlassChip("Automático / nenhum", onClick = { onPick(null) })
            }
            Spacer(Modifier.size(10.dp))
            SearchField(query, { query = it }, Modifier.fillMaxWidth())
            Spacer(Modifier.size(10.dp))
            if (suggested.isNotEmpty() && query.isBlank()) {
                SectionTitle("Sugeridos primeiro", Modifier.padding(start = 6.dp, bottom = 4.dp))
            }
            LazyVerticalGrid(columns = GridCells.Adaptive(108.dp)) {
                items(filtered, key = { it.key }) { e -> AppTile(e, 52.dp, onClick = { onPick(e) }) }
            }
        }
    }
}
