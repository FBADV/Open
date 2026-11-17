package com.fbadv.autoglass.vehicle

import android.content.ComponentCallbacks
import android.content.Context
import android.content.res.Configuration
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Estado dos faróis: [on] nulo quando a central não informa. */
data class HeadlightState(val on: Boolean?, val source: String)

/**
 * Detecta farol aceso/apagado nas centrais Android.
 * Não há API padrão para o fio de iluminação (ILL), então combina:
 *  1. chaves de configuração do sistema usadas por fabricantes de centrais
 *     (procura nomes como "ill", "headlight", "lamp", "day_night"...);
 *  2. o modo noite do Android, que muitas centrais ligam junto com o farol.
 * Sem nenhuma das duas, devolve [HeadlightState.on] = null e o launcher usa o
 * horário (nascer/pôr do sol) como reserva.
 */
class HeadlightDetector(private val context: Context) {
    private val _state = MutableStateFlow(HeadlightState(null, "Não detectado"))
    val state: StateFlow<HeadlightState> = _state.asStateFlow()
    private val main = Handler(Looper.getMainLooper())

    private val keyPattern = Regex(
        "(^|[_.])(ill|illumination|illum|headlight|head_light|headlamp|head_lamp|car_lamp|lamp_state|small_light|smalllight|light_state|night_mode|daynight|day_night|backlight_night|car_ill)([_.]|$)",
        RegexOption.IGNORE_CASE,
    )

    init {
        val observer = object : ContentObserver(main) {
            override fun onChange(selfChange: Boolean, uri: Uri?) = evaluate()
        }
        runCatching { context.contentResolver.registerContentObserver(Settings.System.CONTENT_URI, true, observer) }
        runCatching { context.contentResolver.registerContentObserver(Settings.Global.CONTENT_URI, true, observer) }
        context.registerComponentCallbacks(object : ComponentCallbacks {
            override fun onConfigurationChanged(newConfig: Configuration) = evaluate()
            @Deprecated("Deprecated in Java")
            override fun onLowMemory() = Unit
        })
        evaluate()
    }

    fun evaluate() {
        val fromKey = scan(Settings.System.CONTENT_URI, "Sistema") ?: scan(Settings.Global.CONTENT_URI, "Global")
        if (fromKey != null) { _state.value = fromKey; return }
        val night = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        _state.value = if (night) HeadlightState(true, "Modo noite do sistema (farol aceso)")
        else HeadlightState(null, "Não informado pela central — usando o horário")
    }

    private fun scan(uri: Uri, label: String): HeadlightState? = runCatching {
        context.contentResolver.query(uri, arrayOf("name", "value"), null, null, null)?.use { c ->
            while (c.moveToNext()) {
                val name = c.getString(0) ?: continue
                if (!keyPattern.containsMatchIn(name)) continue
                val v = c.getString(1)?.trim()?.lowercase() ?: continue
                val on = when (v) { "1", "true", "on", "night", "yes" -> true; "0", "false", "off", "day", "no" -> false; else -> null } ?: continue
                return@runCatching HeadlightState(on, "$label: $name")
            }
            null
        }
    }.getOrNull()
}
