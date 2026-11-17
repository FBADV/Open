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
            _nowPlaying.value = null
            return
        }
        val md = c.metadata
        val st = c.playbackState
        val art: Bitmap? = md?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: md?.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: md?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
        val previous = _nowPlaying.value
        _nowPlaying.value = NowPlaying(
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
        )
    }

    /**
     * Tocar/pausar. Com uma sessão ativa usa os controles dela; sem nenhuma,
     * envia "play" (o Android retoma o último app de música) e, se nada
     * começar a tocar, abre o app de música escolhido (YouTube Music).
     */
    fun playPause() {
        val c = controller
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
        val c = controller ?: return sendKey(KeyEvent.KEYCODE_MEDIA_NEXT)
        command(c, PlaybackState.ACTION_SKIP_TO_NEXT, KeyEvent.KEYCODE_MEDIA_NEXT) { it.skipToNext() }
    }

    fun previous() {
        val c = controller ?: return sendKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
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
