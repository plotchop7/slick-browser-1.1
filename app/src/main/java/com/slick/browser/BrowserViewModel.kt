package com.slick.browser

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.PowerManager
import android.view.Window
import android.webkit.CookieManager
import android.webkit.WebStorage
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.slick.browser.data.*
import com.slick.browser.web.SlickWebViewFactory
import com.slick.browser.web.SnapshotManager
import com.slick.browser.web.WebInjector
import com.slick.browser.web.WebTabCallbacks

class BrowserViewModel(application: Application) : AndroidViewModel(application), WebTabCallbacks {
    private val repository = BrowserRepository(application)
    private val regular = mutableStateListOf<BrowserTab>()
    private val private = mutableStateListOf<BrowserTab>()

    var activeTabId by mutableStateOf<String?>(null)
        private set
    var isIncognito by mutableStateOf(false)
        private set
    var zeroPointVisible by mutableStateOf(true)
        private set
    var panel by mutableStateOf(PanelState.NONE)
        private set
    var settingsOpen by mutableStateOf(false)
    var siteMenuOpen by mutableStateOf(false)
    var historyOpen by mutableStateOf(false)
    var downloadsOpen by mutableStateOf(false)
    var settingsSection by mutableStateOf(SettingsSection.NETWORK)
    var searchEngine by mutableStateOf(repository.searchEngine)
    var monochrome by mutableStateOf(repository.monochrome)
    var browserTheme by mutableStateOf(repository.theme)
    var liquidMode by mutableStateOf(repository.liquidMode)
    var energySaver by mutableStateOf(repository.energySaver)
    var systemPowerSaver by mutableStateOf(false)
    var downloadPath by mutableStateOf(repository.downloadPath)
    var incognitoArmedUntil by mutableStateOf(0L)
        private set
    var waveNonce by mutableStateOf(0L)
        private set

    val bookmarks = mutableStateListOf<Bookmark>()
    val history = mutableStateListOf<HistoryEntry>()
    val downloads = mutableStateListOf<DownloadEntry>()

    val tabs: MutableList<BrowserTab> get() = if (isIncognito) private else regular
    val activeTab: BrowserTab? get() = tabs.firstOrNull { it.id == activeTabId }
    val effectiveEnergySaver: Boolean get() = energySaver || systemPowerSaver
    val downloadProgress: Float
        get() {
            val active = downloads.filter {
                it.status == android.app.DownloadManager.STATUS_RUNNING ||
                    it.status == android.app.DownloadManager.STATUS_PENDING
            }
            if (active.isEmpty()) return 0f
            val done = active.sumOf { it.downloadedBytes.coerceAtLeast(0L) }
            val total = active.sumOf { if (it.totalBytes > 0) it.totalBytes else it.downloadedBytes + 1L }
            return if (total <= 0L) 0.08f else (done.toFloat() / total.toFloat()).coerceIn(0.02f, 1f)
        }

    private val downloadController = DownloadController(application, viewModelScope) { items ->
        downloads.clear()
        downloads.addAll(items)
    }

    init {
        bookmarks.addAll(repository.bookmarks())
        history.addAll(repository.history())
        refreshPowerMode()
    }

    fun refreshPowerMode() {
        val manager = getApplication<Application>().getSystemService(PowerManager::class.java)
        systemPowerSaver = manager.isPowerSaveMode
        applyWebPolicies()
    }

    fun openPanel(next: PanelState) {
        if (panel == next) return
        panel = next
        siteMenuOpen = false
        if (next != PanelState.NONE) waveNonce = System.nanoTime()
        applyWebPolicies()
    }

    fun showZeroPoint() {
        zeroPointVisible = true
        panel = PanelState.NONE
        siteMenuOpen = false
        applyWebPolicies()
    }

