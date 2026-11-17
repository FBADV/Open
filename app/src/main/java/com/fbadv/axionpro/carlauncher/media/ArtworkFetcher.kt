package com.fbadv.axionpro.carlauncher.media

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Busca a capa do álbum quando o app que toca não fornece (player e rádio da
 * central). Usa a busca pública do iTunes, gratuita e sem chave de API.
 */
object ArtworkFetcher {
    private val cache = LruCache<String, Bitmap>(40)
    private val misses = HashSet<String>()

    fun lookup(artist: String, title: String): Bitmap? {
        val key = "${artist.trim().lowercase()}|${title.trim().lowercase()}"
        if (title.isBlank()) return null
        cache.get(key)?.let { return it }
        if (key in misses) return null
        return runCatching {
            val term = URLEncoder.encode("$artist $title".trim(), "UTF-8")
            val json = get("https://itunes.apple.com/search?term=$term&entity=song&limit=1&country=BR")
            val results = JSONObject(json).getJSONArray("results")
            if (results.length() == 0) null else {
                val url = results.getJSONObject(0).optString("artworkUrl100").replace("100x100bb", "600x600bb")
                if (url.isBlank()) null else download(url)
            }
        }.getOrNull().also { if (it != null) cache.put(key, it) else misses.add(key) }
    }

    private fun get(url: String): String {
        val c = (URL(url).openConnection() as HttpURLConnection).apply { connectTimeout = 8000; readTimeout = 8000 }
        return c.inputStream.bufferedReader().use { it.readText() }
    }

    private fun download(url: String): Bitmap? {
        val c = (URL(url).openConnection() as HttpURLConnection).apply { connectTimeout = 8000; readTimeout = 8000 }
        return c.inputStream.use { BitmapFactory.decodeStream(it) }
    }
}
