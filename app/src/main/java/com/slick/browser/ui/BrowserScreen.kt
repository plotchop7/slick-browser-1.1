package com.slick.browser.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.ViewGroup
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.slick.browser.BrowserViewModel
import com.slick.browser.data.BrowserTab
import com.slick.browser.data.PanelState
import com.slick.browser.ui.components.LiquidFrame
import com.slick.browser.ui.components.OmniGestureActions
import com.slick.browser.ui.components.SlickIcon
import com.slick.browser.ui.components.monochromeColorFilter
import com.slick.browser.ui.components.omniGestures
import com.slick.browser.ui.components.quadraticProgress
import com.slick.browser.ui.theme.LocalSlickColors
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun BrowserScreen(viewModel: BrowserViewModel) {
    val colors = LocalSlickColors.current
    val context = LocalContext.current
    val activity = context.findActivity()
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var searchFocused by remember { mutableStateOf(false) }
    var editingActive by remember { mutableStateOf(false) }
    var verticalDrag by remember { mutableFloatStateOf(0f) }
    var horizontalTarget by remember { mutableStateOf<BrowserTab?>(null) }
    var targetDirection by remember { mutableStateOf(0) }
    var switching by remember { mutableStateOf(false) }
    val horizontalDrag = remember { Animatable(0f) }
    val refreshRotation = remember { Animatable(0f) }

    LaunchedEffect(viewModel.panel, viewModel.activeTabId) {
        val tab = viewModel.activeTab ?: return@LaunchedEffect
        if (viewModel.panel != PanelState.NONE && activity != null) viewModel.prepareSnapshot(tab, activity.window)
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(colors.background)) {
        val widthPx = with(LocalDensity.current) { maxWidth.toPx() }
        val heightPx = with(LocalDensity.current) { maxHeight.toPx() }
        val siteVisible = !viewModel.zeroPointVisible && viewModel.activeTab != null
        val frameScale by animateFloatAsState(
            when (viewModel.panel) {
                PanelState.NONE -> 1.035f
                PanelState.BOOKMARKS -> 0.78f
                PanelState.TAB_CAROUSEL -> 0.38f
                PanelState.TAB_GRID -> 0.28f
            },
            tween(if (viewModel.effectiveEnergySaver) 1 else 420), label = "siteFrame"
        )

        if (siteVisible) {
            val tab = viewModel.activeTab!!
            Box(
                Modifier.fillMaxSize().graphicsLayer {
                    scaleX = frameScale
                    scaleY = frameScale
                    translationY = when (viewModel.panel) {
                        PanelState.BOOKMARKS -> 100f
                        PanelState.TAB_CAROUSEL -> -heightPx * 0.2f
                        PanelState.TAB_GRID -> -heightPx * 0.28f
                        PanelState.NONE -> 0f
                    }
                }
            ) {
                val frozen = horizontalTarget != null && tab.snapshot != null
                if (frozen) {
                    Snapshot(tab, Modifier.fillMaxSize().offset { IntOffset(horizontalDrag.value.roundToInt(), 0) })
                    horizontalTarget?.let { target ->
                        val base = if (targetDirection > 0) widthPx else -widthPx
                        Snapshot(target, Modifier.fillMaxSize().offset {
                            IntOffset((base + horizontalDrag.value).roundToInt(), 0)
                        })
                    }
                } else {
                    WebSurface(tab, viewModel, Modifier.fillMaxSize().offset {
                        IntOffset(horizontalDrag.value.roundToInt(), 0)
                    })
                }
                tab.errorCode?.let { error -> NetworkError(error, viewModel::retryActive) }
                LiquidFrame(
                    modifier = Modifier.fillMaxSize(), cornerRadius = 26.dp, borderWidth = 1.5.dp,
                    liquidMode = viewModel.liquidMode, energySaver = viewModel.effectiveEnergySaver,
                    waveNonce = viewModel.waveNonce
                ) {}
            }
        }

        if (viewModel.zeroPointVisible) {
            val zeroGestures = OmniGestureActions(
                panel = viewModel.panel, siteMode = false,
                onTap = { searchFocused = true },
                onLongPress = { searchFocused = true },
                onVerticalDrag = { verticalDrag = it },
                onHorizontalDrag = {},
                onOpenBookmarks = { verticalDrag = 0f; viewModel.openPanel(PanelState.BOOKMARKS) },
                onOpenCarousel = { verticalDrag = 0f; viewModel.openPanel(PanelState.TAB_CAROUSEL) },
                onOpenGrid = { verticalDrag = 0f; viewModel.openPanel(PanelState.TAB_GRID) },
                onClosePanel = { viewModel.openPanel(PanelState.NONE) },
                onSwitchTab = {}, onHorizontalStart = {},
                onGestureEnd = { verticalDrag = 0f }
            )
            ZeroPoint(
                viewModel = viewModel, query = query, onQueryChange = { query = it },
                focused = searchFocused, onFocused = { searchFocused = it },
                editActive = editingActive, gestures = zeroGestures
            )
        }

        if (siteVisible) {
            val tab = viewModel.activeTab!!
            val siteGestures = OmniGestureActions(
                panel = viewModel.panel, siteMode = true,
                onTap = {
                    query = ""
                    editingActive = false
                    viewModel.showZeroPoint()
                    searchFocused = true
                },
                onLongPress = {
                    query = tab.currentUrl
                    editingActive = true
                    viewModel.showZeroPoint()
                    searchFocused = true
                },
                onVerticalDrag = { verticalDrag = it },
                onHorizontalDrag = { value ->
                    if (horizontalTarget != null) scope.launch { horizontalDrag.snapTo(value.coerceIn(-widthPx, widthPx)) }
                },
                onOpenBookmarks = { verticalDrag = 0f; viewModel.openPanel(PanelState.BOOKMARKS) },
                onOpenCarousel = { verticalDrag = 0f; viewModel.openPanel(PanelState.TAB_CAROUSEL) },
                onOpenGrid = { verticalDrag = 0f; viewModel.openPanel(PanelState.TAB_GRID) },
                onClosePanel = { viewModel.openPanel(PanelState.NONE) },
                onSwitchTab = { direction ->
                    if (horizontalTarget != null) {
                        switching = true
                        scope.launch {
                            horizontalDrag.animateTo(if (direction > 0) -widthPx else widthPx, tween(180))
                            viewModel.switchTab(direction)
                            horizontalDrag.snapTo(0f)
                            horizontalTarget = null
                            switching = false
                        }
                    }
                },
                onHorizontalStart = { direction ->
                    targetDirection = direction
                    horizontalTarget = viewModel.adjacentTab(direction)
                    if (activity != null) {
                        viewModel.prepareSnapshot(tab, activity.window)
                        horizontalTarget?.let { viewModel.prepareSnapshot(it, activity.window) }
                    }
                },
                onGestureEnd = {
                    verticalDrag = 0f
                    if (!switching) {
                        scope.launch {
                            horizontalDrag.animateTo(
                                0f,
                                if (viewModel.effectiveEnergySaver) tween(1)
                                else spring(dampingRatio = 0.72f, stiffness = 420f)
                            )
                            horizontalTarget = null
                        }
                    }
                }
            )
            val animatedProgress by quadraticProgress(tab.progress, viewModel.effectiveEnergySaver)
            val progressAlpha by animateFloatAsState(if (tab.isLoading) 1f else 0f, tween(420), label = "progressFade")
            Box(
                Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                LiquidFrame(
                    Modifier.fillMaxWidth().height(58.dp).omniGestures(siteGestures),
                    cornerRadius = 29.dp, liquidMode = viewModel.liquidMode,
                    energySaver = viewModel.effectiveEnergySaver,
                    progress = if (tab.isLoading || progressAlpha > 0f) animatedProgress else -1f,
                    progressAlpha = progressAlpha, aura = false, waveNonce = viewModel.waveNonce
                ) {
                    Row(
                        Modifier.fillMaxSize().background(colors.background.copy(alpha = 0.94f),
                            androidx.compose.foundation.shape.RoundedCornerShape(29.dp)).padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.IconButton(onClick = {
                            scope.launch {
                                refreshRotation.snapTo(0f)
                                refreshRotation.animateTo(360f, tween(if (viewModel.effectiveEnergySaver) 1 else 500))
                                viewModel.reloadActive()
                            }
                        }) {
                            SlickIcon(Icons.Rounded.Refresh, "Обновить", Modifier.graphicsLayer {
                                rotationZ = refreshRotation.value
                            })
                        }
                        Text(
                            tab.domain(), Modifier.weight(1f), color = colors.foreground,
                            fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                        androidx.compose.material3.IconButton(onClick = { viewModel.siteMenuOpen = true }) {
                            SlickIcon(Icons.Rounded.MoreVert, "Настройки сайта")
                        }
                    }
                }
            }
        }

        if (abs(verticalDrag) > 12f && viewModel.panel == PanelState.NONE) {
            Box(
                Modifier.align(if (verticalDrag > 0) Alignment.TopCenter else Alignment.BottomCenter)
                    .statusBarsPadding().navigationBarsPadding().padding(12.dp)
                    .width((36 + abs(verticalDrag) / 8f).coerceAtMost(160f).dp)
                    .height((abs(verticalDrag) / 18f).coerceIn(2f, 10f).dp)
                    .background(colors.foreground, androidx.compose.foundation.shape.CircleShape)
            )
        }

        Box(Modifier.fillMaxSize().graphicsLayer {
            translationY = when (viewModel.panel) {
                PanelState.BOOKMARKS -> verticalDrag.coerceAtMost(0f)
                PanelState.TAB_CAROUSEL, PanelState.TAB_GRID -> verticalDrag.coerceAtLeast(0f)
                PanelState.NONE -> 0f
            }
        }) {
            BookmarksPanel(viewModel)
            TabManager(viewModel)
        }
        OverflowMenu(viewModel)
        SettingsPanel(viewModel)
        DataPanels(viewModel)
    }
}

@Composable
private fun WebSurface(tab: BrowserTab, viewModel: BrowserViewModel, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            val web = tab.webView ?: com.slick.browser.web.SlickWebViewFactory.create(context, tab, viewModel)
            (web.parent as? ViewGroup)?.removeView(web)
            web
        },
        update = { web ->
            web.settings.textZoom = tab.textZoom
        }
    )
}

@Composable
private fun Snapshot(tab: BrowserTab, modifier: Modifier = Modifier) {
    val bitmap = tab.snapshot ?: return
    if (!bitmap.isRecycled) {
        Image(
            bitmap.asImageBitmap(), tab.title, modifier,
            contentScale = ContentScale.FillBounds, colorFilter = monochromeColorFilter()
        )
    }
}

@Composable
private fun NetworkError(reason: String, onRetry: () -> Unit) {
    val colors = LocalSlickColors.current
    Column(
        Modifier.fillMaxSize().background(colors.background).padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
    ) {
        Text(reason, color = colors.foreground, fontSize = 24.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        com.slick.browser.ui.components.SlickTextButton("Попробовать снова", onClick = onRetry)
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
