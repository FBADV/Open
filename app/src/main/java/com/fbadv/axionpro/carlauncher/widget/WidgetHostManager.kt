package com.fbadv.axionpro.carlauncher.widget

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle

/**
 * Hospeda widgets de outros apps (Google Maps, Spotify, relógios, rádio da
 * central…) dentro do painel do launcher.
 */
class WidgetHostManager(private val context: Context) {
    val manager: AppWidgetManager = AppWidgetManager.getInstance(context)
    val host = AppWidgetHost(context, HOST_ID)

    fun startListening() = runCatching { host.startListening() }
    fun stopListening() = runCatching { host.stopListening() }

    fun providers(): List<AppWidgetProviderInfo> =
        manager.installedProviders.sortedBy { it.loadLabel(context.packageManager)?.lowercase() }

    fun allocate(): Int = host.allocateAppWidgetId()
    fun delete(id: Int) = runCatching { host.deleteAppWidgetId(id) }

    /** Tenta vincular direto; se o sistema exigir confirmação devolve o Intent a abrir. */
    fun bindOrIntent(id: Int, provider: ComponentName): Intent? {
        val ok = runCatching { manager.bindAppWidgetIdIfAllowed(id, provider) }.getOrDefault(false)
        if (ok) return null
        return Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider)
    }

    fun configureIntent(id: Int): Intent? {
        val info = manager.getAppWidgetInfo(id) ?: return null
        val cfg = info.configure ?: return null
        return Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE)
            .setComponent(cfg)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
    }

    fun info(id: Int): AppWidgetProviderInfo? = if (id < 0) null else manager.getAppWidgetInfo(id)

    fun createView(id: Int): AppWidgetHostView? {
        val info = info(id) ?: return null
        return host.createView(context.applicationContext, id, info).apply { setAppWidget(id, info) }
    }

    fun updateSize(view: AppWidgetHostView, widthDp: Int, heightDp: Int) {
        runCatching {
            val opts = Bundle().apply {
                putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, widthDp)
                putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, widthDp)
                putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, heightDp)
                putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, heightDp)
            }
            manager.updateAppWidgetOptions(view.appWidgetId, opts)
        }
    }

    companion object {
        const val HOST_ID = 0x4A47
    }
}
