package com.fbadv.autoglass.data

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

data class AppEntry(
    val label: String,
    val packageName: String,
    val className: String,
) {
    /** Chave persistida: "pacote/classe". */
    val key: String get() = "$packageName/$className"
    val component: ComponentName get() = ComponentName(packageName, className)
}

/** Lista os apps instalados que podem ser abertos e mantém a lista atualizada. */
class AppRepository(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val pm: PackageManager = context.packageManager
    private val iconCache = ConcurrentHashMap<String, ImageBitmap>()
    private val _apps = MutableStateFlow<List<AppEntry>>(emptyList())
    val apps: StateFlow<List<AppEntry>> = _apps.asStateFlow()

    init {
        refresh()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(context, object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) {
                i.data?.schemeSpecificPart?.let { pkg -> iconCache.keys.removeAll { it.startsWith("$pkg/") } }
                refresh()
            }
        }, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    fun refresh() {
        scope.launch {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val list = pm.queryIntentActivities(intent, 0)
                .filter { it.activityInfo.packageName != context.packageName }
                .map {
                    AppEntry(
                        label = it.loadLabel(pm).toString().trim(),
                        packageName = it.activityInfo.packageName,
                        className = it.activityInfo.name,
                    )
                }
                .distinctBy { it.key }
                .sortedBy { it.label.lowercase() }
            _apps.value = list
        }
    }

    fun find(key: String?): AppEntry? {
        if (key.isNullOrBlank()) return null
        val list = _apps.value
        return list.firstOrNull { it.key == key }
            ?: list.firstOrNull { it.packageName == key.substringBefore('/') }
    }

    fun findPackage(pkg: String): AppEntry? = _apps.value.firstOrNull { it.packageName == pkg }

    fun isInstalled(pkg: String): Boolean = try {
        pm.getApplicationInfo(pkg, 0); true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    /** Ícone do app como ImageBitmap (com cache). */
    fun icon(entry: AppEntry, sizePx: Int = 160): ImageBitmap? = iconCache[entry.key] ?: runCatching {
        val d: Drawable = try {
            pm.getActivityIcon(entry.component)
        } catch (_: Exception) {
            pm.getApplicationIcon(entry.packageName)
        }
        d.toBitmap(sizePx, sizePx).asImageBitmap().also { iconCache[entry.key] = it }
    }.getOrNull()

    fun iconForPackage(pkg: String, sizePx: Int = 160): ImageBitmap? =
        findPackage(pkg)?.let { icon(it, sizePx) } ?: runCatching {
            pm.getApplicationIcon(pkg).toBitmap(sizePx, sizePx).asImageBitmap()
        }.getOrNull()

    /**
     * Resolve o app associado a uma função: primeiro o escolhido pelo usuário,
     * depois pacotes conhecidos, depois pelo nome visível.
     */
    fun resolveRole(role: AppRole, chosen: String?): AppEntry? {
        find(chosen)?.let { return it }
        val list = _apps.value
        KnownApps.packagesFor(role).forEach { pkg ->
            list.firstOrNull { it.packageName == pkg }?.let { return it }
        }
        val regex = KnownApps.labelRegexFor(role) ?: return null
        return list.firstOrNull { regex.containsMatchIn(it.label) }
    }

    /** Apps instalados que se encaixam numa função (para sugerir na escolha). */
    fun candidates(role: AppRole): List<AppEntry> {
        val list = _apps.value
        val known = KnownApps.packagesFor(role)
        val regex = KnownApps.labelRegexFor(role)
        return list.filter { e -> e.packageName in known || (regex?.containsMatchIn(e.label) == true) }
    }
}
