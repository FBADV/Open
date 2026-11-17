package com.fbadv.axionpro.carlauncher.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Sequência de quadros para o giro 360° do veículo.
 * Os quadros ficam comprimidos em memória e são decodificados sob demanda
 * (com cache), para não pesar na RAM das centrais.
 */
class FrameSource(private val frames: List<ByteArray>) {
    val count: Int get() = frames.size
    private val cache = LruCache<Int, ImageBitmap>(14)

    fun cached(i: Int): ImageBitmap? = cache.get(i)

    suspend fun frame(i: Int): ImageBitmap? {
        if (frames.isEmpty()) return null
        val k = Math.floorMod(i, frames.size)
        cache.get(k)?.let { return it }
        return withContext(Dispatchers.Default) {
            val b: Bitmap? = BitmapFactory.decodeByteArray(frames[k], 0, frames[k].size)
            b?.asImageBitmap()?.also { cache.put(k, it) }
        }
    }

    companion object {
        /** Modelo 3D do Compass Limited embutido no app (dia = faróis apagados, noite = acesos). */
        fun fromAssets(context: Context, folder: String): FrameSource? = runCatching {
            val names = context.assets.list(folder).orEmpty().filter { it.endsWith(".webp") || it.endsWith(".png") }.sorted()
            if (names.isEmpty()) null else FrameSource(names.map { n -> context.assets.open("$folder/$n").use { it.readBytes() } })
        }.getOrNull()

        /** Fotos 360° importadas pelo usuário (Configurações → Veículo). */
        fun fromFiles(dir: File): FrameSource? {
            val files = dir.listFiles()?.sortedBy { it.name }.orEmpty()
            if (files.size < 8) return null
            return FrameSource(files.map { it.readBytes() })
        }
    }
}
