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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    val sceneBmp by produceState<android.graphics.Bitmap?>(null, settings.scene, customScene) {
        value = withContext(Dispatchers.IO) { Images.loadScene(context, settings.scene, customScene) }
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

    LauncherSurface(blurred, settings.uiScale) {
                AnimatedContent(
                    targetState = screen.value,
                    transitionSpec = { (fadeIn(tween(250)) + scaleIn(tween(250), initialScale = 0.97f)) togetherWith fadeOut(tween(150)) },
                    label = "screen",
                ) { s ->
                    when (s) {
                        Screen.HOME -> Dashboard(
                            settings = settings,
                            scene = scene,
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
fun LauncherSurface(blurred: ImageBitmap?, uiScale: Float, content: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize().background(Color(0xFF060A14))) {
        val wPx = constraints.maxWidth
        val hPx = constraints.maxHeight
        val density = (min(wPx, hPx) / DESIGN_SHORT_SIDE * uiScale).coerceIn(0.6f, 4f)
        val backdrop = blurred?.let { Backdrop(it, IntSize(wPx, hPx)) }
        CompositionLocalProvider(
            LocalDensity provides Density(density, 1f),
            LocalBackdrop provides backdrop,
        ) {
            Box(Modifier.fillMaxSize()) {
                blurred?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                Box(Modifier.fillMaxSize().background(Color(0x70050914)))
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
    scene: ImageBitmap?,
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
    val onOpen = actions.openApp
    val onOpenRole = actions.openRole
    val onPickRole = actions.pickRole
    val onPickFavorite = actions.pickFavorite
    val onPickWidget = actions.pickWidget
    val onScreen = actions.goTo
    val railItems = listOf(
        RailItem(HomeIcon, "Início", null, active = true, onClick = {}),
        *listOf(AppRole.NAVIGATION, AppRole.MUSIC, AppRole.RADIO, AppRole.PHONE, AppRole.VIDEO).map { r ->
            RailItem(roleIcon(r), r.label, r, onClick = { onOpenRole(r, null) }, onLongClick = { onPickRole(r) })
        }.toTypedArray(),
    )
    val railBottom = listOf(
        RailItem(AppsIcon, "Apps", null, onClick = { onScreen(Screen.APPS) }),
        RailItem(SettingsIcon, "Configurações", null, onClick = { onScreen(Screen.SETTINGS) }),
    )

    val hero: @Composable (Modifier) -> Unit = { m ->
        VehicleHero(
            scene = scene,
            sceneMode = settings.scene,
            vehicle = vehicle,
            vehicleName = settings.vehicleName,
            vehicleTrim = settings.vehicleTrim,
            weather = weather,
            clock24h = settings.clock24h,
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
                    LivePanelMode.MAPA -> onOpenRole(AppRole.NAVIGATION, bounds)
                    LivePanelMode.VIDEO -> onOpenRole(AppRole.VIDEO, bounds)
                    LivePanelMode.WIDGET -> actions.openWidgetApp()
                }
            },
            onPickWidget = onPickWidget,
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
            onOpenApp = onOpen,
            onOpenSource = actions.openPackage,
            onGrantAccess = actions.grantMediaAccess,
            onPrevious = actions.previous,
            onPlayPause = actions.playPause,
            onNext = actions.next,
            modifier = m,
        )
    }
    val favs: @Composable (Modifier, Int) -> Unit = { m, cols ->
        FavoritesCard(
            favorites = favorites,
            columns = cols,
            onOpen = onOpen,
            onPick = onPickFavorite,
            onRemove = actions.removeFavorite,
            modifier = m,
        )
    }

    BoxWithConstraints(Modifier.fillMaxSize().padding(14.dp)) {
        val aspect = maxWidth / maxHeight
        val gap = 14.dp
        when {
            // telas largas (ex.: 1920x720, 1280x480) — 3 colunas, como no CarPlay
            aspect >= 1.95f -> Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                SideRail(railItems, railBottom, vertical = true, modifier = Modifier.width(72.dp).fillMaxHeight())
                hero(Modifier.weight(1.3f).fillMaxHeight())
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(gap)) {
                    live(Modifier.weight(1.25f).fillMaxWidth())
                    media(Modifier.weight(1f).fillMaxWidth())
                }
                favs(Modifier.weight(0.42f).fillMaxHeight(), 1)
            }
            // 16:9 / 16:10 (ex.: 1024x600, 1280x720, 1920x1080, 2000x1200)
            aspect >= 1.15f -> Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                SideRail(railItems, railBottom, vertical = true, modifier = Modifier.width(72.dp).fillMaxHeight())
                Column(Modifier.weight(1.12f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(gap)) {
                    hero(Modifier.weight(1.3f).fillMaxWidth())
                    Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                        media(Modifier.weight(1.6f).fillMaxHeight())
                        favs(Modifier.weight(1f).fillMaxHeight(), 2)
                    }
                }
                live(Modifier.weight(1f).fillMaxHeight())
            }
            // telas em pé (estilo Tesla, 10"–15" verticais)
            else -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(gap)) {
                hero(Modifier.weight(1f).fillMaxWidth())
                live(Modifier.weight(1.15f).fillMaxWidth())
                media(Modifier.height(200.dp).fillMaxWidth())
                favs(Modifier.height(130.dp).fillMaxWidth(), 4)
                SideRail(railItems, railBottom, vertical = false, modifier = Modifier.fillMaxWidth().height(76.dp))
            }
        }
    }
}
