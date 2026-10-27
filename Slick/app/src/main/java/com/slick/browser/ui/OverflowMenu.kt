package com.slick.browser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Laptop
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.slick.browser.BrowserViewModel
import com.slick.browser.ui.components.ActionIcon
import com.slick.browser.ui.components.DownloadButton
import com.slick.browser.ui.components.LiquidFrame
import com.slick.browser.ui.components.SectionTitle
import com.slick.browser.ui.components.SlickIcon
import com.slick.browser.ui.theme.LocalSlickColors

@Composable
fun OverflowMenu(viewModel: BrowserViewModel) {
    val tab = viewModel.activeTab ?: return
    if (!viewModel.siteMenuOpen) return
    val colors = LocalSlickColors.current
    Dialog(onDismissRequest = { viewModel.siteMenuOpen = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.38f)).clickable {
            viewModel.siteMenuOpen = false
        }) {
            LiquidFrame(
                modifier = Modifier.align(Alignment.BottomCenter).padding(14.dp).navigationBarsPadding()
                    .fillMaxWidth().clickable(enabled = false) {},
                cornerRadius = 28.dp,
                liquidMode = viewModel.liquidMode,
                energySaver = viewModel.effectiveEnergySaver
            ) {
                Column(
                    Modifier.background(colors.background, RoundedCornerShape(28.dp)).padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LiquidFrame(cornerRadius = 20.dp, borderWidth = 1.dp) {
                        Row(
                            Modifier.fillMaxWidth().background(colors.background, RoundedCornerShape(20.dp)).padding(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                tab.title, Modifier.weight(1f).padding(start = 10.dp), color = colors.foreground,
                                fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                            ActionIcon(Icons.Rounded.BookmarkBorder, "Сохранить") { viewModel.addBookmark(tab) }
                            ActionIcon(Icons.Rounded.ContentCopy, "Копировать ссылку") { viewModel.copyLink(tab.currentUrl) }
                            ActionIcon(Icons.Rounded.Share, "Поделиться") { viewModel.shareLink(tab.currentUrl) }
                        }
                    }
                    DownloadButton(viewModel.downloadProgress) {
                        viewModel.downloadsOpen = true
                        viewModel.refreshDownloads()
                    }
                    MenuRow(Icons.Rounded.Laptop, "Версия для ПК") {
                        Switch(checked = tab.desktopMode, onCheckedChange = viewModel::setDesktopMode)
                    }
                    Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            SectionTitle("Размер текста на сайте")
                            Text("${tab.textZoom}%", color = colors.foreground, fontSize = 11.sp)
                        }
                        Slider(
                            value = tab.textZoom.toFloat(), onValueChange = { viewModel.setTextZoom(it.toInt()) },
                            valueRange = 30f..200f,
                            colors = SliderDefaults.colors(
                                thumbColor = colors.foreground, activeTrackColor = colors.foreground,
                                inactiveTrackColor = colors.muted.copy(alpha = 0.3f)
                            )
                        )
                    }
                    MenuRow(Icons.Rounded.History, "История") {
                        Text("›", color = colors.foreground, fontSize = 24.sp, modifier = Modifier.clickable {
                            viewModel.historyOpen = true
                        })
                    }
                    MenuRow(Icons.Rounded.Settings, "Настройки") {
                        Text("›", color = colors.foreground, fontSize = 24.sp, modifier = Modifier.clickable {
                            viewModel.settingsOpen = true
                        })
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuRow(icon: ImageVector, title: String, trailing: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
        SlickIcon(icon, title)
        Spacer(Modifier.padding(horizontal = 7.dp))
        Text(title, Modifier.weight(1f), color = LocalSlickColors.current.foreground, fontSize = 12.sp)
        trailing()
    }
}
