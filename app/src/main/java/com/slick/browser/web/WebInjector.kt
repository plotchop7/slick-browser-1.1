package com.slick.browser.web

import android.webkit.WebView

object WebInjector {
    private val monochromeScript = """
        (() => {
          if (window.__slickMonoInstalled) { window.__slickMonoSweep && window.__slickMonoSweep(); return; }
          window.__slickMonoInstalled = true;
          const style = document.createElement('style');
          style.id = 'slick-monochrome-style';
          style.textContent = `
            html { filter: grayscale(100%) contrast(1.13) !important; }
            img, video, canvas, svg { filter: grayscale(100%) contrast(1.12) !important; }
            [role="navigation"] a[href*="tbm="], [role="navigation"] a[href*="/images"],
            a[href*="tbm=isch"], a[href*="tbm=shop"], a[href*="tbm=nws"], a[href*="tbm=vid"],
            a[href*="/search/images"], a[href*="/search/video"], a[href*="/search/maps"],
            .search-filter, .services-nav, .serp-controller__tabs { display: none !important; }
            .slick-video-placeholder { position:relative; display:flex !important; align-items:center;
              justify-content:center; min-height:120px; background:#111 !important; border:1px solid #777 !important;
              overflow:hidden; filter:grayscale(100%) !important; }
            .slick-video-placeholder svg { width:52px; height:52px; filter:grayscale(100%); }
          `;
          (document.head || document.documentElement).appendChild(style);
          const isVideo = (node) => /youtube\.|youtu\.be|vimeo\.|rutube\.|dailymotion\./i.test(node.href || '');
          const sweep = () => {
            document.querySelectorAll('a').forEach(a => {
              if (!isVideo(a) || a.dataset.slickVideo === 'done') return;
              const images = a.querySelectorAll('img');
              if (!images.length) return;
              a.dataset.slickVideo = 'done';
              const box = document.createElement('span');
              box.className = 'slick-video-placeholder';
              const letter = (new URL(a.href).hostname.replace(/^www\./, '')[0] || 'S').toUpperCase();
              box.innerHTML = '<svg viewBox="0 0 52 52" aria-hidden="true"><rect x="1" y="1" width="50" height="50" rx="14" fill="none" stroke="#fff"/><text x="26" y="34" text-anchor="middle" font-family="monospace" font-size="24" fill="#fff">' + letter + '</text></svg>';
              const rect = images[0].getBoundingClientRect();
              if (rect.height > 60) box.style.height = rect.height + 'px';
              images.forEach(img => img.style.display = 'none');
              a.prepend(box);
            });
          };
          window.__slickMonoSweep = sweep;
          sweep();
          let queued = false;
          const observer = new MutationObserver(() => {
            if (queued) return;
            queued = true;
            requestAnimationFrame(() => { sweep(); queued = false; });
          });
          observer.observe(document.documentElement, {childList:true, subtree:true});
        })();
    """.trimIndent()

    fun limitFrameRate(webView: WebView, enabled: Boolean) {
        val script = if (enabled) {
            """
            (() => {
              if (window.__slickOriginalRaf) return;
              window.__slickOriginalRaf = window.requestAnimationFrame.bind(window);
              window.requestAnimationFrame = callback => setTimeout(() => callback(performance.now()), 33);
            })();
            """.trimIndent()
        } else {
            """
            (() => {
              if (!window.__slickOriginalRaf) return;
              window.requestAnimationFrame = window.__slickOriginalRaf;
              delete window.__slickOriginalRaf;
            })();
            """.trimIndent()
        }
        webView.evaluateJavascript(script, null)
    }

    fun inject(webView: WebView, enabled: Boolean) {
        if (enabled) {
            webView.evaluateJavascript(monochromeScript, null)
        } else {
            webView.evaluateJavascript(
                "document.getElementById('slick-monochrome-style')?.remove();" +
                    "document.querySelectorAll('[data-slick-video] img').forEach(i=>i.style.display='');" +
                    "document.querySelectorAll('.slick-video-placeholder').forEach(i=>i.remove());",
                null
            )
        }
    }
}
