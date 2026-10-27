package com.slick.browser.web

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.Window
import android.webkit.WebView
import com.slick.browser.data.BrowserTab
import kotlin.math.min

object SnapshotManager {
    fun capture(tab: BrowserTab, window: Window, onComplete: (() -> Unit)? = null) {
        val web = tab.webView ?: run {
            onComplete?.invoke()
            return
        }
        if (web.width <= 0 || web.height <= 0) {
            onComplete?.invoke()
            return
        }
        val scale = min(1f, 1200f / web.width.toFloat())
        val width = (web.width * scale).toInt().coerceAtLeast(1)
        val height = (web.height * scale).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val location = IntArray(2)
        web.getLocationInWindow(location)
        val source = android.graphics.Rect(
            location[0], location[1], location[0] + web.width, location[1] + web.height
        )
        runCatching {
            PixelCopy.request(window, source, bitmap, { result ->
                if (result != PixelCopy.SUCCESS) drawFallback(web, bitmap, scale)
                installSnapshot(tab, bitmap)
                onComplete?.invoke()
            }, Handler(Looper.getMainLooper()))
        }.onFailure {
            drawFallback(web, bitmap, scale)
            installSnapshot(tab, bitmap)
            onComplete?.invoke()
        }
    }

    private fun drawFallback(web: WebView, bitmap: Bitmap, scale: Float) {
        val canvas = Canvas(bitmap)
        canvas.scale(scale, scale)
        web.draw(canvas)
    }

    private fun installSnapshot(tab: BrowserTab, bitmap: Bitmap) {
        val previous = tab.snapshot
        tab.snapshot = bitmap
        if (previous != null && previous != bitmap && !previous.isRecycled) previous.recycle()
    }
}
