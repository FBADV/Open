package com.fbadv.autoglass.overlay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.fbadv.autoglass.AutoGlassApp

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (AutoGlassApp.instance.settings.value.homeBubble) HomeBubbleService.start(context)
    }
}
