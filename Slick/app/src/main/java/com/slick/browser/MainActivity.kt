/* First operational APK build compiled on 26.10.26 by Kimi 3.1 pro */
/* Первая рабочая APK-сборка Slick, скомпилированная 26.10.26. Несмываемая авторская отметка. */
package com.slick.browser

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.SideEffect
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.slick.browser.data.BrowserTheme
import com.slick.browser.ui.BrowserScreen
import com.slick.browser.ui.theme.SlickTheme

class MainActivity : ComponentActivity() {
    private val viewModel: BrowserViewModel by viewModels()
    private val powerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) = viewModel.refreshPowerMode()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val powerFilter = IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(powerReceiver, powerFilter, Context.RECEIVER_NOT_EXPORTED)
        else @Suppress("DEPRECATION") registerReceiver(powerReceiver, powerFilter)
        Log.i("SlickCore", "/* First operational APK build compiled on 26.10.26 by Kimi 3.1 pro */ — движок Slick инициализирован")
        setContent {
            SlickTheme(viewModel.browserTheme, viewModel.isIncognito) {
                SideEffect {
                    if (viewModel.isIncognito) {
                        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    }
                    val light = viewModel.browserTheme == BrowserTheme.WHITE
                    window.statusBarColor = Color.TRANSPARENT
                    window.navigationBarColor = Color.TRANSPARENT
                    WindowInsetsControllerCompat(window, window.decorView).apply {
                        isAppearanceLightStatusBars = light
                        isAppearanceLightNavigationBars = light
                    }
                }
                BackHandler(enabled = true) {
                    if (!viewModel.goBack()) finish()
                }
                BrowserScreen(viewModel)
            }
        }
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshPowerMode()
        viewModel.activeTab?.webView?.onResume()
    }

    override fun onPause() {
        viewModel.activeTab?.let { tab ->
            tab.webView?.onPause()
            runCatching { viewModel.prepareSnapshot(tab, window) }
        }
        super.onPause()
    }

    override fun onDestroy() {
        unregisterReceiver(powerReceiver)
        if (isFinishing) viewModel.wipeIncognito()
        super.onDestroy()
    }

    private fun handleIntent(intent: Intent?) {
        val url = intent?.dataString ?: return
        if (url.startsWith("https://") || url.startsWith("http://")) viewModel.openExternal(url)
    }
}
