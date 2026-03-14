package com.hianimetvx

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

@CloudstreamPlugin
class Hianime : MainAPI() {
    override var name = "HiAnimeTV"
    override var baseUrl = "https://hianimestv.su"
    override val hasMainPage = true
    override val supportedTypes = setOf(TvType.Anime)

    override suspend fun getMainPage(): HomePageResponse {
        val doc = app.get(baseUrl).document
        val latest = doc.select("div.latest-episodes div.item, div.recent div.card, div.anime-card, div.item").map { el ->
            val title = el.select("a.title, h3 a, .name, .title").text()
            val url = fixUrl(el.select("a").attr("href"))
            val poster = fixUrl(el.select("img").attr("src") ?: el.select("img").attr("data-src") ?: el.select("img").attr("data-lazy"))
            AnimeSearchResponse(title, url, name, TvType.Anime, poster)
        }
        return HomePageResponse(listOf(HomePageList("Son Bölümler", latest)))
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val doc = app.get("$baseUrl/search?keyword=$query").document
        return doc.select("div.search-item, div.anime-card, div.item").map { el ->
            AnimeSearchResponse(el.select("h3 a, .title, a.title").text(), fixUrl(el.select("a").attr("href")), name, TvType.Anime, fixUrl(el.select("img").attr("src") ?: el.select("img").attr("data-src")))
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val doc = app.get(url).document
        val title = doc.select("h1.title, h1.anime-title, .anime-name").text()
        val poster = fixUrl(doc.select("div.poster img, img.poster, .cover img").attr("src"))
        val episodes = doc.select("div.ep-list a, ul.episodes li a, .episode-list a").mapIndexed { i, el ->
            Episode(fixUrl(el.attr("href")), "Episode ${i+1}", i+1)
        }
        return newAnimeLoadResponse(title, url, TvType.Anime) {
            posterUrl = poster
            addEpisodes(episodes)
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit) {
        val doc = app.get(data).document
        val iframe = doc.select("iframe[src*='player'], iframe[src*='embed'], iframe").attr("src")
        if (iframe.isNotEmpty()) loadExtractor(fixUrl(iframe), data, subtitleCallback, callback)
        
        doc.select("video source, a[href*='.m3u8'], a[href*='.mp4']").forEach {
            callback(ExtractorLink(name, "Direct", fixUrl(it.attr("src") ?: it.attr("href")), baseUrl, Qualities.Unknown.value))
        }
    }
}