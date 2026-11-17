package com.fbadv.autoglass.ui

import android.graphics.Rect
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.fbadv.autoglass.ui.theme.Glass
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fbadv.autoglass.AutoGlassApp
import com.fbadv.autoglass.data.AppEntry
import com.fbadv.autoglass.data.AppRole
import com.fbadv.autoglass.data.KnownApps
import com.fbadv.autoglass.data.LivePanelMode
import com.fbadv.autoglass.data.SettingsStore
import com.fbadv.autoglass.launch.AppLauncher
import com.fbadv.autoglass.ui.panels.LivePanel
import com.fbadv.autoglass.ui.panels.rememberWidgetBinder
import com.fbadv.autoglass.ui.theme.Backdrop
import com.fbadv.autoglass.ui.theme.LocalBackdrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.min

enum class Screen { HOME, APPS, SETTINGS }

/** Altura de referência do layout: tudo escala a partir dela (telas de 9" a 15"). */
private const val DESIGN_SHORT_SIDE = 600f

@Composable
fun LauncherRoot(screen: MutableState<Screen>) {
    val app = AutoGlassApp.instance
    val context = LocalContext.current
    val settings by app.settings.state.collectAsStateWithLifecycle()
    val apps by app.apps.apps.collectAsStateWithLifecycle()
    val nowPlaying by app.media.nowPlaying.collectAsStateWithLifecycle()
    val hasAccess by app.media.hasAccess.collectAsStateWithLifecycle()
    val weather by app.weather.weather.collectAsStateWithLifecycle()

    val customScene = settings.customScenes[settings.scene]
    val isDay = rememberIsDay(settings.lighting, weather)
    val sceneBmp by produceState<android.graphics.Bitmap?>(null, settings.scene, customScene, isDay) {
        value = withContext(Dispatchers.IO) { Images.loadScene(context, settings.scene, customScene, isDay) }
    }
    val scene: ImageBitmap? = remember(sceneBmp) { sceneBmp?.asImageBitmap() }
    val blurred by produceState<ImageBitmap?>(null, sceneBmp) {
        val src = sceneBmp ?: return@produceState
        value = withContext(Dispatchers.Default) { Images.blurred(src).asImageBitmap() }
    }
    val vehicle by produceState<ImageBitmap?>(null, settings.customVehiclePath) {
        value = withContext(Dispatchers.IO) { Images.loadVehicle(context, settings.customVehiclePath)?.asImageBitmap() }
    }

    // diálogos de escolha de app
    var pickFavoriteSlot by remember { mutableStateOf<Int?>(null) }
    var pickRole by remember { mutableStateOf<AppRole?>(null) }
    val (widgetBinder, widgetUi) = rememberWidgetBinder { id ->
        val old = app.settings.value.widgetId
        if (old >= 0 && old != id) app.widgets.delete(old)
        app.settings.update { it.copy(widgetId = id, livePanel = LivePanelMode.WIDGET) }
    }

    fun open(e: AppEntry, bounds: Rect? = null) = AppLauncher.launch(context, e, bounds)
    fun openRole(role: AppRole, bounds: Rect? = null) {
        val e = app.apps.resolveRole(role, settings.roles[role])
        if (e != null) open(e, bounds) else pickRole = role
    }

    val favorites = remember(settings.favorites, apps) {
        List(SettingsStore.MAX_FAVORITES) { app.apps.find(settings.favorites.getOrNull(it)) }
    }
    val radio = remember(settings.roles, apps) { app.apps.resolveRole(AppRole.RADIO, settings.roles[AppRole.RADIO]) }
    val player = remember(settings.roles, apps) { app.apps.resolveRole(AppRole.PLAYER, settings.roles[AppRole.PLAYER]) }
    val shortcuts = remember(apps) { KnownApps.MUSIC.mapNotNull { p -> apps.firstOrNull { it.packageName == p } } }

    val actions = DashboardActions(
        openApp = { open(it) },
        openRole = { r, b -> openRole(r, b) },
        pickRole = { pickRole = it },
        pickFavorite = { pickFavoriteSlot = it },
        removeFavorite = { slot -> app.settings.setFavorite(slot, null) },
        pickWidget = { widgetBinder.start() },
        setScene = { sc -> app.settings.update { it.copy(scene = sc) } },
        setLiveMode = { m -> app.settings.update { it.copy(livePanel = m) } },
        openWidgetApp = {
            app.widgets.info(app.settings.value.widgetId)?.provider?.packageName?.let { AppLauncher.launchPackage(context, it) }
        },
        openPackage = { pkg -> AppLauncher.launchPackage(context, pkg) },
        grantMediaAccess = { AppLauncher.start(context, app.media.accessSettingsIntent()) },
        previous = { app.media.previous() },
        playPause = { app.media.playPause() },
        next = { app.media.next() },
        goTo = { screen.value = it },
    )

    SideEffect { Glass.accent = Color(settings.accent.argb) }
    val network = rememberNetworkState()
    val rain = settings.rainEffect && isRaining(weather)
    LauncherSurface(scene, blurred, settings.uiScale, dimForOverlay = screen.value != Screen.HOME, rain = if (rain) rainIntensity(weather) else 0f) {
                AnimatedContent(
                    targetState = screen.value,
                    transitionSpec = { (fadeIn(tween(250)) + scaleIn(tween(250), initialScale = 0.97f)) togetherWith fadeOut(tween(150)) },
                    label = "screen",
                ) { s ->
                    when (s) {
                        Screen.HOME -> Dashboard(
                            settings = settings,
                            network = network,
                            night = !isDay,
                            vehicle = vehicle,
                            weather = weather,
                            nowPlaying = nowPlaying,
                            hasMediaAccess = hasAccess,
                            favorites = favorites,
                            radio = radio,
                            player = player,
                            shortcuts = shortcuts,
                            actions = actions,
                        )
                        Screen.APPS -> AppDrawer(
                            apps = apps,
                            onOpen = { open(it) },
                            onPin = { app.settings.addFavorite(it.key) },
                            onSetRole = { r, e -> app.settings.setRole(r, e.key) },
                            onInfo = { AppLauncher.openAppInfo(context, it.packageName) },
                            onClose = { screen.value = Screen.HOME },
                            modifier = Modifier.padding(16.dp),
                        )
                        Screen.SETTINGS -> SettingsScreen(
                            settings = settings,
                            apps = apps,
                            hasMediaAccess = hasAccess,
                            onPickWidget = { widgetBinder.start() },
                            onClose = { screen.value = Screen.HOME },
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }

                pickFavoriteSlot?.let { slot ->
                    AppPickerDialog(
                        title = "Escolha o app para a tela inicial",
                        apps = apps,
                        suggested = apps.filter { a ->
                            a.packageName in KnownApps.MUSIC || a.packageName in KnownApps.VIDEO || a.packageName in KnownApps.NAVIGATION
                        },
                        onPick = { e -> app.settings.setFavorite(slot, e?.key); pickFavoriteSlot = null },
                        onDismiss = { pickFavoriteSlot = null },
                    )
                }
                pickRole?.let { role ->
                    AppPickerDialog(
                        title = "App para ${role.label}",
                        apps = apps,
                        suggested = app.apps.candidates(role),
                        onPick = { e -> app.settings.setRole(role, e?.key); pickRole = null },
                        onDismiss = { pickRole = null },
                    )
                }
                widgetUi()
    }
}

/**
 * Superfície do launcher: escala a interface a partir do menor lado da tela
 * (mesma proporção visual em 9", 10", 12" ou 15") e fornece o fundo desfocado
 * usado pelos painéis de vidro.
 */
@Composable
fun LauncherSurface(
    scene: ImageBitmap?,
    blurred: ImageBitmap?,
    uiScale: Float,
    dimForOverlay: Boolean = false,
    rain: Float = 0f,
    content: @Composable () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize().background(Glass.surface)) {
        val wPx = constraints.maxWidth
        val hPx = constraints.maxHeight
        val density = (min(wPx, hPx) / DESIGN_SHORT_SIDE * uiScale).coerceIn(0.6f, 4f)
        val backdrop = blurred?.let { Backdrop(it, IntSize(wPx, hPx)) }
        CompositionLocalProvider(
            LocalDensity provides Density(density, 1f),
            LocalBackdrop provides backdrop,
        ) {
            Box(Modifier.fillMaxSize()) {
                // ambiente em tela cheia, com tratamento cinematográfico
                scene?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(0f to Color(0xCC050608), 0.16f to Color(0x33050608), 0.72f to Color(0x00050608), 1f to Color(0xE6050608)),
                    ),
                )
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.horizontalGradient(0f to Color(0x8C050608), 0.32f to Color(0x1A050608), 0.55f to Color.Transparent, 1f to Color(0x99050608)),
                    ),
                )
                if (rain > 0f) RainOverlay(rain)
                if (dimForOverlay) Box(Modifier.fillMaxSize().background(Color(0xB3050608)))
                content()
            }
        }
    }
}

