package com.slick.browser.ui

import android.app.DownloadManager
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slick.browser.BrowserViewModel
import com.slick.browser.ui.components.ModalCard
import com.slick.browser.ui.components.SectionTitle
import com.slick.browser.ui.components.SlickTextButton
import com.slick.browser.ui.theme.LocalSlickColors
import java.text.DateFormat
import java.util.Date

@Composable
fun DataPanels(viewModel: BrowserViewModel) {
    val colors = LocalSlickColors.current
    val context = LocalContext.current
    if (viewModel.historyOpen) {
        ModalCard(
            onDismiss = { viewModel.historyOpen = false },
            modifier = Modifier.fillMaxWidth(0.94f).widthIn(max = 520.dp).heightIn(max = 650.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SectionTitle("Полная история")
                SlickTextButton("Очистить", onClick = viewModel::clearHistory)
            }
            Spacer(Modifier.height(14.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(viewModel.history, key = { it.visitedAt }) { entry ->
                    Column(Modifier.fillMaxWidth().clickable {
                        viewModel.historyOpen = false
                        viewModel.submit(entry.url)
                    }) {
                        Text(entry.title, color = colors.foreground, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(entry.url, color = colors.muted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(entry.visitedAt)),
                            color = colors.muted, fontSize = 9.sp
                        )
                    }
                }
                if (viewModel.history.isEmpty()) item { Text("История пуста", color = colors.muted) }
            }
        }
    }
    if (viewModel.downloadsOpen) {
        ModalCard(
            onDismiss = { viewModel.downloadsOpen = false },
            modifier = Modifier.fillMaxWidth(0.94f).widthIn(max = 520.dp).heightIn(max = 650.dp)
        ) {
            SectionTitle("Загрузки")
            Spacer(Modifier.height(14.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                items(viewModel.downloads, key = { it.id }) { entry ->
                    val progress = if (entry.totalBytes > 0) entry.downloadedBytes.toFloat() / entry.totalBytes else 0f
                    Column(Modifier.fillMaxWidth().clickable {
                        val manager = context.getSystemService(DownloadManager::class.java)
                        val uri = manager.getUriForDownloadedFile(entry.id)
                        if (uri != null) {
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
                            }
                        }
                    }) {
                        Text(entry.title, color = colors.foreground, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(5.dp))
                        LinearProgressIndicator(
                            progress = { progress.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(3.dp),
                            color = colors.foreground, trackColor = colors.muted.copy(alpha = 0.25f)
                        )
                        Text(
                            when (entry.status) {
                                DownloadManager.STATUS_SUCCESSFUL -> "Завершено · ${entry.destination}"
                                DownloadManager.STATUS_FAILED -> "Ошибка загрузки"
                                else -> "${formatBytes(entry.downloadedBytes)} / ${formatBytes(entry.totalBytes)}"
                            },
                            color = colors.muted, fontSize = 9.sp, maxLines = 2, overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (viewModel.downloads.isEmpty()) item { Text("Загрузок пока нет", color = colors.muted) }
            }
        }
    }
}

private fun formatBytes(value: Long): String {
    if (value < 0) return "объём уточняется"
    if (value < 1024) return "$value Б"
    if (value < 1024 * 1024) return "${value / 1024} КБ"
    return String.format("%.1f МБ", value / 1024f / 1024f)
}
