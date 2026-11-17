package com.fbadv.axionpro.carlauncher.ui.panels

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.GestureDetector
import android.view.MotionEvent
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

/**
 * YouTube dentro do painel (versão web, totalmente tocável).
 * Dois toques abrem o app do YouTube em tela cheia.
 */
@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@Composable
fun VideoPanel(url: String, onDoubleTap: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val web = remember {
        WebView(context).apply {
            setBackgroundColor(Color.BLACK)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    val scheme = request.url.scheme ?: return true
                    // links "intent://" e de outros apps ficam no painel
                    return scheme != "http" && scheme != "https"
                }
            }
            val detector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
                override fun onDoubleTap(e: MotionEvent): Boolean { onDoubleTap(); return true }
            })
            setOnTouchListener { _, ev -> detector.onTouchEvent(ev); false }
            loadUrl(url)
        }
    }
    DisposableEffect(Unit) {
        web.onResume()
        onDispose {
            web.onPause()
            web.stopLoading()
            web.destroy()
        }
    }
    AndroidView(factory = { web }, modifier = modifier)
}