class DashboardActions(
    val openApp: (AppEntry) -> Unit,
    val openRole: (AppRole, Rect?) -> Unit,
    val pickRole: (AppRole) -> Unit,
    val pickFavorite: (Int) -> Unit,
    val removeFavorite: (Int) -> Unit,
    val pickWidget: () -> Unit,
    val setScene: (com.fbadv.autoglass.data.SceneMode) -> Unit,
    val setLiveMode: (LivePanelMode) -> Unit,
    val openWidgetApp: () -> Unit,
    val openPackage: (String) -> Unit,
    val grantMediaAccess: () -> Unit,
    val previous: () -> Unit,
    val playPause: () -> Unit,
    val next: () -> Unit,
    val goTo: (Screen) -> Unit,
) {
    companion object {
        val NONE = DashboardActions({}, { _, _ -> }, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})
    }
}

@Composable
fun Dashboard(
    settings: com.fbadv.autoglass.data.Settings,
    network: NetworkState,
    night: Boolean,
    vehicle: ImageBitmap?,
    weather: com.fbadv.autoglass.weather.Weather?,
    nowPlaying: com.fbadv.autoglass.media.NowPlaying?,
    hasMediaAccess: Boolean,
    favorites: List<AppEntry?>,
    radio: AppEntry?,
    player: AppEntry?,
    shortcuts: List<AppEntry>,
    actions: DashboardActions,
) {
    val dockItems = listOf(
        RailItem(HomeIcon, "Início", null, active = true, onClick = {}),
    ) + listOf(AppRole.NAVIGATION, AppRole.MUSIC, AppRole.RADIO, AppRole.PHONE, AppRole.VIDEO).map { r ->
        RailItem(roleIcon(r), r.label, r, onClick = { actions.openRole(r, null) }, onLongClick = { actions.pickRole(r) })
    }
    val dockTrailing = listOf(
        RailItem(AppsIcon, "Apps", null, onClick = { actions.goTo(Screen.APPS) }),
        RailItem(SettingsIcon, "Configurações", null, onClick = { actions.goTo(Screen.SETTINGS) }),
    )

    val stage: @Composable (Modifier) -> Unit = { m ->
        VehicleStage(
            vehicle = vehicle,
            sceneMode = settings.scene,
            vehicleName = settings.vehicleName,
            vehicleTrim = settings.vehicleTrim,
            clock24h = settings.clock24h,
            night = night,
            onSceneChange = actions.setScene,
            modifier = m,
        )
    }
    val live: @Composable (Modifier) -> Unit = { m ->
        LivePanel(
            mode = settings.livePanel,
            widgetId = settings.widgetId,
            darkMap = settings.darkMap,
            freeform = settings.freeformWindows,
            onModeChange = actions.setLiveMode,
            onExpand = { mode, bounds ->
                when (mode) {
                    LivePanelMode.MAPA -> actions.openRole(AppRole.NAVIGATION, bounds)
                    LivePanelMode.VIDEO -> actions.openRole(AppRole.VIDEO, bounds)
                    LivePanelMode.WIDGET -> actions.openWidgetApp()
                }
            },
            onPickWidget = actions.pickWidget,
            modifier = m,
        )
    }
    val media: @Composable (Modifier) -> Unit = { m ->
        MediaCard(
            nowPlaying = nowPlaying,
            hasAccess = hasMediaAccess,
            shortcuts = shortcuts,
            radio = radio,
            player = player,
            onOpenApp = actions.openApp,
            onOpenSource = actions.openPackage,
            onGrantAccess = actions.grantMediaAccess,
            onPrevious = actions.previous,
            onPlayPause = actions.playPause,
            onNext = actions.next,
            modifier = m,
        )
    }
    val dock: @Composable (Modifier) -> Unit = { m ->
        Dock(
            items = dockItems,
            favorites = favorites,
            trailing = dockTrailing,
            onOpenFavorite = actions.openApp,
            onPickFavorite = actions.pickFavorite,
            onRemoveFavorite = actions.removeFavorite,
            modifier = m,
        )
    }

    BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 10.dp)) {
        val aspect = maxWidth / maxHeight
        val gap = 14.dp
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(gap)) {
            StatusBar(weather, network, Modifier.fillMaxWidth().height(28.dp))
            when {
                aspect >= 1.15f -> Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                    stage(Modifier.weight(if (aspect >= 1.95f) 1.45f else 1.1f).fillMaxHeight())
                    Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(gap)) {
                        live(Modifier.weight(1.2f).fillMaxWidth())
                        media(Modifier.weight(1f).fillMaxWidth())
                    }
                }
                else -> Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(gap)) {
                    stage(Modifier.weight(1.05f).fillMaxWidth())
                    live(Modifier.weight(1f).fillMaxWidth())
                    media(Modifier.height(190.dp).fillMaxWidth())
                }
            }
            dock(Modifier.fillMaxWidth().height(72.dp))
        }
    }
}

@Composable
private fun rememberNetworkState(): NetworkState {
    val context = LocalContext.current
    val state by produceState(NetworkState.OFFLINE) {
        val cm = context.getSystemService(android.net.ConnectivityManager::class.java)
        while (true) {
            value = runCatching {
                val caps = cm?.getNetworkCapabilities(cm.activeNetwork)
                when {
                    caps == null -> NetworkState.OFFLINE
                    caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) -> NetworkState.WIFI
                    else -> NetworkState.CELLULAR
                }
            }.getOrDefault(NetworkState.OFFLINE)
            kotlinx.coroutines.delay(10_000)
        }
    }
    return state
}
