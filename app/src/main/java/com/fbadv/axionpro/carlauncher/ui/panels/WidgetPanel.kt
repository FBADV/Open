package com.fbadv.axionpro.carlauncher.ui.panels

import android.app.Activity
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.widget.FrameLayout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.drawable.toBitmap
import com.fbadv.axionpro.carlauncher.AxionApp
import com.fbadv.axionpro.carlauncher.ui.GlassChip
import com.fbadv.axionpro.carlauncher.ui.theme.Glass
import com.fbadv.axionpro.carlauncher.ui.theme.glass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Mostra o widget escolhido (ex.: Google Maps, Spotify, rádio da central). */
@Composable
fun WidgetPanel(widgetId: Int, onRequestPick: () -> Unit, modifier: Modifier = Modifier) {
    val widgets = AxionApp.instance.widgets
    val info = remember(widgetId) { widgets.info(widgetId) }
    if (info == null) {
        Column(
            modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Outlined.Widgets, null, tint = Glass.textDim, modifier = Modifier.size(48.dp))
            Spacer(Modifier.size(10.dp))
            Text("Nenhum widget escolhido", color = Glass.text, style = MaterialTheme.typography.titleMedium)
            Text(
                "Use widgets do Google Maps, Spotify, rádio da central e outros",
                color = Glass.textDim,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp),
            )
            Spacer(Modifier.size(8.dp))
            GlassChip("Escolher widget", Icons.Outlined.Widgets, selected = true, onClick = onRequestPick)
        }
        return
    }
    val density = LocalDensity.current
    BoxWithConstraints(modifier) {
        val wDp = with(density) { constraints.maxWidth.toDp() }.value.toInt()
        val hDp = with(density) { constraints.maxHeight.toDp() }.value.toInt()
        key(widgetId) {
            AndroidView(
                factory = { ctx -> widgets.createView(widgetId) ?: FrameLayout(ctx) },
                update = { v ->
                    if (v is android.appwidget.AppWidgetHostView) widgets.updateSize(v, wDp, hDp)
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/**
 * Fluxo completo para escolher e vincular um widget: lista os provedores,
 * pede permissão de vínculo se necessário e abre a configuração do widget.
 */
class WidgetBinder(val start: () -> Unit)

@Composable
fun rememberWidgetBinder(onBound: (Int) -> Unit): Pair<WidgetBinder, @Composable () -> Unit> {
    val widgets = AxionApp.instance.widgets
    var showPicker by remember { mutableStateOf(false) }
    var pendingId by remember { mutableIntStateOf(-1) }

    val configure = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode == Activity.RESULT_OK) onBound(pendingId) else widgets.delete(pendingId)
    }
    fun finish(id: Int) {
        val cfg = widgets.configureIntent(id)
        if (cfg != null) {
            pendingId = id
            runCatching { configure.launch(cfg) }.onFailure { onBound(id) }
        } else onBound(id)
    }
    val bind = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode == Activity.RESULT_OK) finish(pendingId) else widgets.delete(pendingId)
    }
    val binder = remember { WidgetBinder { showPicker = true } }
    val ui: @Composable () -> Unit = {
        if (showPicker) {
            WidgetPickerDialog(
                onDismiss = { showPicker = false },
                onPick = { provider ->
                    showPicker = false
                    val id = widgets.allocate()
                    pendingId = id
                    val intent = widgets.bindOrIntent(id, provider)
                    if (intent == null) finish(id) else bind.launch(intent)
                },
            )
        }
    }
    return binder to ui
}

@Composable
private fun WidgetPickerDialog(onDismiss: () -> Unit, onPick: (ComponentName) -> Unit) {
    val context = LocalContext.current
    val widgets = AxionApp.instance.widgets
    val providers by produceState<List<AppWidgetProviderInfo>>(emptyList()) {
        value = withContext(Dispatchers.IO) { widgets.providers() }
    }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 520.dp)
                .glass(strength = 3f)
                .padding(16.dp),
        ) {
            Text("Escolha um widget", color = Glass.text, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.size(10.dp))
            LazyColumn {
                items(providers, key = { it.provider.flattenToString() }) { p ->
                    val label = remember(p) { p.loadLabel(context.packageManager) ?: p.provider.className }
                    val appLabel = remember(p) {
                        runCatching {
                            context.packageManager.getApplicationLabel(
                                context.packageManager.getApplicationInfo(p.provider.packageName, 0),
                            ).toString()
                        }.getOrDefault(p.provider.packageName)
                    }
                    val icon = remember(p) {
                        runCatching { p.loadIcon(context, 0)?.toBitmap(96, 96)?.asImageBitmap() }.getOrNull()
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onPick(p.provider) }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(40.dp)) { icon?.let { Image(it, null, Modifier.fillMaxSize()) } }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(appLabel, color = Glass.text, style = MaterialTheme.typography.titleMedium)
                            Text(label, color = Glass.textDim, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}
