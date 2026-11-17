package com.fbadv.axionpro.carlauncher.data

/**
 * Pacotes conhecidos de apps populares e dos apps de fábrica das centrais
 * multimídia Android mais comuns (FYT/SYU, MTCD/Microntek, TS, Topway, etc.).
 */
object KnownApps {
    val NAVIGATION = listOf(
        "com.waze",
        "com.google.android.apps.maps",
        "com.google.android.apps.mapslite",
        "com.sygic.aura",
        "com.here.app.maps",
        "net.osmand",
        "net.osmand.plus",
        "app.organicmaps",
        "com.mapswithme.maps.pro",
    )
    val MUSIC = listOf(
        "com.google.android.apps.youtube.music",
        "com.spotify.music",
        "deezer.android.app",
        "com.amazon.mp3",
        "com.apple.android.music",
        "com.aspiro.tidal",
        "com.soundcloud.android",
        "com.palcomp3.app",
        "com.google.android.music",
    )
    val VIDEO = listOf(
        "com.google.android.youtube",
        "com.google.android.youtube.tv",
        "com.amazon.avod.thirdpartyclient",
        "com.netflix.mediaclient",
        "com.globo.globotv",
        "com.disney.disneyplus",
        "com.wbd.stream",
        "com.hbo.hbonow",
        "org.videolan.vlc",
    )
    val RADIO = listOf(
        "com.syu.radio",
        "com.fyt.radio",
        "com.microntek.radio",
        "com.ts.radio",
        "com.android.fmradio",
        "com.caf.fmradio",
        "com.android.car.radio",
        "com.spreadtrum.fmradio",
        "com.mediatek.FMRadio",
        "com.hct.radio",
        "com.yf.radio",
        "com.android.radio",
        "com.zjinnova.radio",
        "com.nwd.radio",
        "com.tw.radio",
    )
    val PLAYER = listOf(
        "com.syu.music",
        "com.fyt.music",
        "com.microntek.music",
        "com.ts.music",
        "com.android.car.media",
        "com.android.music",
        "com.hct.music",
        "com.nwd.music",
        "com.tw.music",
        "com.zjinnova.music",
    )
    val PHONE = listOf(
        "com.syu.bt",
        "com.fyt.bt",
        "com.microntek.bluetooth",
        "com.ts.bt",
        "com.android.car.dialer",
        "com.google.android.dialer",
        "com.android.dialer",
        "com.hct.bt",
        "com.nwd.bt",
        "com.tw.bt",
        "com.zjinnova.bt",
    )

    fun packagesFor(role: AppRole): List<String> = when (role) {
        AppRole.NAVIGATION -> NAVIGATION
        // YouTube Music (YouTube Premium) primeiro; sem ele, o próprio YouTube
        AppRole.MUSIC -> listOf(MUSIC.first(), YOUTUBE) + MUSIC.drop(1)
        AppRole.RADIO -> RADIO
        AppRole.PLAYER -> PLAYER
        AppRole.VIDEO -> VIDEO
        AppRole.PHONE -> PHONE
    }

    fun labelRegexFor(role: AppRole): Regex? = when (role) {
        AppRole.RADIO -> Regex("r[aá]dio|\\bfm\\b|tuner", RegexOption.IGNORE_CASE)
        AppRole.PLAYER -> Regex("^(m[uú]sica|music|player|media|m[ií]dia)$", RegexOption.IGNORE_CASE)
        AppRole.PHONE -> Regex("bluetooth|telefone|phone|discador|dialer|\\bbt\\b", RegexOption.IGNORE_CASE)
        AppRole.NAVIGATION -> Regex("waze|maps|navega", RegexOption.IGNORE_CASE)
        AppRole.MUSIC -> Regex("spotify|deezer|youtube music|amazon music|tidal", RegexOption.IGNORE_CASE)
        AppRole.VIDEO -> Regex("youtube$|prime video|netflix|globoplay|disney|max$", RegexOption.IGNORE_CASE)
    }

    /** Fontes de mídia mostradas como atalhos rápidos no cartão de música. */
    val MEDIA_SHORTCUTS = MUSIC + listOf("com.google.android.youtube", "com.amazon.avod.thirdpartyclient")

    const val WAZE = "com.waze"
    const val GOOGLE_MAPS = "com.google.android.apps.maps"
    const val YOUTUBE = "com.google.android.youtube"
}
