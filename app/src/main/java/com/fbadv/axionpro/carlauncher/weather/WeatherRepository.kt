package com.fbadv.axionpro.carlauncher.weather

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

data class Weather(
    val city: String,
    val temperature: Double,
    val max: Double,
    val min: Double,
    val code: Int,
    val isDay: Boolean,
) {
    val description: String get() = describe(code)

    companion object {
        /** Códigos WMO usados pela Open-Meteo. */
        fun describe(code: Int): String = when (code) {
            0 -> "Céu limpo"
            1 -> "Poucas nuvens"
            2 -> "Parcialmente nublado"
            3 -> "Nublado"
            45, 48 -> "Neblina"
            51, 53, 55, 56, 57 -> "Garoa"
            61, 63, 65, 66, 67 -> "Chuva"
            71, 73, 75, 77 -> "Neve"
            80, 81, 82 -> "Pancadas de chuva"
            85, 86 -> "Neve"
            95, 96, 99 -> "Tempestade"
            else -> "—"
        }
    }
}

/**
 * Previsão do tempo pela Open-Meteo (gratuita, sem chave) a partir da
 * localização do GPS da central. Atualiza a cada 20 minutos.
 */
class WeatherRepository(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _weather = MutableStateFlow<Weather?>(null)
    val weather: StateFlow<Weather?> = _weather.asStateFlow()
    private val _location = MutableStateFlow<Location?>(null)
    val location: StateFlow<Location?> = _location.asStateFlow()
    private var job: Job? = null
    private var listening = false

    fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    fun start() {
        startLocationUpdates()
        if (job?.isActive == true) return
        job = scope.launch {
            while (isActive) {
                val loc = _location.value ?: lastKnown()
                if (loc != null) runCatching { _weather.value = fetch(loc) }
                delay(if (_weather.value == null) 60_000L else 20 * 60_000L)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        if (listening || !hasLocationPermission()) return
        val lm = context.getSystemService(LocationManager::class.java) ?: return
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                val first = _location.value == null
                _location.value = location
                if (first) scope.launch { runCatching { _weather.value = fetch(location) } }
            }
            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
            override fun onProviderEnabled(provider: String) = Unit
            override fun onProviderDisabled(provider: String) = Unit
        }
        for (p in listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)) {
            runCatching {
                if (lm.isProviderEnabled(p)) {
                    lm.requestLocationUpdates(p, 5_000L, 10f, listener, Looper.getMainLooper())
                    listening = true
                }
            }
        }
        lastKnown()?.let { _location.value = it }
    }

    @SuppressLint("MissingPermission")
    private fun lastKnown(): Location? {
        if (!hasLocationPermission()) return null
        val lm = context.getSystemService(LocationManager::class.java) ?: return null
        return lm.getProviders(true).mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }
    }

    private fun fetch(loc: Location): Weather {
        val url = URL(
            "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f".format(Locale.US, loc.latitude, loc.longitude) +
                "&current=temperature_2m,weather_code,is_day&daily=temperature_2m_max,temperature_2m_min&timezone=auto&forecast_days=1",
        )
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 10_000
        }
        val body = conn.inputStream.bufferedReader().use { it.readText() }
        val json = JSONObject(body)
        val cur = json.getJSONObject("current")
        val daily = json.getJSONObject("daily")
        return Weather(
            city = cityName(loc),
            temperature = cur.getDouble("temperature_2m"),
            max = daily.getJSONArray("temperature_2m_max").getDouble(0),
            min = daily.getJSONArray("temperature_2m_min").getDouble(0),
            code = cur.getInt("weather_code"),
            isDay = cur.optInt("is_day", 1) == 1,
        )
    }

    @Suppress("DEPRECATION")
    private fun cityName(loc: Location): String = runCatching {
        if (!Geocoder.isPresent()) return@runCatching null
        val a = Geocoder(context, Locale("pt", "BR")).getFromLocation(loc.latitude, loc.longitude, 1)?.firstOrNull()
        a?.locality ?: a?.subAdminArea ?: a?.adminArea
    }.getOrNull() ?: _weather.value?.city ?: "Local atual"
}
