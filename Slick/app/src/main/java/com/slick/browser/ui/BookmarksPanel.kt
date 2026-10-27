package com.slick.browser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slick.browser.BrowserViewModel
import com.slick.browser.data.Bookmark
import com.slick.browser.data.PanelState
import com.slick.browser.ui.components.CardActions
import com.slick.browser.ui.components.LiquidFrame
import com.slick.browser.ui.components.SectionTitle
import com.slick.browser.ui.components.SlickIcon
import com.slick.browser.ui.theme.LocalSlickColors
import kotlin.math.roundToInt

@Composable
fun BookmarksPanel(viewModel: BrowserViewModel) {
    if (viewModel.panel != PanelState.BOOKMARKS) return
    val colors = LocalSlickColors.current
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().clickable { viewModel.openPanel(PanelState.NONE) })
        LiquidFrame(
            Modifier.align(Alignment.TopCenter).padding(10.dp).fillMaxWidth().height(190.dp),
            cornerRadius = 24.dp, liquidMode = viewModel.liquidMode,
            energySaver = viewModel.effectiveEnergySaver, waveNonce = viewModel.waveNonce
        ) {
            Column(
                Modifier.fillMaxSize().background(colors.background, RoundedCornerShape(24.dp)).padding(16.dp)
            ) {
                SectionTitle("Сохранённые")
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    viewModel.bookmarks.forEachIndexed { index, bookmark ->
                        BookmarkItem(bookmark, index, viewModel)
                    }
                    if (viewModel.bookmarks.isEmpty()) {
                        Text("Сохраняйте сайты через меню ···", color = colors.muted, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun BookmarkItem(bookmark: Bookmark, index: Int, viewModel: BrowserViewModel) {
    val colors = LocalSlickColors.current
    var actions by remember { mutableStateOf(false) }
    var dragX by remember { mutableFloatStateOf(0f) }
    Box(Modifier.width(96.dp).graphicsLayer { translationX = dragX }) {
        Column(
            Modifier.width(96.dp).pointerInput(bookmark.id) {
                detectTapGestures(
                    onTap = {
                        if (!actions) {
                            viewModel.openPanel(PanelState.NONE)
                            viewModel.submit(bookmark.url)
                        }
                    },
                    onLongPress = { actions = true }
                )
            }.pointerInput(bookmark.id, index) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { actions = false },
                    onDrag = { change, amount ->
                        change.consume()
                        dragX += amount.x
                    },
                    onDragEnd = {
                        val steps = (dragX / 106f).roundToInt()
                        viewModel.moveBookmark(bookmark.id, index + steps)
                        dragX = 0f
                    },
                    onDragCancel = { dragX = 0f }
                )
            },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier.size(60.dp).background(colors.foreground.copy(alpha = 0.08f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                SlickIcon(Icons.Rounded.Bookmark, bookmark.title, Modifier.size(25.dp))
            }
            Spacer(Modifier.height(7.dp))
            Text(
                bookmark.title, color = colors.foreground, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center
            )
        }
        if (actions) {
            Box(Modifier.align(Alignment.TopCenter).graphicsLayer { translationY = -12f }) {
                CardActions(
                    onSave = null, onDelete = { viewModel.removeBookmark(bookmark.id) },
                    onCopy = { viewModel.copyLink(bookmark.url); actions = false },
                    onShare = { viewModel.shareLink(bookmark.url); actions = false }
                )
            }
        }
    }
}
