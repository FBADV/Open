package com.fbadv.axionpro.carlauncher.overlay

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.fbadv.axionpro.carlauncher.AxionApp
import com.fbadv.axionpro.carlauncher.MainActivity
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.fbadv.axionpro.carlauncher.R
import kotlin.math.abs

/**
 * Serviço permanente do Axion Pro:
 *  - abre o launcher quando a central liga/acorda (tela acende após ficar apagada);
 *  - opcionalmente mostra o botão Home flutuante sobre os outros apps.
 */
class HomeBubbleService : Service() {
    private var wm: WindowManager? = null
    private var bubble: View? = null
    private var params: WindowManager.LayoutParams? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        startAsForeground()
        val s = AxionApp.instance.settings.value
        if (s.homeBubble && Settings.canDrawOverlays(this)) addBubble()
        registerScreenReceiver()
    }

    private var screenOffAt = 0L
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            when (i.action) {
                Intent.ACTION_SCREEN_OFF -> screenOffAt = SystemClock.elapsedRealtime()
                Intent.ACTION_SCREEN_ON, Intent.ACTION_USER_PRESENT -> {
                    val wasOffLong = screenOffAt > 0 && SystemClock.elapsedRealtime() - screenOffAt > 20_000
                    if (wasOffLong && AxionApp.instance.settings.value.autoStart) {
                        screenOffAt = 0
                        goHome(c)
                    }
                }
            }
        }
    }

    private fun registerScreenReceiver() {
        val f = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        ContextCompat.registerReceiver(this, screenReceiver, f, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    private fun startAsForeground() {
        val nm = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "Axion Pro em segundo plano", NotificationManager.IMPORTANCE_MIN),
            )
        }
        val n: Notification = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_bubble_home)
            .setContentTitle("Axion Pro Car Launcher")
            .setContentText("Tela inicial ativa")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else 0
        ServiceCompat.startForeground(this, 1, n, type)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun addBubble() {
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        this.wm = wm
        val size = (resources.displayMetrics.density * 56).toInt()
        val view = ImageView(this).apply {
            setImageResource(R.drawable.ic_launcher_fg)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0x991B2A4A.toInt())
                setStroke((resources.displayMetrics.density * 1.5f).toInt(), 0x66FFFFFF)
            }
            elevation = 12f
        }
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE
        }
        val lp = WindowManager.LayoutParams(
            size, size, type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            val prefs = getSharedPreferences("bubble", MODE_PRIVATE)
            x = prefs.getInt("x", 24)
            y = prefs.getInt("y", 200)
        }
        params = lp
        var downX = 0f; var downY = 0f; var startX = 0; var startY = 0; var moved = false
        view.setOnTouchListener { v, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = e.rawX; downY = e.rawY; startX = lp.x; startY = lp.y; moved = false
                    v.alpha = 1f
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = e.rawX - downX; val dy = e.rawY - downY
                    if (abs(dx) > 12 || abs(dy) > 12) moved = true
                    lp.x = startX + dx.toInt(); lp.y = startY + dy.toInt()
                    runCatching { wm.updateViewLayout(v, lp) }
                }
                MotionEvent.ACTION_UP -> {
                    v.alpha = 0.85f
                    if (moved) {
                        getSharedPreferences("bubble", MODE_PRIVATE).edit().putInt("x", lp.x).putInt("y", lp.y).apply()
                    } else goHome(this)
                }
            }
            true
        }
        view.alpha = 0.85f
        runCatching { wm.addView(view, lp) }
        bubble = view
    }

    fun setVisible(visible: Boolean) {
        bubble?.visibility = if (visible) View.VISIBLE else View.GONE
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(screenReceiver) }
        runCatching { bubble?.let { wm?.removeView(it) } }
        bubble = null
        instance = null
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL = "home_bubble"
        var instance: HomeBubbleService? = null
            private set

        fun goHome(context: Context) {
            runCatching {
                context.startActivity(
                    Intent(context, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT),
                )
            }
        }

        /** Liga ou desliga o serviço conforme as opções (abrir ao ligar / botão flutuante). */
        fun refresh(context: Context) {
            val s = AxionApp.instance.settings.value
            stop(context)
            if (s.autoStart || s.homeBubble) start(context)
        }

        fun start(context: Context) {
            val i = Intent(context, HomeBubbleService::class.java)
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(i) else context.startService(i)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, HomeBubbleService::class.java))
        }
    }
}