    fun submit(raw: String, editActive: Boolean = false) {
        val value = raw.trim()
        if (value.isBlank()) return
        val url = toUrl(value)
        if (!isOnline()) {
            val tab = if (editActive) activeTab ?: createTab() else createTab()
            tab.currentUrl = url
            tab.title = url
            tab.errorCode = "ERR_INTERNET_DISCONNECTED"
            zeroPointVisible = false
            return
        }
        val tab = if (editActive) activeTab ?: createTab() else createTab()
        tab.errorCode = null
        tab.currentUrl = url
        tab.title = url
        tab.progress = 0
        tab.isLoading = true
        tab.webView?.loadUrl(url) ?: run { tab.pendingUrl = url }
        zeroPointVisible = false
        panel = PanelState.NONE
        applyWebPolicies()
    }

    fun openExternal(url: String) = submit(url)

    fun createTab(): BrowserTab {
        val tab = BrowserTab(incognito = isIncognito)
        tabs.add(tab)
        activeTabId = tab.id
        return tab
    }

    fun selectTab(id: String) {
        if (tabs.none { it.id == id }) return
        activeTabId = id
        zeroPointVisible = false
        panel = PanelState.NONE
        activeTab?.let { tab ->
            tab.lastVisitedAt = System.currentTimeMillis()
            if (tab.webView == null && tab.pendingUrl != null) return@let
        }
        applyWebPolicies()
    }

    fun closeTab(id: String) {
        val index = tabs.indexOfFirst { it.id == id }
        if (index < 0) return
        val removed = tabs.removeAt(index)
        SlickWebViewFactory.destroy(removed)
        if (activeTabId == id) activeTabId = tabs.getOrNull(index.coerceAtMost(tabs.lastIndex))?.id
        if (tabs.isEmpty()) {
            activeTabId = null
            zeroPointVisible = true
            panel = PanelState.NONE
        }
    }

    fun switchTab(direction: Int) {
        if (tabs.size < 2) return
        val current = tabs.indexOfFirst { it.id == activeTabId }.coerceAtLeast(0)
        val target = (current + direction).coerceIn(0, tabs.lastIndex)
        if (target == current) return
        selectTab(tabs[target].id)
    }

    fun adjacentTab(direction: Int): BrowserTab? {
        val current = tabs.indexOfFirst { it.id == activeTabId }
        return tabs.getOrNull(current + direction)
    }

    fun prepareSnapshot(tab: BrowserTab, window: Window) = SnapshotManager.capture(tab, window)

    fun reloadActive() {
        activeTab?.webView?.reload()
    }

    fun retryActive() = activeTab?.let(SlickWebViewFactory::retry)

    fun goBack(): Boolean {
        when {
            settingsOpen -> settingsOpen = false
            historyOpen -> historyOpen = false
            downloadsOpen -> downloadsOpen = false
            siteMenuOpen -> siteMenuOpen = false
            panel != PanelState.NONE -> openPanel(PanelState.NONE)
            zeroPointVisible && tabs.isNotEmpty() -> zeroPointVisible = false
            activeTab?.webView?.canGoBack() == true -> activeTab?.webView?.goBack()
            else -> return false
        }
        return true
    }

    fun armIncognito() {
        incognitoArmedUntil = System.currentTimeMillis() + 3_000L
    }

    fun completeIncognitoGesture(direction: Int): Boolean {
        if (System.currentTimeMillis() > incognitoArmedUntil || direction == 0) return false
        incognitoArmedUntil = 0
        if (isIncognito) leaveIncognito() else enterIncognito()
        return true
    }

    private fun enterIncognito() {
        isIncognito = true
        activeTabId = null
        zeroPointVisible = true
        panel = PanelState.NONE
        settingsOpen = false
    }

    private fun leaveIncognito() {
        wipeIncognito()
        isIncognito = false
        activeTabId = regular.firstOrNull()?.id
        zeroPointVisible = true
        panel = PanelState.NONE
        settingsOpen = false
    }

    fun wipeIncognito() {
        private.forEach(SlickWebViewFactory::destroy)
        private.clear()
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
        WebStorage.getInstance().deleteAllData()
    }

    fun updateSearchEngine(value: SearchEngine) {
        searchEngine = value
        repository.searchEngine = value
    }

    fun updateMonochrome(value: Boolean) {
        monochrome = value
        repository.monochrome = value
        tabs.forEach { tab -> tab.webView?.let { WebInjector.inject(it, value) } }
        regular.forEach { tab -> tab.webView?.let { WebInjector.inject(it, value) } }
    }

