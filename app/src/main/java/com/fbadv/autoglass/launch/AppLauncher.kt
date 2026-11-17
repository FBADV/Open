package com.fbadv.autoglass.launch

import android.app.ActivityOptions
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import com.fbadv.autoglass.data.AppEntry

/** Abre apps em tela cheia ou, se a central suportar, numa janela sobre o painel. */
object AppLauncher {

    fun launch(context: Context, entry: AppEntry, bounds: Rect? = null) {
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(entry.component)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        start(context, intent, bounds)
    }

    fun launchPackage(context: Context, pkg: String, bounds: Rect? = null): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(pkg) ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        return start(context, intent, bounds)
    }

    fun start(context: Context, intent: Intent, bounds: Rect? = null): Boolean {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            if (bounds != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                context.startActivity(intent, freeformOptions(bounds).toBundle())
            } else {
                context.startActivity(intent)
            }
            true
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "App não encontrado", Toast.LENGTH_SHORT).show()
            false
        } catch (e: SecurityException) {
            Toast.makeText(context, "Sem permissão para abrir este app", Toast.LENGTH_SHORT).show()
            false
        }
    }

    /**
     * Pede ao sistema para abrir a atividade dentro de [bounds] (modo janela /
     * freeform). Funciona nas centrais com "janelas de formato livre" ativado;
     * nas demais o Android simplesmente abre em tela cheia.
     */
    private fun freeformOptions(bounds: Rect): ActivityOptions {
        val opts = ActivityOptions.makeBasic()
        opts.setLaunchBounds(bounds)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            runCatching {
                // WINDOWING_MODE_FREEFORM = 5 (API oculta; ignorada se não houver suporte)
                ActivityOptions::class.java.getMethod("setLaunchWindowingMode", Int::class.javaPrimitiveType)
                    .invoke(opts, 5)
            }
        }
        return opts
    }

    /** Navega até um destino no app de navegação escolhido. */
    fun navigate(context: Context, pkg: String?, query: String) {
        val uri = when (pkg) {
            "com.waze" -> Uri.parse("https://waze.com/ul?q=${Uri.encode(query)}&navigate=yes")
            else -> Uri.parse("google.navigation:q=${Uri.encode(query)}")
        }
        val i = Intent(Intent.ACTION_VIEW, uri)
        if (pkg != null) i.setPackage(pkg)
        if (!start(context, i)) start(context, Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode(query)}")))
    }

    fun openAppInfo(context: Context, pkg: String) {
        start(context, Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$pkg")))
    }

    fun openSystemSettings(context: Context) = start(context, Intent(Settings.ACTION_SETTINGS))

    fun openHomeSettings(context: Context) {
        if (!start(context, Intent(Settings.ACTION_HOME_SETTINGS))) openSystemSettings(context)
    }

    fun openOverlaySettings(context: Context) {
        start(context, Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")))
    }
}
