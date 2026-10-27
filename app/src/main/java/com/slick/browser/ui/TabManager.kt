package com.slick.browser.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slick.browser.BrowserViewModel
import com.slick.browser.data.BrowserTab
import com.slick.browser.data.PanelState
import com.slick.browser.ui.components.CardActions
import com.slick.browser.ui.components.LiquidFrame
import com.slick.browser.ui.components.SectionTitle
import com.slick.browser.ui.components.monochromeColorFilter
import com.slick.browser.ui.theme.LocalSlickColors
import kotlinx.coroutines.launch

@Composable
fun TabManager(viewModel: BrowserViewModel) {
    if (viewModel.panel != PanelState.TAB_CAROUSEL && viewModel.panel != PanelState.TAB_GRID) return
    val colors = LocalSlickColors.current
    val landscape = LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val grid = viewModel.panel == PanelState.TAB_GRID
    Box(Modifier.fillMaxSize().background(if (grid) colors.background.copy(alpha = 0.94f) else androidx.compose.ui.graphics.Color.Transparent)) {
        if (!grid) Box(Modifier.fillMaxSize().pointerInput(Unit) {
            detectTapGestures(onTap = { viewModel.openPanel(PanelState.NONE) })
        })
        LiquidFrame(
            if (grid) Modifier.fillMaxSize().padding(10.dp)
            else Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(0.52f).padding(10.dp),
            cornerRadius = 24.dp,
            liquidMode = viewModel.liquidMode, energySaver = viewModel.effectiveEnergySaver,
            waveNonce = viewModel.waveNonce
        ) {
            Column(Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 18.dp)) {
                SectionTitle(if (viewModel.panel == PanelState.TAB_GRID) "Все вкладки" else "Карусель вкладок")
                Spacer(Modifier.height(10.dp))
                if (viewModel.tabs.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Нет открытых вкладок", color = colors.muted)
                    }
                } else if (viewModel.panel == PanelState.TAB_CAROUSEL) {
                    Row(
                        Modifier.fillMaxSize().horizontalScroll(rememberScrollState()).padding(vertical = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        viewModel.tabs.forEach { tab ->
                            TabCard(
                                tab = tab, active = tab.id == viewModel.activeTabId,
                                modifier = Modifier.width(if (landscape) 230.dp else 270.dp)
                                    .height(if (landscape) 270.dp else 430.dp),
                                carousel = true, viewModel = viewModel
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(if (landscape) 3 else 2),
                        contentPadding = PaddingValues(bottom = 28.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(viewModel.tabs, key = { it.id }) { tab ->
                            TabCard(
                                tab = tab, active = tab.id == viewModel.activeTabId,
                                modifier = Modifier.fillMaxWidth().height(if (landscape) 180.dp else 265.dp),
                                carousel = false, viewModel = viewModel
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabCard(
    tab: BrowserTab,
    active: Boolean,
    carousel: Boolean,
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalSlickColors.current
    var actions by remember { mutableStateOf(false) }
    var dragX by remember { mutableStateOf(0f) }
    var dragY by remember { mutableStateOf(0f) }
    val stretch = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    Box(modifier.graphicsLayer {
        translationX = dragX
        translationY = dragY
        scaleX = stretch.value
        scaleY = stretch.value
    }) {
        LiquidFrame(
            modifier = Modifier.fillMaxSize(), cornerRadius = 18.dp,
            borderWidth = if (active) 2.dp else 1.dp,
            liquidMode = viewModel.liquidMode, energySaver = viewModel.effectiveEnergySaver
        ) {
            Column(
                Modifier.fillMaxSize().clip(RoundedCornerShape(18.dp)).background(colors.background)
                    .pointerInput(tab.id, carousel) {
                        detectTapGestures(
                            onTap = { if (!actions) viewModel.selectTab(tab.id) },
                            onLongPress = { actions = true }
                        )
                    }
                    .pointerInput(tab.id, carousel) {
                        if (carousel) {
                            detectVerticalDragGestures(
                                onVerticalDrag = { change, amount ->
                                    change.consume()
                                    dragY += amount
                                },
                                onDragEnd = {
                                    when {
                                        dragY > 70f -> scope.launch {
                                            stretch.animateTo(4f, tween(if (viewModel.effectiveEnergySaver) 1 else 260))
                                            viewModel.selectTab(tab.id)
                                            stretch.snapTo(1f)
                                            dragY = 0f
                                        }
                                        dragY < -65f -> viewModel.closeTab(tab.id)
                                        else -> dragY = 0f
                                    }
                                },
                                onDragCancel = { dragY = 0f }
                            )
                        } else {
                            detectHorizontalDragGestures(
                                onHorizontalDrag = { change, amount ->
                                    change.consume()
                                    dragX += amount
                                },
                                onDragEnd = {
                                    if (kotlin.math.abs(dragX) > 80f) viewModel.closeTab(tab.id) else dragX = 0f
                                },
                                onDragCancel = { dragX = 0f }
                            )
                        }
                    }
            ) {
                Box(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(topStart = 17.dp, topEnd = 17.dp))) {
                    val bitmap = tab.snapshot
                    if (bitmap != null && !bitmap.isRecycled) {
                        Image(
                            bitmap = bitmap.asImageBitmap(), contentDescription = tab.title,
                            modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop,
                            colorFilter = monochromeColorFilter()
                        )
                    } else TabSkeleton()
                }
                Text(
                    tab.title.ifBlank { tab.domain() }, color = colors.foreground,
                    fontSize = 11.sp, fontWeight = if (active) FontWeight.Black else FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp)
                )
            }
        }
        if (actions) {
            Box(Modifier.align(Alignment.TopCenter).padding(top = 10.dp)) {
                CardActions(
                    onSave = { viewModel.addBookmark(tab); actions = false },
                    onCopy = { viewModel.copyLink(tab.currentUrl); actions = false },
                    onShare = { viewModel.shareLink(tab.currentUrl); actions = false }
                )
            }
        }
    }
}

@Composable
private fun TabSkeleton() {
    val colors = LocalSlickColors.current
    Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.fillMaxWidth().height(90.dp).background(colors.muted.copy(alpha = 0.18f)))
        listOf(0.92f, 0.72f, 0.84f, 0.48f).forEach { width ->
            Box(Modifier.fillMaxWidth(width).height(8.dp).background(colors.muted.copy(alpha = 0.3f)))
        }
    }
}
