package com.slick.browser.data

import android.graphics.Bitmap
import android.webkit.WebView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.UUID

enum class SearchEngine(val label: String, val queryUrl: String) {
    GOOGLE("Google", "https://www.google.com/search?q="),
    YANDEX("Yandex", "https://yandex.ru/search/?text="),
    DUCKGO("DuckGo", "https://duckduckgo.com/?q=")
}

enum class BrowserTheme { BLACK, WHITE }
enum class LiquidMode { LIQUID, MONO }
enum class PanelState { NONE, BOOKMARKS, TAB_CAROUSEL, TAB_GRID }
enum class SettingsSection { NETWORK, INTERFACE, DATA, SYSTEM }

data class Bookmark(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val url: String
)

data class HistoryEntry(
    val title: String,
    val url: String,
    val visitedAt: Long = System.currentTimeMillis()
)

data class DownloadEntry(
    val id: Long,
    val title: String,
    val url: String,
    val destination: String,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = -1L,
    val status: Int = 0
)

class BrowserTab(
    val id: String = UUID.randomUUID().toString(),
    val incognito: Boolean,
    initialUrl: String? = null
) {
    var webView: WebView? = null
    var currentUrl by mutableStateOf(initialUrl.orEmpty())
    var title by mutableStateOf(if (initialUrl == null) "Новая вкладка" else initialUrl)
    var progress by mutableIntStateOf(if (initialUrl == null) 100 else 0)
    var isLoading by mutableStateOf(initialUrl != null)
    var canGoBack by mutableStateOf(false)
    var desktopMode by mutableStateOf(false)
    var textZoom by mutableIntStateOf(100)
    var errorCode by mutableStateOf<String?>(null)
    var snapshot by mutableStateOf<Bitmap?>(null)
    var pendingUrl by mutableStateOf(initialUrl)
    var lastVisitedAt = System.currentTimeMillis()

    fun domain(): String = currentUrl
        .removePrefix("https://")
        .removePrefix("http://")
        .substringBefore('/')
        .ifBlank { "slick" }
}
