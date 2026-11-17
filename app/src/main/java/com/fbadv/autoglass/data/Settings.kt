package com.fbadv.autoglass.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Ambientes (fundos) onde o veículo é exibido. */
enum class SceneMode(val label: String, val key: String) {
    RUA("Rua", "rua"),
    DUNAS("Dunas", "dunas"),
    ESTRADA("Estrada", "estrada"),
    LAMA("Lama", "lama"),
}

/** O que o painel interativo central mostra. */
enum class LivePanelMode(val label: String) {
    MAPA("Mapa"),
    VIDEO("YouTube"),
    WIDGET("Widget"),
}

/** Funções de atalho que o usuário associa a um app instalado. */
enum class AppRole(val label: String, val key: String) {
    NAVIGATION("Navegação", "nav"),
    MUSIC("Música", "music"),
    RADIO("Rádio", "radio"),
    PLAYER("Player da multimídia", "player"),
    VIDEO("Vídeo", "video"),
    PHONE("Telefone / Bluetooth", "phone"),
}

data class Settings(
    val scene: SceneMode = SceneMode.ESTRADA,
    val favorites: List<String> = emptyList(),
    val roles: Map<AppRole, String> = emptyMap(),
    val livePanel: LivePanelMode = LivePanelMode.MAPA,
    val widgetId: Int = -1,
    val freeformWindows: Boolean = false,
    val homeBubble: Boolean = false,
    val darkMap: Boolean = true,
    val clock24h: Boolean = true,
    val vehicleName: String = "Compass",
    val vehicleTrim: String = "Limited",
    val customVehiclePath: String? = null,
    val customScenes: Map<SceneMode, String> = emptyMap(),
    val mapZoom: Double = 16.0,
    val immersive: Boolean = true,
    val uiScale: Float = 1f,
)

/** Configurações persistidas em SharedPreferences, expostas como StateFlow. */
class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("autoglass", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(load())
    val state: StateFlow<Settings> = _state.asStateFlow()
    val value: Settings get() = _state.value

    fun update(block: (Settings) -> Settings) {
        _state.update(block)
        save(_state.value)
    }

    fun setFavorite(slot: Int, componentKey: String?) = update { s ->
        val list = MutableList(MAX_FAVORITES) { s.favorites.getOrNull(it).orEmpty() }
        list[slot] = componentKey.orEmpty()
        s.copy(favorites = list)
    }

    fun addFavorite(componentKey: String) = update { s ->
        val list = MutableList(MAX_FAVORITES) { s.favorites.getOrNull(it).orEmpty() }
        if (componentKey in list) return@update s
        val free = list.indexOfFirst { it.isEmpty() }
        list[if (free >= 0) free else MAX_FAVORITES - 1] = componentKey
        s.copy(favorites = list)
    }

    fun setRole(role: AppRole, componentKey: String?) = update { s ->
        val roles = s.roles.toMutableMap()
        if (componentKey == null) roles.remove(role) else roles[role] = componentKey
        s.copy(roles = roles)
    }

    private fun load(): Settings {
        val d = Settings()
        return Settings(
            scene = SceneMode.entries.firstOrNull { it.key == prefs.getString("scene", null) } ?: d.scene,
            favorites = prefs.getString("favorites", "")!!.split('|').let { l ->
                List(MAX_FAVORITES) { l.getOrNull(it).orEmpty() }
            },
            roles = AppRole.entries.mapNotNull { r ->
                prefs.getString("role_${r.key}", null)?.let { r to it }
            }.toMap(),
            livePanel = LivePanelMode.entries.firstOrNull { it.name == prefs.getString("livePanel", null) } ?: d.livePanel,
            widgetId = prefs.getInt("widgetId", -1),
            freeformWindows = prefs.getBoolean("freeform", false),
            homeBubble = prefs.getBoolean("homeBubble", false),
            darkMap = prefs.getBoolean("darkMap", true),
            clock24h = prefs.getBoolean("clock24h", true),
            vehicleName = prefs.getString("vehicleName", d.vehicleName)!!,
            vehicleTrim = prefs.getString("vehicleTrim", d.vehicleTrim)!!,
            customVehiclePath = prefs.getString("customVehicle", null),
            customScenes = SceneMode.entries.mapNotNull { m ->
                prefs.getString("scene_custom_${m.key}", null)?.let { m to it }
            }.toMap(),
            mapZoom = prefs.getFloat("mapZoom", 16f).toDouble(),
            immersive = prefs.getBoolean("immersive", true),
            uiScale = prefs.getFloat("uiScale", 1f),
        )
    }

    private fun save(s: Settings) {
        prefs.edit().apply {
            putString("scene", s.scene.key)
            putString("favorites", s.favorites.joinToString("|"))
            AppRole.entries.forEach { r ->
                val v = s.roles[r]
                if (v == null) remove("role_${r.key}") else putString("role_${r.key}", v)
            }
            putString("livePanel", s.livePanel.name)
            putInt("widgetId", s.widgetId)
            putBoolean("freeform", s.freeformWindows)
            putBoolean("homeBubble", s.homeBubble)
            putBoolean("darkMap", s.darkMap)
            putBoolean("clock24h", s.clock24h)
            putString("vehicleName", s.vehicleName)
            putString("vehicleTrim", s.vehicleTrim)
            if (s.customVehiclePath == null) remove("customVehicle") else putString("customVehicle", s.customVehiclePath)
            SceneMode.entries.forEach { m ->
                val v = s.customScenes[m]
                if (v == null) remove("scene_custom_${m.key}") else putString("scene_custom_${m.key}", v)
            }
            putFloat("mapZoom", s.mapZoom.toFloat())
            putBoolean("immersive", s.immersive)
            putFloat("uiScale", s.uiScale)
        }.apply()
    }

    companion object {
        const val MAX_FAVORITES = 4
    }
}
