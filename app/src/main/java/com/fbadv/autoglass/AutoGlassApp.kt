package com.fbadv.autoglass

import android.app.Application
import com.fbadv.autoglass.data.AppRepository
import com.fbadv.autoglass.data.SettingsStore
import com.fbadv.autoglass.media.MediaRepository
import com.fbadv.autoglass.weather.WeatherRepository
import com.fbadv.autoglass.widget.WidgetHostManager
import org.osmdroid.config.Configuration

/** Ponto único de acesso aos repositórios do launcher. */
class AutoGlassApp : Application() {
    lateinit var settings: SettingsStore
    lateinit var apps: AppRepository
    lateinit var media: MediaRepository
    lateinit var weather: WeatherRepository
    lateinit var widgets: WidgetHostManager
    lateinit var headlights: com.fbadv.autoglass.vehicle.HeadlightDetector

    override fun onCreate() {
        super.onCreate()
        instance = this
        Configuration.getInstance().apply {
            userAgentValue = packageName
            osmdroidBasePath = filesDir.resolve("osmdroid")
            osmdroidTileCache = cacheDir.resolve("osmdroid-tiles")
        }
        settings = SettingsStore(this)
        apps = AppRepository(this)
        media = MediaRepository(this)
        weather = WeatherRepository(this)
        widgets = WidgetHostManager(this)
        headlights = com.fbadv.autoglass.vehicle.HeadlightDetector(this)
    }

    companion object {
        lateinit var instance: AutoGlassApp
            private set

        /** Nulo em pré-visualizações/testes de tela, onde o app não é iniciado. */
        fun instanceOrNull(): AutoGlassApp? = if (::instance.isInitialized) instance else null
    }
}
