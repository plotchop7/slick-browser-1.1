package com.slick.browser.web

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.slick.browser.data.BrowserTab

interface WebTabCallbacks {
    fun onStateChanged(tab: BrowserTab)
    fun onHistoryCommit(tab: BrowserTab, title: String, url: String)
    fun onDownload(tab: BrowserTab, url: String, userAgent: String, contentDisposition: String?, mimeType: String?)
    fun monochromeEnabled(): Boolean
}

object SlickWebViewFactory {
    @SuppressLint("SetJavaScriptEnabled")
    fun create(context: Context, tab: BrowserTab, callbacks: WebTabCallbacks): WebView {
        return WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.BLACK)
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false
            overScrollMode = WebView.OVER_SCROLL_NEVER
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = !tab.incognito
                databaseEnabled = !tab.incognito
                loadsImagesAutomatically = true
                useWideViewPort = true
                loadWithOverviewMode = true
                builtInZoomControls = true
                displayZoomControls = false
                allowFileAccess = false
                allowContentAccess = false
                mediaPlaybackRequiresUserGesture = true
                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                cacheMode = if (tab.incognito) WebSettings.LOAD_NO_CACHE else WebSettings.LOAD_DEFAULT
                textZoom = tab.textZoom
                userAgentString = if (tab.desktopMode) desktopUserAgent(userAgentString) else userAgentString
                setSupportMultipleWindows(false)
                javaScriptCanOpenWindowsAutomatically = false
                setGeolocationEnabled(false)
                saveFormData = false
                savePassword = false
            }
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, false)
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    val uri = request.url
                    if (uri.scheme == "http" || uri.scheme == "https") return false
                    return try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        true
                    } catch (_: ActivityNotFoundException) {
                        true
                    }
                }

                override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
                    tab.currentUrl = url.orEmpty()
                    tab.canGoBack = view.canGoBack()
                    callbacks.onStateChanged(tab)
                }

                override fun onPageStarted(view: WebView, url: String?, favicon: android.graphics.Bitmap?) {
                    tab.currentUrl = url.orEmpty()
                    tab.isLoading = true
                    tab.errorCode = null
                    callbacks.onStateChanged(tab)
                }

                override fun onPageCommitVisible(view: WebView, url: String?) {
                    if (!tab.incognito && !url.isNullOrBlank() && url != "about:blank") {
                        callbacks.onHistoryCommit(tab, tab.title, url)
                    }
                }

                override fun onPageFinished(view: WebView, url: String?) {
                    tab.currentUrl = url.orEmpty()
                    tab.title = view.title?.takeIf { it.isNotBlank() } ?: tab.domain()
                    tab.isLoading = false
                    tab.canGoBack = view.canGoBack()
                    tab.pendingUrl = null
                    WebInjector.inject(view, callbacks.monochromeEnabled())
                    callbacks.onStateChanged(tab)
                }

                override fun onReceivedError(
                    view: WebView,
                    request: WebResourceRequest,
                    error: WebResourceError
                ) {
                    if (!request.isForMainFrame) return
                    tab.errorCode = mapError(error.errorCode, error.description.toString())
                    tab.isLoading = false
                    callbacks.onStateChanged(tab)
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView, newProgress: Int) {
                    tab.progress = newProgress
                    tab.isLoading = newProgress < 100
                    if (newProgress > 10) WebInjector.inject(view, callbacks.monochromeEnabled())
                    callbacks.onStateChanged(tab)
                }

                override fun onReceivedTitle(view: WebView, title: String?) {
                    tab.title = title?.takeIf { it.isNotBlank() } ?: tab.domain()
                    callbacks.onStateChanged(tab)
                }
            }
            setDownloadListener(DownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
                callbacks.onDownload(tab, url, userAgent, contentDisposition, mimeType)
            })
            tab.webView = this
            tab.pendingUrl?.takeIf { it.isNotBlank() }?.let(::loadUrl)
        }
    }

    fun applyDesktopMode(tab: BrowserTab, enabled: Boolean) {
        tab.desktopMode = enabled
        tab.webView?.let { web ->
            web.settings.userAgentString = if (enabled) desktopUserAgent(web.settings.userAgentString)
            else WebSettings.getDefaultUserAgent(web.context)
            web.reload()
        }
    }

    fun retry(tab: BrowserTab) {
        tab.errorCode = null
        tab.webView?.let { if (tab.currentUrl.isNotBlank()) it.loadUrl(tab.currentUrl) else it.reload() }
    }

    fun destroy(tab: BrowserTab) {
        tab.webView?.let { web ->
            web.stopLoading()
            web.setWebChromeClient(null)
            web.webViewClient = WebViewClient()
            web.clearHistory()
            if (tab.incognito) web.clearCache(true)
            web.removeAllViews()
            web.destroy()
        }
        tab.webView = null
        tab.snapshot?.recycle()
        tab.snapshot = null
    }

    private fun desktopUserAgent(mobileAgent: String): String {
        val base = mobileAgent.replace(Regex(";? Android [^;)]+"), "")
            .replace(" Mobile", "")
        return "$base X11; Linux x86_64"
    }

    private fun mapError(code: Int, description: String): String = when (code) {
        WebViewClient.ERROR_HOST_LOOKUP -> "ERR_NAME_NOT_RESOLVED"
        WebViewClient.ERROR_CONNECT -> "ERR_CONNECTION_REFUSED"
        WebViewClient.ERROR_IO -> "ERR_NETWORK_IO"
        WebViewClient.ERROR_TIMEOUT -> "ТАЙМАУТ СОЕДИНЕНИЯ"
        WebViewClient.ERROR_REDIRECT_LOOP -> "ERR_TOO_MANY_REDIRECTS"
        WebViewClient.ERROR_UNSUPPORTED_SCHEME -> "ERR_UNKNOWN_URL_SCHEME"
        WebViewClient.ERROR_FAILED_SSL_HANDSHAKE -> "ERR_SSL_PROTOCOL_ERROR"
        WebViewClient.ERROR_BAD_URL -> "ERR_INVALID_URL"
        WebViewClient.ERROR_FILE -> "ERR_FILE_NOT_FOUND"
        WebViewClient.ERROR_FILE_NOT_FOUND -> "ERR_FILE_NOT_FOUND"
        WebViewClient.ERROR_TOO_MANY_REQUESTS -> "ERR_TOO_MANY_REQUESTS"
        WebViewClient.ERROR_UNSAFE_RESOURCE -> "ERR_UNSAFE_RESOURCE"
        WebViewClient.ERROR_PROXY_AUTHENTICATION -> "ERR_PROXY_AUTH_FAILED"
        else -> description.ifBlank { "ERR_NETWORK_$code" }
    }
}
