package com.fbadv.autoglass.media

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.view.KeyEvent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NowPlaying(
    val packageName: String,
    val title: String,
    val artist: String,
    val album: String,
    val art: ImageBitmap?,
    val isPlaying: Boolean,
    val durationMs: Long,
    val positionMs: Long,
    val positionUpdatedAt: Long,
    val speed: Float,
    /** Rádio (sem capa de álbum: o cartão mostra a frequência). */
    val isRadio: Boolean = false,
    /** Veio de notificação/aviso (sem sessão): comandos vão como teclas de mídia. */
    val fromFallback: Boolean = false,
    val updatedAt: Long = SystemClock.elapsedRealtime(),
) {
    fun currentPosition(now: Long = SystemClock.elapsedRealtime()): Long {
        if (!isPlaying) return positionMs
        val p = positionMs + ((now - positionUpdatedAt) * speed).toLong()
        return if (durationMs > 0) p.coerceIn(0, durationMs) else p.coerceAtLeast(0)
    }
}

/**
 * Observa as sessões de mídia do sistema e expõe "o que está tocando".
 * Sem a permissão de notificações os comandos continuam funcionando
 * via teclas de mídia (AudioManager.dispatchMediaKeyEvent).
 */
class MediaRepository(private val context: Context) {
    private val main = Handler(Looper.getMainLooper())
    private val msm = context.getSystemService(MediaSessionManager::class.java)
    private val audio = context.getSystemService(AudioManager::class.java)
    private val listenerComponent = ComponentName(context, MediaListenerService::class.java)

    private val _nowPlaying = MutableStateFlow<NowPlaying?>(null)
    val nowPlaying: StateFlow<NowPlaying?> = _nowPlaying.asStateFlow()
    private val _hasAccess = MutableStateFlow(hasNotificationAccess())
    val hasAccess: StateFlow<Boolean> = _hasAccess.asStateFlow()

    private var controller: MediaController? = null
    private var listening = false
    /** Últimos dados vindos de notificação/aviso de rádio ou player sem sessão. */
    private var fallback: NowPlaying? = null
    private var sessionNow: NowPlaying? = null
    private val io = java.util.concurrent.Executors.newSingleThreadExecutor()

