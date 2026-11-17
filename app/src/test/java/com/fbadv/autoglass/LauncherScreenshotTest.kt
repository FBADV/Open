package com.fbadv.autoglass

import android.graphics.BitmapFactory
import android.os.SystemClock
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalInspectionMode
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import com.android.resources.ScreenOrientation
import com.fbadv.autoglass.data.AppEntry
import com.fbadv.autoglass.data.LivePanelMode
import com.fbadv.autoglass.data.SceneMode
import com.fbadv.autoglass.data.Settings
import com.fbadv.autoglass.media.NowPlaying
import com.fbadv.autoglass.ui.Dashboard
import com.fbadv.autoglass.ui.DashboardActions
import com.fbadv.autoglass.ui.Images
import com.fbadv.autoglass.ui.LauncherSurface
import com.fbadv.autoglass.ui.NetworkState
import com.fbadv.autoglass.ui.theme.AutoGlassTheme
import com.fbadv.autoglass.weather.Weather
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Gera imagens do launcher em telas típicas de centrais multimídia
 * (9" a 15", paisagem e retrato) para conferir o layout sem dispositivo.
 * Rode: ./gradlew :app:recordPaparazziDebug
 */
@RunWith(Parameterized::class)
class LauncherScreenshotTest(private val case: Case) {

    data class Case(val name: String, val w: Int, val h: Int, val scene: SceneMode, val live: LivePanelMode = LivePanelMode.MAPA, val day: Boolean = false) {
        override fun toString() = name
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun cases() = listOf(
            Case("9pol_1024x600_estudio", 1024, 600, SceneMode.ESTUDIO),
            Case("10pol_1280x720_dunas_dia", 1280, 720, SceneMode.DUNAS, day = true),
            Case("10pol_1920x720_rua_wide", 1920, 720, SceneMode.RUA),
            Case("12pol_1920x1080_lama", 1920, 1080, SceneMode.LAMA, LivePanelMode.VIDEO),
            Case("13pol_2000x1200_estrada_dia", 2000, 1200, SceneMode.ESTRADA, day = true),
            Case("10pol_1280x720_estudio_dia", 1280, 720, SceneMode.ESTUDIO, day = true),
            Case("10pol_1920x720_estudio_wide", 1920, 720, SceneMode.ESTUDIO),
            Case("15pol_vertical_1080x1920_dunas", 1080, 1920, SceneMode.DUNAS),
        )
    }

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.NEXUS_7.copy(
            screenWidth = case.w,
            screenHeight = case.h,
            xdpi = 160,
            ydpi = 160,
            density = Density.MEDIUM,
            orientation = if (case.w > case.h) ScreenOrientation.LANDSCAPE else ScreenOrientation.PORTRAIT,
            softButtons = false,
        ),
        showSystemUi = false,
        maxPercentDifference = 1.0,
    )

    @Test
    fun dashboard() {
        val res = paparazzi.context.resources
        val sceneBmp = BitmapFactory.decodeResource(res, Images.sceneRes(case.scene, case.day))
        val vehicle = BitmapFactory.decodeResource(res, R.drawable.vehicle_compass_limited).asImageBitmap()
        val blurred = Images.blurred(sceneBmp).asImageBitmap()
        val fav = listOf(
            AppEntry("Spotify", "com.spotify.music", "a"),
            AppEntry("YouTube Music", "com.google.android.apps.youtube.music", "b"),
            AppEntry("Waze", "com.waze", "c"),
            null,
        )
        paparazzi.snapshot {
            CompositionLocalProvider(LocalInspectionMode provides true) {
            AutoGlassTheme {
                LauncherSurface(sceneBmp.asImageBitmap(), blurred, 1f) {
                    Dashboard(
                        settings = Settings(scene = case.scene, livePanel = case.live),
                        network = NetworkState.WIFI,
                        night = !case.day,
                        vehicle = vehicle,
                        weather = Weather("Natal", 29.4, 31.0, 24.0, 1, true),
                        nowPlaying = NowPlaying(
                            "com.spotify.music", "Estrada Afora", "Trilha AutoGlass", "", null,
                            true, 215_000, 92_000, SystemClock.elapsedRealtime(), 1f,
                        ),
                        hasMediaAccess = true,
                        favorites = fav,
                        radio = AppEntry("Rádio", "com.syu.radio", "r"),
                        player = AppEntry("Música", "com.syu.music", "m"),
                        shortcuts = listOf(
                            AppEntry("Spotify", "com.spotify.music", "a"),
                            AppEntry("Deezer", "deezer.android.app", "d"),
                            AppEntry("Amazon Music", "com.amazon.mp3", "e"),
                        ),
                        actions = DashboardActions.NONE,
                    )
                }
            }
            }
        }
    }
}
