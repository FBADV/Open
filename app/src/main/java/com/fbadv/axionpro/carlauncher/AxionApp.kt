package com.fbadv.axionpro.carlauncher

import android.app.Application
import com.fbadv.axionpro.carlauncher.data.AppRepository
import com.fbadv.axionpro.carlauncher.data.SettingsStore
import com.fbadv.axionpro.carlauncher.media.MediaRepository
import com.fbadv.axionpro.carlauncher.weather.WeatherRepository
import com.fbadv.axionpro.carlauncher.widget.WidgetHostManager
import org.osmdroid.config.Configuration

/** Ponto único de acesso aos repositórios do launcher. */
class AxionApp : Application() {
    lateinit var settings: SettingsStore
    lateinit var apps: AppRepository
    lateinit var media: MediaRepository
    lateinit var weather: WeatherRepository
    lateinit var widgets: WidgetHostManager
    lateinit var headlights: com.fbadv.axionpro.carlauncher.vehicle.HeadlightDetector

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
        headlights = com.fbadv.axionpro.carlauncher.vehicle.HeadlightDetector(this)
    }

    companion object {
        lateinit var instance: AxionApp
            private set

        /** Nulo em pré-visualizações/testes de tela, onde o app não é iniciado. */
        fun instanceOrNull(): AxionApp? = if (::instance.isInitialized) instance else null
    }
}