    init {
        // avisos de "música mudou" usados por players nativos e de centrais
        val f = android.content.IntentFilter()
        listOf(
            "com.android.music.metachanged", "com.android.music.playstatechanged",
            "com.android.music.playbackcomplete", "com.htc.music.metachanged",
            "com.sec.android.app.music.metachanged", "com.miui.player.metachanged",
            "com.andrew.apollo.metachanged", "com.syu.music.metachanged",
            "com.microntek.music.metachanged", "com.ts.music.metachanged",
            "fm.last.android.metachanged", "com.real.IMP.metachanged",
        ).forEach { f.addAction(it) }
        androidx.core.content.ContextCompat.registerReceiver(context, object : android.content.BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) = onMetaBroadcast(i)
        }, f, androidx.core.content.ContextCompat.RECEIVER_EXPORTED)
    }

    private fun isRadioPackage(pkg: String): Boolean {
        val app = runCatching { com.fbadv.autoglass.AutoGlassApp.instance }.getOrNull()
        val chosen = app?.settings?.value?.roles?.get(com.fbadv.autoglass.data.AppRole.RADIO)?.substringBefore('/')
        return pkg == chosen || pkg in com.fbadv.autoglass.data.KnownApps.RADIO ||
            Regex("radio|fmradio|\\.fm\\b|tuner", RegexOption.IGNORE_CASE).containsMatchIn(pkg)
    }

    private fun isPlayerPackage(pkg: String): Boolean {
        val app = runCatching { com.fbadv.autoglass.AutoGlassApp.instance }.getOrNull()
        val chosen = app?.settings?.value?.roles?.get(com.fbadv.autoglass.data.AppRole.PLAYER)?.substringBefore('/')
        return pkg == chosen || pkg in com.fbadv.autoglass.data.KnownApps.PLAYER
    }

    /** Notificação postada/removida (chamado pelo serviço de notificações). */
    fun onNotification(sbn: android.service.notification.StatusBarNotification, posted: Boolean) {
        val pkg = sbn.packageName ?: return
        if (pkg == context.packageName) return
        val n = sbn.notification ?: return
        val extras = n.extras
        if (!posted) {
            if (fallback?.packageName == pkg) { fallback = null; publishCombined() }
            return
        }
        // se a notificação traz o token da sessão, controla o app diretamente
        val token: android.media.session.MediaSession.Token? = runCatching {
            @Suppress("DEPRECATION") extras.getParcelable<android.media.session.MediaSession.Token>(android.app.Notification.EXTRA_MEDIA_SESSION)
        }.getOrNull()
        if (token != null) {
            if (controller?.sessionToken != token) {
                runCatching {
                    val c = MediaController(context, token)
                    if (controller == null || controller?.playbackState?.state != PlaybackState.STATE_PLAYING ||
                        c.playbackState?.state == PlaybackState.STATE_PLAYING) {
                        controller?.unregisterCallback(callback)
                        controller = c
                        c.registerCallback(callback, main)
                    }
                }
            }
            publish(); return
        }
        val template = extras.getString(android.app.Notification.EXTRA_TEMPLATE).orEmpty()
        val mediaLike = isRadioPackage(pkg) || isPlayerPackage(pkg) ||
            n.category == android.app.Notification.CATEGORY_TRANSPORT || template.contains("MediaStyle")
        if (!mediaLike) return
        val title = extras.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = (extras.getCharSequence(android.app.Notification.EXTRA_TEXT)
            ?: extras.getCharSequence(android.app.Notification.EXTRA_SUB_TEXT))?.toString().orEmpty()
        if (title.isBlank() && text.isBlank()) return
        val large: Bitmap? = runCatching {
            (extras.get(android.app.Notification.EXTRA_LARGE_ICON) as? Bitmap)
                ?: (extras.get(android.app.Notification.EXTRA_LARGE_ICON) as? android.graphics.drawable.Icon)
                    ?.loadDrawable(context)?.let { d -> d.toBitmap(300, 300) }
        }.getOrNull()
        val ongoing = (n.flags and android.app.Notification.FLAG_ONGOING_EVENT) != 0
        fallback = NowPlaying(
            packageName = pkg, title = title, artist = text, album = "",
            art = large?.let { runCatching { it.asImageBitmap() }.getOrNull() },
            isPlaying = ongoing || isRadioPackage(pkg), durationMs = 0, positionMs = 0,
            positionUpdatedAt = SystemClock.elapsedRealtime(), speed = 1f,
            isRadio = isRadioPackage(pkg), fromFallback = true,
        )
        publishCombined()
    }

    private fun onMetaBroadcast(i: Intent) {
        val title = i.getStringExtra("track") ?: i.getStringExtra("title") ?: return
        val artist = i.getStringExtra("artist").orEmpty()
        val playing = if (i.hasExtra("playing")) i.getBooleanExtra("playing", true) else true
        val pkg = i.getStringExtra("package") ?: i.action?.substringBeforeLast('.')?.takeIf { it.isNotBlank() } ?: "player"
        fallback = NowPlaying(
            packageName = pkg, title = title, artist = artist, album = i.getStringExtra("album").orEmpty(), art = null,
            isPlaying = playing, durationMs = i.getLongExtra("duration", 0L), positionMs = i.getLongExtra("position", 0L),
            positionUpdatedAt = SystemClock.elapsedRealtime(), speed = 1f, isRadio = isRadioPackage(pkg), fromFallback = true,
        )
        publishCombined()
    }

    /** Escolhe o que mostrar: a sessão que toca; senão o rádio/player de fábrica. */
    private fun publishCombined() {
        val s = sessionNow
        val f = fallback
        val chosen = when {
            s != null && s.isPlaying -> s
            f != null && (f.isPlaying || s == null) -> f
            else -> s ?: f
        }
        if (chosen != null && chosen.art == null && chosen.title.isNotBlank()) enrichArtwork(chosen)
        _nowPlaying.value = chosen
    }

    /** Capa automática para músicas sem capa (player da central e rádio com RDS "Artista - Música"). */
    private fun enrichArtwork(np: NowPlaying) {
        var artist = np.artist; var title = np.title
        if (np.isRadio) {
            val rds = listOf(np.artist, np.title).firstOrNull { it.contains(" - ") } ?: return
            artist = rds.substringBefore(" - "); title = rds.substringAfter(" - ")
        }
        io.execute {
            val bmp = ArtworkFetcher.lookup(artist, title) ?: return@execute
            main.post {
                val cur = _nowPlaying.value
                if (cur != null && cur.title == np.title && cur.packageName == np.packageName && cur.art == null) {
                    val withArt = cur.copy(art = bmp.asImageBitmap())
                    if (cur.fromFallback) fallback = withArt else sessionNow = withArt
                    _nowPlaying.value = withArt
                }
            }
        }
    }

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { list ->
        pickController(list.orEmpty())
    }

    private val callback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = publish()
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            // Se outro app começou a tocar, troca para ele.
            refreshSessions()
            publish()
        }
        override fun onSessionDestroyed() = refreshSessions()
    }

    fun hasNotificationAccess(): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

    /** Liga a escuta das sessões, se o usuário já concedeu o acesso. */
    fun attach() {
        _hasAccess.value = hasNotificationAccess()
        if (!_hasAccess.value) return
        try {
            if (!listening) {
                msm.addOnActiveSessionsChangedListener(sessionsListener, listenerComponent, main)
                listening = true
            }
            refreshSessions()
        } catch (_: SecurityException) {
            _hasAccess.value = false
        }
    }

    private fun refreshSessions() {
        try {
            pickController(msm.getActiveSessions(listenerComponent))
        } catch (_: SecurityException) {
            _hasAccess.value = false
        }
    }

    private fun pickController(list: List<MediaController>) {
        val best = list.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: list.firstOrNull { it.packageName == controller?.packageName }
            ?: list.firstOrNull()
        if (best?.sessionToken != controller?.sessionToken) {
            controller?.unregisterCallback(callback)
            controller = best
            best?.registerCallback(callback, main)
        }
        publish()
    }

    private fun publish() {
        val c = controller
        if (c == null) {
            sessionNow = null
            publishCombined()
            return
        }
        val md = c.metadata
        val st = c.playbackState
        val art: Bitmap? = md?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: md?.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: md?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
        val previous = sessionNow
        sessionNow = NowPlaying(
            packageName = c.packageName,
            title = md?.getString(MediaMetadata.METADATA_KEY_TITLE)
                ?: md?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE).orEmpty(),
            artist = md?.getString(MediaMetadata.METADATA_KEY_ARTIST)
                ?: md?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
                ?: md?.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE).orEmpty(),
            album = md?.getString(MediaMetadata.METADATA_KEY_ALBUM).orEmpty(),
            art = when {
                art == null -> null
                previous?.art != null && previous.title == md?.getString(MediaMetadata.METADATA_KEY_TITLE) &&
                    previous.packageName == c.packageName -> previous.art
                else -> runCatching { art.asImageBitmap() }.getOrNull()
            },
            isPlaying = st?.state == PlaybackState.STATE_PLAYING || st?.state == PlaybackState.STATE_BUFFERING,
            durationMs = md?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L,
            positionMs = st?.position ?: 0L,
            positionUpdatedAt = st?.lastPositionUpdateTime?.takeIf { it > 0 } ?: SystemClock.elapsedRealtime(),
            speed = st?.playbackSpeed?.takeIf { it > 0f } ?: 1f,
            isRadio = isRadioPackage(c.packageName),
        )
        publishCombined()
    }

    /**
     * Tocar/pausar. Com uma sessão ativa usa os controles dela; sem nenhuma,
     * envia "play" (o Android retoma o último app de música) e, se nada
     * começar a tocar, abre o app de música escolhido (YouTube Music).
     */
    /** Se o que aparece é o rádio/player sem sessão, comanda por teclas de mídia. */
    private val showingFallback get() = _nowPlaying.value?.fromFallback == true

    fun playPause() {
        val c = if (showingFallback) null else controller
        if (showingFallback) { sendKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE); return }
        if (c != null) {
            val playing = c.playbackState?.state == PlaybackState.STATE_PLAYING ||
                c.playbackState?.state == PlaybackState.STATE_BUFFERING
            if (playing) command(c, PlaybackState.ACTION_PAUSE, KeyEvent.KEYCODE_MEDIA_PAUSE) { it.pause() }
            else command(c, PlaybackState.ACTION_PLAY, KeyEvent.KEYCODE_MEDIA_PLAY) { it.play() }
            return
        }
        sendKey(KeyEvent.KEYCODE_MEDIA_PLAY)
        main.postDelayed({
            refreshSessions()
            if (_nowPlaying.value?.isPlaying != true) onNothingToPlay?.invoke()
        }, 1500)
    }

    fun next() {
        val c = (if (showingFallback) null else controller) ?: return sendKey(KeyEvent.KEYCODE_MEDIA_NEXT)
        command(c, PlaybackState.ACTION_SKIP_TO_NEXT, KeyEvent.KEYCODE_MEDIA_NEXT) { it.skipToNext() }
    }

    fun previous() {
        val c = (if (showingFallback) null else controller) ?: return sendKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
        command(c, PlaybackState.ACTION_SKIP_TO_PREVIOUS, KeyEvent.KEYCODE_MEDIA_PREVIOUS) { it.skipToPrevious() }
    }

    fun seekTo(ms: Long) {
        val c = controller ?: return
        c.transportControls.seekTo(ms)
        main.postDelayed({ publish() }, 300)
    }

    /** Chamado quando o usuário pede "play" e não há nada para retomar. */
    var onNothingToPlay: (() -> Unit)? = null

    /**
     * Usa o comando do app se ele declara suportá-lo; caso contrário envia a
     * tecla de mídia direto para a sessão do app (alguns apps só tratam teclas).
     */
    private fun command(c: MediaController, action: Long, key: Int, tc: (MediaController.TransportControls) -> Unit) {
        val actions = c.playbackState?.actions ?: 0L
        val ok = runCatching {
            if (actions and action != 0L || actions == 0L) tc(c.transportControls)
            else {
                val now = SystemClock.uptimeMillis()
                c.dispatchMediaButtonEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, key, 0))
                c.dispatchMediaButtonEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, key, 0))
            }
        }.isSuccess
        if (!ok) sendKey(key)
        main.postDelayed({ publish() }, 400)
    }

    private fun sendKey(code: Int) {
        val now = SystemClock.uptimeMillis()
        audio.dispatchMediaKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, code, 0))
        audio.dispatchMediaKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, code, 0))
    }

    fun volume(delta: Int) {
        audio.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            if (delta > 0) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER,
            AudioManager.FLAG_SHOW_UI,
        )
    }

    fun accessSettingsIntent(): Intent =
        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /**
     * No Android 13+ apps instalados por APK precisam antes de "Permitir
     * configurações restritas" (Informações do app → menu ⋮). Esta tela abre lá.
     */
    fun appInfoIntent(): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:" + context.packageName))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** Pede direto o acesso ao listener (Android 11+), quando a central oferece a tela. */
    fun directAccessIntent(): Intent? {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.R) return null
        return Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
            .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, listenerComponent.flattenToString())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
