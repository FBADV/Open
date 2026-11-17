package com.fbadv.autoglass.media

import android.service.notification.NotificationListenerService
import com.fbadv.autoglass.AutoGlassApp

/**
 * Serviço de notificações: só existe para que o Android libere ao launcher a
 * leitura das sessões de mídia ativas (Spotify, YouTube Music, Deezer, rádio…).
 */
class MediaListenerService : NotificationListenerService() {
    override fun onListenerConnected() {
        super.onListenerConnected()
        AutoGlassApp.instance.media.attach()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        AutoGlassApp.instance.media.attach()
    }
}
