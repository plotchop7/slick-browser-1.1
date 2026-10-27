package com.slick.browser.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Typography
import com.slick.browser.data.BrowserTheme

@Immutable
data class SlickColors(
    val background: Color,
    val foreground: Color,
    val muted: Color,
    val liquid: List<Color>
)

val LocalSlickColors = staticCompositionLocalOf {
    SlickColors(Color.Black, Color.White, Color(0xFF8B8B8B), listOf(Color.White))
}

@Composable
fun SlickTheme(theme: BrowserTheme, incognito: Boolean, content: @Composable () -> Unit) {
    val black = theme == BrowserTheme.BLACK
    val background = if (black) Color(0xFF000000) else Color(0xFFFFFFFF)
    val foreground = if (black) Color(0xFFFFFFFF) else Color(0xFF000000)
    val liquid = if (incognito) {
        listOf(Color.White, Color(0xFF777777), Color.White, Color(0xFFBBBBBB), Color.White)
    } else {
        listOf(
            Color(0xFF8B5CF6), Color(0xFF06B6D4), Color(0xFFF472B6),
            Color(0xFFF8FAFC), Color(0xFF6366F1), Color(0xFF8B5CF6)
        )
    }
    val colors = SlickColors(background, foreground, if (black) Color(0xFF9D9D9D) else Color(0xFF666666), liquid)
    val scheme = if (black) darkColorScheme(
        background = background, surface = background, onBackground = foreground,
        onSurface = foreground, primary = foreground, onPrimary = background,
        surfaceVariant = Color(0xFF111111), onSurfaceVariant = foreground
    ) else lightColorScheme(
        background = background, surface = background, onBackground = foreground,
        onSurface = foreground, primary = foreground, onPrimary = background,
        surfaceVariant = Color(0xFFF2F2F2), onSurfaceVariant = foreground
    )
    val typography = Typography(
        displayLarge = androidx.compose.ui.text.TextStyle(
            fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Black,
            fontStyle = FontStyle.Italic, fontSize = 104.sp, letterSpacing = (-3).sp
        ),
        bodyMedium = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
        labelLarge = androidx.compose.ui.text.TextStyle(
            fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp
        )
    )
    CompositionLocalProvider(LocalSlickColors provides colors) {
        MaterialTheme(colorScheme = scheme, typography = typography, content = content)
    }
}
