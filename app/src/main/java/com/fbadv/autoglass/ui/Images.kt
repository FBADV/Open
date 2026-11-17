package com.fbadv.autoglass.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.net.Uri
import com.fbadv.autoglass.R
import com.fbadv.autoglass.data.SceneMode
import java.io.File

object Images {
    fun sceneRes(mode: SceneMode): Int = when (mode) {
        SceneMode.RUA -> R.drawable.scene_rua
        SceneMode.DUNAS -> R.drawable.scene_dunas
        SceneMode.ESTRADA -> R.drawable.scene_estrada
        SceneMode.LAMA -> R.drawable.scene_lama
    }

    fun loadScene(context: Context, mode: SceneMode, customPath: String?): Bitmap? =
        customPath?.let { decodeFile(it, 1920) } ?: decodeRes(context, sceneRes(mode), 1920)

    fun loadVehicle(context: Context, customPath: String?): Bitmap? =
        customPath?.let { decodeFile(it, 1400) } ?: decodeRes(context, R.drawable.vehicle_compass_limited, 1400)

    private fun decodeRes(context: Context, res: Int, maxW: Int): Bitmap? = runCatching {
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeResource(context.resources, res, o)
        BitmapFactory.decodeResource(context.resources, res, BitmapFactory.Options().apply {
            inSampleSize = sample(o.outWidth, maxW)
        })
    }.getOrNull()

    private fun decodeFile(path: String, maxW: Int): Bitmap? = runCatching {
        if (!File(path).exists()) return null
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, o)
        BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample(o.outWidth, maxW) })
    }.getOrNull()

    private fun sample(w: Int, maxW: Int): Int {
        var s = 1
        while (w / (s * 2) >= maxW) s *= 2
        return s
    }

    /** Copia uma imagem escolhida pelo usuário para o armazenamento do app. */
    fun import(context: Context, uri: Uri, name: String): String? = runCatching {
        val dst = File(context.filesDir, name)
        context.contentResolver.openInputStream(uri)!!.use { input -> dst.outputStream().use { input.copyTo(it) } }
        dst.absolutePath
    }.getOrNull()

    /**
     * Gera a versão desfocada do fundo (usada pelo efeito de vidro):
     * reduz, aplica box-blur em 3 passadas e escurece levemente.
     */
    fun blurred(src: Bitmap, width: Int = 128, radius: Int = 4): Bitmap {
        val h = (src.height * width.toFloat() / src.width).toInt().coerceAtLeast(8)
        val small = Bitmap.createScaledBitmap(src, width, h, true).copy(Bitmap.Config.ARGB_8888, true)
        val px = IntArray(width * h)
        small.getPixels(px, 0, width, 0, 0, width, h)
        repeat(3) {
            boxBlur(px, width, h, radius, horizontal = true)
            boxBlur(px, width, h, radius, horizontal = false)
        }
        small.setPixels(px, 0, width, 0, 0, width, h)
        val out = Bitmap.createBitmap(width, h, Bitmap.Config.ARGB_8888)
        val cm = ColorMatrix().apply { setSaturation(1.25f) }
        cm.postConcat(ColorMatrix(floatArrayOf(
            0.78f, 0f, 0f, 0f, 0f,
            0f, 0.78f, 0f, 0f, 0f,
            0f, 0f, 0.82f, 0f, 6f,
            0f, 0f, 0f, 1f, 0f,
        )))
        Canvas(out).drawBitmap(small, 0f, 0f, Paint().apply { colorFilter = ColorMatrixColorFilter(cm) })
        return out
    }

    private fun boxBlur(px: IntArray, w: Int, h: Int, r: Int, horizontal: Boolean) {
        val outer = if (horizontal) h else w
        val inner = if (horizontal) w else h
        val tmp = IntArray(inner)
        for (o in 0 until outer) {
            var ra = 0; var rr = 0; var rg = 0; var rb = 0
            fun idx(i: Int) = if (horizontal) o * w + i.coerceIn(0, inner - 1) else i.coerceIn(0, inner - 1) * w + o
            for (i in -r..r) {
                val c = px[idx(i)]
                ra += c ushr 24; rr += (c shr 16) and 255; rg += (c shr 8) and 255; rb += c and 255
            }
            val n = 2 * r + 1
            for (i in 0 until inner) {
                tmp[i] = ((ra / n) shl 24) or ((rr / n) shl 16) or ((rg / n) shl 8) or (rb / n)
                val add = px[idx(i + r + 1)]
                val rem = px[idx(i - r)]
                ra += (add ushr 24) - (rem ushr 24)
                rr += ((add shr 16) and 255) - ((rem shr 16) and 255)
                rg += ((add shr 8) and 255) - ((rem shr 8) and 255)
                rb += (add and 255) - (rem and 255)
            }
            for (i in 0 until inner) px[idx(i)] = tmp[i]
        }
    }
}
