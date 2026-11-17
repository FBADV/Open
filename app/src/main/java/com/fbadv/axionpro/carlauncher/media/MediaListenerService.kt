package com.fbadv.axionpro.carlauncher.media

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.fbadv.axionpro.carlauncher.AxionApp

/**
 * Serviço de notificações: libera ao launcher as sessões de mídia ativas e
 * também lê as notificações de rádios/players de fábrica que não publicam
 * sessão de mídia (estação, música, capa).
 */
class MediaListenerService : NotificationListenerService() {
    override fun onListenerConnected() {
        super.onListenerConnected()
        val media = AxionApp.instance.media
        media.attach()
        runCatching { activeNotifications?.forEach { media.onNotification(it, posted = true) } }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        AxionApp.instance.media.attach()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        AxionApp.instance.media.onNotification(sbn, posted = true)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        AxionApp.instance.media.onNotification(sbn, posted = false)
    }
}
