package com.slick.browser.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slick.browser.BrowserViewModel
import com.slick.browser.data.BrowserTheme
import com.slick.browser.data.LiquidMode
import com.slick.browser.data.SearchEngine
import com.slick.browser.data.SettingsSection
import com.slick.browser.ui.components.DownloadButton
import com.slick.browser.ui.components.ModalCard
import com.slick.browser.ui.components.SectionTitle
import com.slick.browser.ui.components.SettingToggle
import com.slick.browser.ui.components.SlickTextButton
import com.slick.browser.ui.theme.LocalSlickColors

@Composable
fun SettingsPanel(viewModel: BrowserViewModel) {
    if (!viewModel.settingsOpen) return
    val colors = LocalSlickColors.current
    ModalCard(
        onDismiss = { viewModel.settingsOpen = false },
        modifier = Modifier.fillMaxWidth(0.92f).widthIn(max = 420.dp).heightIn(max = 690.dp)
    ) {
        Text("SLICK / НАСТРОЙКИ", color = colors.foreground, fontWeight = FontWeight.Black, fontSize = 16.sp)
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(
                SettingsSection.NETWORK to "Сеть", SettingsSection.INTERFACE to "UI",
                SettingsSection.DATA to "Данные", SettingsSection.SYSTEM to "Система"
            ).forEach { (section, title) ->
                SlickTextButton(
                    title, Modifier.weight(1f), viewModel.settingsSection == section
                ) { viewModel.settingsSection = section }
            }
        }
        Spacer(Modifier.height(16.dp))
        Column(
            Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (viewModel.settingsSection) {
                SettingsSection.NETWORK -> {
                    SectionTitle("Поиск и сеть")
                    Text("Поисковая система", color = colors.foreground, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        SearchEngine.entries.forEach { engine ->
                            SlickTextButton(engine.label, selected = viewModel.searchEngine == engine) {
                                viewModel.updateSearchEngine(engine)
                            }
                        }
                    }
                    SettingToggle("Монохромный фильтр сайтов и выдачи", viewModel.monochrome) {
                        viewModel.updateMonochrome(it)
                    }
                }
                SettingsSection.INTERFACE -> {
                    SectionTitle("Интерфейс")
                    Text("Тема", color = colors.foreground, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SlickTextButton("#000 / ЧЁРНАЯ", selected = viewModel.browserTheme == BrowserTheme.BLACK) {
                            viewModel.updateTheme(BrowserTheme.BLACK)
                        }
                        SlickTextButton("#FFF / БЕЛАЯ", selected = viewModel.browserTheme == BrowserTheme.WHITE) {
                            viewModel.updateTheme(BrowserTheme.WHITE)
                        }
                    }
                    Text("Логотип и аура", color = colors.foreground, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SlickTextButton("LIQUID", selected = viewModel.liquidMode == LiquidMode.LIQUID) {
                            viewModel.updateLiquidMode(LiquidMode.LIQUID)
                        }
                        SlickTextButton("MONO", selected = viewModel.liquidMode == LiquidMode.MONO) {
                            viewModel.updateLiquidMode(LiquidMode.MONO)
                        }
                    }
                }
                SettingsSection.DATA -> {
                    SectionTitle("Данные и хранилище")
                    SlickTextButton("История браузера (${viewModel.history.size})", Modifier.fillMaxWidth()) {
                        viewModel.historyOpen = true
                    }
                    SlickTextButton("Очистить историю", Modifier.fillMaxWidth(), onClick = viewModel::clearHistory)
                    Text("Путь внутри системной папки «Загрузки»", color = colors.muted, fontSize = 10.sp)
                    OutlinedTextField(
                        value = viewModel.downloadPath,
                        onValueChange = viewModel::updateDownloadPath,
                        singleLine = true,
                        label = { Text("Папка") },
                        textStyle = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                SettingsSection.SYSTEM -> {
                    SectionTitle("Система и энергосбережение")
                    SettingToggle("Энергосбережение", viewModel.effectiveEnergySaver) {
                        if (!viewModel.systemPowerSaver) viewModel.updateEnergySaver(it)
                    }
                    Text(
                        if (viewModel.systemPowerSaver) "Автоматически включено системой Android"
                        else "Отключает блобы и пружины, замораживает фоновые вкладки и ограничивает WebView 30 FPS",
                        color = colors.muted, fontSize = 10.sp, lineHeight = 15.sp
                    )
                    if (viewModel.isIncognito) {
                        Text("FLAG_SECURE активен · сессия только в памяти", color = colors.foreground, fontSize = 10.sp)
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        DownloadButton(viewModel.downloadProgress) {
            viewModel.downloadsOpen = true
            viewModel.refreshDownloads()
        }
    }
}