    fun updateTheme(value: BrowserTheme) {
        browserTheme = value
        repository.theme = value
    }

    fun updateLiquidMode(value: LiquidMode) {
        liquidMode = value
        repository.liquidMode = value
    }

    fun updateEnergySaver(value: Boolean) {
        energySaver = value
        repository.energySaver = value
        applyWebPolicies()
    }

    fun updateDownloadPath(value: String) {
        downloadPath = value
        repository.downloadPath = value
    }

    fun setDesktopMode(enabled: Boolean) = activeTab?.let { SlickWebViewFactory.applyDesktopMode(it, enabled) }

    fun setTextZoom(value: Int) {
        activeTab?.let {
            it.textZoom = value.coerceIn(30, 200)
            it.webView?.settings?.textZoom = it.textZoom
        }
    }

    fun addBookmark(tab: BrowserTab? = activeTab) {
        if (tab == null || tab.currentUrl.isBlank() || bookmarks.any { it.url == tab.currentUrl }) return
        bookmarks.add(0, Bookmark(title = tab.title.ifBlank { tab.domain() }, url = tab.currentUrl))
        repository.saveBookmarks(bookmarks)
    }

    fun removeBookmark(id: String) {
        bookmarks.removeAll { it.id == id }
        repository.saveBookmarks(bookmarks)
    }

    fun moveBookmark(id: String, targetIndex: Int) {
        val old = bookmarks.indexOfFirst { it.id == id }
        if (old < 0) return
        val item = bookmarks.removeAt(old)
        bookmarks.add(targetIndex.coerceIn(0, bookmarks.size), item)
        repository.saveBookmarks(bookmarks)
    }

    fun clearHistory() {
        repository.clearHistory()
        history.clear()
    }

    fun copyLink(url: String) {
        val manager = getApplication<Application>().getSystemService(ClipboardManager::class.java)
        manager.setPrimaryClip(ClipData.newPlainText("Slick URL", url))
    }

    fun shareLink(url: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, url)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        ContextCompat.startActivity(
            getApplication(), Intent.createChooser(intent, "Поделиться").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), null
        )
    }

    override fun onStateChanged(tab: BrowserTab) = Unit

    override fun onHistoryCommit(tab: BrowserTab, title: String, url: String) {
        if (tab.incognito) return
        val entry = HistoryEntry(title.ifBlank { tab.domain() }, url)
        history.removeAll { it.url == url }
        history.add(0, entry)
        if (history.size > 1000) history.removeAt(history.lastIndex)
        repository.addHistory(entry)
    }

    override fun onDownload(
        tab: BrowserTab,
        url: String,
        userAgent: String,
        contentDisposition: String?,
        mimeType: String?
    ) {
        if (!tab.incognito) {
            downloadController.enqueue(url, userAgent, contentDisposition, mimeType, repository.downloadPath)
        }
    }

    override fun monochromeEnabled(): Boolean = monochrome

    fun refreshDownloads() = downloadController.refresh()

    private fun toUrl(value: String): String {
        val explicit = value.startsWith("http://") || value.startsWith("https://")
        val looksLikeDomain = !value.contains(" ") &&
            Regex("^[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}([/:?#].*)?$").matches(value)
        return when {
            explicit -> value
            looksLikeDomain -> "https://$value"
            else -> searchEngine.queryUrl + android.net.Uri.encode(value)
        }
    }

    private fun isOnline(): Boolean {
        val manager = getApplication<Application>().getSystemService(ConnectivityManager::class.java)
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun applyWebPolicies() {
        (regular + private).forEach { tab ->
            val web = tab.webView ?: return@forEach
            val background = tab.id != activeTabId || zeroPointVisible || panel != PanelState.NONE
            if (background) web.onPause() else web.onResume()
            if (effectiveEnergySaver) WebInjector.limitFrameRate(web, true)
        }
    }

    override fun onCleared() {
        regular.forEach(SlickWebViewFactory::destroy)
        wipeIncognito()
        super.onCleared()
    }
}
