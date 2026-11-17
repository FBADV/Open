package com.fbadv.autoglass.ui

import android.Manifest
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.ScreenRotationAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fbadv.autoglass.AutoGlassApp
import com.fbadv.autoglass.data.AppEntry
import com.fbadv.autoglass.data.AppRole
import com.fbadv.autoglass.data.AccentTheme
import com.fbadv.autoglass.data.LightingMode
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import com.fbadv.autoglass.data.LivePanelMode
import com.fbadv.autoglass.data.SceneMode
import com.fbadv.autoglass.data.Settings
import com.fbadv.autoglass.launch.AppLauncher
import com.fbadv.autoglass.overlay.HomeBubbleService
import com.fbadv.autoglass.ui.theme.Glass
import com.fbadv.autoglass.ui.theme.glass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    settings: Settings,
    apps: List<AppEntry>,
    hasMediaAccess: Boolean,
    onPickWidget: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val app = AutoGlassApp.instance
    val store = app.settings
    val context = LocalContext.current
    var pickRole by remember { mutableStateOf<AppRole?>(null) }
    var importTarget by remember { mutableStateOf<SceneMode?>(null) }

    val pickVehicle = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val path = Images.import(context, uri, "vehicle_custom_${System.currentTimeMillis()}.png")
        if (path != null) store.update { it.copy(customVehiclePath = path) }
    }
    val pick360 = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            Images.import360(context, uris)
            store.update { it.copy(vehicle360Version = it.vehicle360Version + 1) }
        }
    }
    val count360 = remember(settings.vehicle360Version) { Images.dir360(context).listFiles()?.size ?: 0 }
    val pickScene = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val target = importTarget ?: return@rememberLauncherForActivityResult
        uri ?: return@rememberLauncherForActivityResult
        val path = Images.import(context, uri, "scene_${target.key}_${System.currentTimeMillis()}.jpg")
        if (path != null) store.update { it.copy(customScenes = it.customScenes + (target to path)) }
    }
    val locationPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        app.weather.start()
    }

    Column(modifier.fillMaxSize().glass(strength = 1.6f).padding(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Configurações", color = Glass.text, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            GlassIconButton(Icons.Outlined.Close, "Fechar", onClick = onClose)
        }
        Spacer(Modifier.size(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxSize()) {
            item {
                Section("Veículo") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        VehiclePreview(settings.customVehiclePath, Modifier.width(260.dp))
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            LabeledField("Modelo", settings.vehicleName) { v -> store.update { it.copy(vehicleName = v) } }
                            LabeledField("Versão", settings.vehicleTrim) { v -> store.update { it.copy(vehicleTrim = v) } }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                GlassChip("Usar foto do meu veículo", Icons.Outlined.Image, selected = true) { pickVehicle.launch("image/*") }
                                if (settings.customVehiclePath != null) {
                                    GlassChip("Voltar ao Compass 3D", Icons.Outlined.Restore) {
                                        store.update { it.copy(customVehiclePath = null, vehicleName = "Compass", vehicleTrim = "Limited") }
                                    }
                                }
                            }
                            Text(
                                "Padrão: modelo 3D do Compass Limited (branco, teto preto, aro 19) com giro 360°. Foto própria: PNG sem fundo, ¾ de frente.",
                                color = Glass.textFaint, style = MaterialTheme.typography.bodyMedium,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                GlassChip(if (count360 > 0) "Trocar fotos 360° ()" else "Fotos 360° (opcional)", Icons.Outlined.ScreenRotationAlt) {
                                    pick360.launch("image/*")
                                }
                                if (count360 > 0) GlassChip("Remover 360°", Icons.Outlined.Restore) {
                                    Images.dir360(context).deleteRecursively()
                                    store.update { it.copy(vehicle360Version = it.vehicle360Version + 1) }
                                }
                            }
                            Text(
                                "Giro 360° completo: selecione de 24 a 36 PNGs sem fundo, tirados ao redor do carro em intervalos iguais, na ordem do giro.",
                                color = Glass.textFaint, style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
            item {
                Section("Cor de destaque") {
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        AccentTheme.entries.forEach { a ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { store.update { it.copy(accent = a) } }) {
                                Box(
                                    Modifier.size(44.dp).clip(CircleShape)
                                        .background(Color(a.argb))
                                        .border(BorderStroke(if (settings.accent == a) 3.dp else 0.dp, Glass.text), CircleShape),
                                )
                                Spacer(Modifier.size(6.dp))
                                Text(a.label, color = if (settings.accent == a) Glass.text else Glass.textDim, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
            item {
                Section("Ambiente do veículo") {
                    Text("Iluminação", color = Glass.text, style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LightingMode.entries.forEach { m ->
                            GlassChip(m.label, selected = settings.lighting == m) { store.update { it.copy(lighting = m) } }
                        }
                    }
                    val hl by app.headlights.state.collectAsState()
                    Text(
                        "Faróis: farol aceso = modo escuro (o carro aparece com os faróis ligados); farol apagado = modo claro. " +
                            "Se a central não informar o farol, usa o nascer/pôr do sol.",
                        color = Glass.textFaint, style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        "Detecção do farol: " + hl.source + (hl.on?.let { if (it) " — ACESO" else " — APAGADO" } ?: ""),
                        color = Glass.textDim, style = MaterialTheme.typography.bodyMedium,
                    )
                    SettingSwitch("Efeito de chuva", "Chuva animada sobre o ambiente quando estiver chovendo no local", settings.rainEffect) { v ->
                        store.update { it.copy(rainEffect = v) }
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(SceneMode.entries) { m ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                ScenePreview(
                                    m, settings.customScenes[m], selected = settings.scene == m,
                                    modifier = Modifier.width(200.dp).clickable { store.update { it.copy(scene = m) } },
                                )
                                Spacer(Modifier.size(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    GlassChip("Foto própria", Icons.Outlined.Image) { importTarget = m; pickScene.launch("image/*") }
                                    if (settings.customScenes[m] != null) {
                                        GlassChip("", Icons.Outlined.Restore) {
                                            store.update { it.copy(customScenes = it.customScenes - m) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item {
                Section("Apps da central") {
                    AppRole.entries.forEach { role ->
                        val chosen = app.apps.resolveRole(role, settings.roles[role])
                        SettingRow(
                            title = role.label,
                            subtitle = (chosen?.label ?: "Não encontrado") + if (settings.roles[role] == null) " (automático)" else "",
                            onClick = { pickRole = role },
                        ) { chosen?.let { AppIcon(it, 36.dp) } }
                    }
                }
            }
            item {
                Section("Painel ao vivo") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LivePanelMode.entries.forEach { m ->
                            GlassChip(m.label, selected = settings.livePanel == m) { store.update { it.copy(livePanel = m) } }
                        }
                    }
                    SettingSwitch("Mapa escuro", "Combina com o visual de vidro e à noite", settings.darkMap) { v -> store.update { it.copy(darkMap = v) } }
                    SettingRow("Widget do painel", if (settings.widgetId >= 0) "Toque para trocar" else "Nenhum", onClick = onPickWidget)
                    SettingSwitch(
                        "Abrir apps em janela sobre o painel",
                        "Requer “janelas de formato livre” (freeform) ativo na central. Sem isso, abre em tela cheia.",
                        settings.freeformWindows,
                    ) { v -> store.update { it.copy(freeformWindows = v) } }
                }
            }
            item {
                Section("Tela") {
                    SettingSwitch("Tela cheia", "Oculta as barras do Android na tela inicial", settings.immersive) { v -> store.update { it.copy(immersive = v) } }
                    SettingSwitch("Relógio 24 horas", null, settings.clock24h) { v -> store.update { it.copy(clock24h = v) } }
                    Text("Tamanho da interface: ${(settings.uiScale * 100).roundToInt()}%", color = Glass.text, style = MaterialTheme.typography.titleMedium)
                    Text("Ajuste fino para telas de 9 a 15 polegadas", color = Glass.textFaint, style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = settings.uiScale,
                        onValueChange = { v -> store.update { it.copy(uiScale = (v * 20).roundToInt() / 20f) } },
                        valueRange = 0.75f..1.35f,
                        colors = SliderDefaults.colors(thumbColor = Glass.accent, activeTrackColor = Glass.accent),
                    )
                }
            }
            item {
                Section("Permissões e sistema") {
                    SettingRow(
                        "Acesso às mídias",
                        if (hasMediaAccess) "Concedido" else "Necessário para mostrar música, capa e progresso",
                        onClick = { AppLauncher.start(context, app.media.accessSettingsIntent()) },
                    )
                    SettingRow(
                        "Localização",
                        if (app.weather.hasLocationPermission()) "Concedida" else "Necessária para mapa e previsão do tempo",
                        onClick = {
                            locationPerm.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                        },
                    )
                    val isHome = remember(settings) { AppLauncher.isDefaultHome(context) }
                    SettingRow(
                        "Tela inicial padrão",
                        if (isHome) "Sim — o AutoGlass abre ao ligar a central e ao tocar em Home"
                        else "Não — toque para tornar o AutoGlass a tela principal",
                        onClick = { AppLauncher.start(context, AppLauncher.defaultHomeRequest(context)) },
                    )
                    SettingSwitch(
                        "Abrir ao ligar a central",
                        "Traz o AutoGlass para a frente quando a central liga ou acorda. No Android 10+ requer “Sobrepor a outros apps”.",
                        settings.autoStart,
                    ) { v ->
                        store.update { it.copy(autoStart = v) }
                        if (v && !AndroidSettings.canDrawOverlays(context)) AppLauncher.openOverlaySettings(context)
                        HomeBubbleService.refresh(context)
                    }
                    SettingSwitch(
                        "Botão Home flutuante",
                        "Bolha sobre os outros apps para voltar ao launcher com um toque",
                        settings.homeBubble,
                    ) { v ->
                        store.update { it.copy(homeBubble = v) }
                        if (v && !AndroidSettings.canDrawOverlays(context)) AppLauncher.openOverlaySettings(context)
                        HomeBubbleService.refresh(context)
                    }
                    SettingRow("Configurações do Android", null, onClick = { AppLauncher.openSystemSettings(context) })
                }
            }
            item {
                Section("Sobre") {
                    Text("AutoGlass Launcher 1.4", color = Glass.text, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Veículo: modelo 3D estilizado do Compass Limited criado para o app (tools/car3d_model.js). Ambientes (Estúdio, Rua, Dunas, Estrada, Lama) gerados para o app. " +
                            "Mapas © OpenStreetMap (sem chave de API). Tempo: Open-Meteo.com (CC BY 4.0). " +
                            "Jeep e Compass são marcas de seus respectivos donos; app pessoal sem vínculo com a fabricante.",
                        color = Glass.textDim,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }

    pickRole?.let { role ->
        AppPickerDialog(
            title = "App para ${role.label}",
            apps = apps,
            suggested = app.apps.candidates(role),
            onPick = { e -> store.setRole(role, e?.key); pickRole = null },
            onDismiss = { pickRole = null },
        )
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().glass(RoundedCornerShape(24.dp), strength = 0.8f).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SectionTitle(title)
        content()
    }
}

@Composable
private fun SettingRow(title: String, subtitle: String?, onClick: () -> Unit, trailing: @Composable () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(vertical = 6.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Glass.text, style = MaterialTheme.typography.titleMedium)
            subtitle?.let { Text(it, color = Glass.textDim, style = MaterialTheme.typography.bodyMedium) }
        }
        trailing()
    }
}

@Composable
private fun SettingSwitch(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Glass.text, style = MaterialTheme.typography.titleMedium)
            subtitle?.let { Text(it, color = Glass.textDim, style = MaterialTheme.typography.bodyMedium) }
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = Glass.accent),
        )
    }
}

@Composable
private fun LabeledField(label: String, value: String, onChange: (String) -> Unit) {
    Column {
        Text(label, color = Glass.textFaint, style = MaterialTheme.typography.labelMedium)
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = TextStyle(color = Glass.text, fontSize = 18.sp),
            cursorBrush = SolidColor(Glass.accent),
            modifier = Modifier.fillMaxWidth().glass(RoundedCornerShape(12.dp), strength = 1.3f).padding(10.dp),
        )
    }
}

@Composable
private fun VehiclePreview(custom: String?, modifier: Modifier) {
    val context = LocalContext.current
    val bmp by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, custom) {
        value = withContext(Dispatchers.IO) { Images.loadVehicle(context, custom)?.asImageBitmap() }
    }
    Box(modifier.aspectRatio(16f / 10f).glass(RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
        bmp?.let { Image(it, null, Modifier.fillMaxSize().padding(8.dp), contentScale = ContentScale.Fit) }
    }
}

@Composable
private fun ScenePreview(mode: SceneMode, custom: String?, selected: Boolean, modifier: Modifier) {
    val context = LocalContext.current
    val bmp by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, mode, custom) {
        value = withContext(Dispatchers.IO) {
            Images.loadScene(context, mode, custom)?.let { android.graphics.Bitmap.createScaledBitmap(it, 400, (400f * it.height / it.width).toInt(), true) }?.asImageBitmap()
        }
    }
    Box(
        modifier
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(16.dp))
            .border(BorderStroke(if (selected) 3.dp else 1.dp, if (selected) Glass.accent else Glass.stroke), RoundedCornerShape(16.dp)),
    ) {
        bmp?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
        Row(Modifier.align(Alignment.BottomStart).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(sceneIcon(mode), null, tint = Glass.text, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text(mode.label, color = Glass.text, style = MaterialTheme.typography.labelLarge)
        }
    }
}
