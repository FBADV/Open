package com.fbadv.axionpro.carlauncher.overlay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.fbadv.axionpro.carlauncher.AxionApp

/**
 * Ao ligar a central: abre o Axion Pro e inicia o serviço que o traz de volta
 * sempre que a tela acende (centrais que "dormem" ao desligar o carro).
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val s = AxionApp.instance.settings.value
        HomeBubbleService.refresh(context)
        if (s.autoStart && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) HomeBubbleService.goHome(context)
    }
}
