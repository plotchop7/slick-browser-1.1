package com.slick.browser.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slick.browser.ui.theme.LocalSlickColors

@Composable
fun SlickIcon(icon: ImageVector, description: String, modifier: Modifier = Modifier) {
    Icon(icon, description, modifier.size(20.dp), tint = LocalSlickColors.current.foreground)
}

@Composable
fun SlickTextButton(text: String, modifier: Modifier = Modifier, selected: Boolean = false, onClick: () -> Unit) {
    val colors = LocalSlickColors.current
    Box(
        modifier
            .clip(CircleShape)
            .background(if (selected) colors.foreground else Color.Transparent)
            .border(1.dp, colors.foreground, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text, color = if (selected) colors.background else colors.foreground,
            fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SettingToggle(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val colors = LocalSlickColors.current
    Row(
        Modifier.fillMaxWidth().border(1.dp, colors.foreground.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, Modifier.weight(1f), fontSize = 12.sp, color = colors.foreground)
        Switch(
            checked = checked, onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.background, checkedTrackColor = colors.foreground,
                uncheckedThumbColor = colors.foreground, uncheckedTrackColor = colors.background,
                uncheckedBorderColor = colors.foreground
            )
        )
    }
}

@Composable
fun DownloadButton(progress: Float, onClick: () -> Unit) {
    val colors = LocalSlickColors.current
    Box(
        Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(18.dp))
            .border(1.dp, colors.foreground, RoundedCornerShape(18.dp)).clickable(onClick = onClick)
    ) {
        if (progress > 0f) {
            Box(
                Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(48.dp)
                    .background(Brush.horizontalGradient(colors.liquid))
            )
        }
        Row(
            Modifier.matchParentSize().padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SlickIcon(Icons.Rounded.Download, "Загрузки")
            Text("Загрузки", color = colors.foreground, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            if (progress > 0f) {
                Text("${(progress * 100).toInt()}%", color = colors.foreground, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun CardActions(
    onSave: (() -> Unit)?,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    LiquidFrame(cornerRadius = 20.dp, borderWidth = 1.dp) {
        Row(
            Modifier.background(LocalSlickColors.current.background, RoundedCornerShape(20.dp)).padding(2.dp),
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            if (onDelete != null) ActionIcon(Icons.Rounded.DeleteOutline, "Удалить", onDelete)
            if (onSave != null) ActionIcon(Icons.Rounded.BookmarkBorder, "Сохранить", onSave)
            ActionIcon(Icons.Rounded.ContentCopy, "Копировать ссылку", onCopy)
            ActionIcon(Icons.Rounded.Share, "Поделиться", onShare)
        }
    }
}

@Composable
fun ActionIcon(icon: ImageVector, description: String, onClick: () -> Unit) {
    IconButton(onClick, Modifier.size(42.dp)) { SlickIcon(icon, description) }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text.uppercase(), color = LocalSlickColors.current.muted, fontSize = 10.sp,
        fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp
    )
}

@Composable
fun ModalCard(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = LocalSlickColors.current
    androidx.compose.ui.window.Dialog(onDismiss) {
        LiquidFrame(cornerRadius = 28.dp, modifier = modifier, waveNonce = 1L) {
            Column(
                Modifier.background(colors.background, RoundedCornerShape(28.dp)).padding(20.dp),
                content = content
            )
        }
    }
}
