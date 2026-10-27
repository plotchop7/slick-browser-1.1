package com.slick.browser.ui.components

import android.graphics.BlurMaskFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.RectF
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.slick.browser.data.LiquidMode
import com.slick.browser.ui.theme.LocalSlickColors
import kotlinx.coroutines.isActive
import kotlin.math.min

@Composable
fun LiquidFrame(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 28.dp,
    borderWidth: Dp = 1.5.dp,
    progress: Float = -1f,
    progressAlpha: Float = 1f,
    liquidMode: LiquidMode = LiquidMode.LIQUID,
    energySaver: Boolean = false,
    waveNonce: Long = 0L,
    aura: Boolean = false,
    content: @Composable () -> Unit
) {
    val colors = LocalSlickColors.current
    val pulse = if (energySaver) 0f else rememberInfiniteTransition(label = "liquidPulse").animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3600, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse"
    ).value
    val wave = remember { Animatable(1f) }
    LaunchedEffect(waveNonce) {
        if (waveNonce == 0L || energySaver) return@LaunchedEffect
        wave.snapTo(0f)
        wave.animateTo(1f, tween(720, easing = LinearEasing))
    }
    val palette = if (liquidMode == LiquidMode.MONO) listOf(colors.foreground, colors.foreground) else colors.liquid
    Box(modifier) {
        Canvas(Modifier.matchParentSize()) {
            val stroke = borderWidth.toPx()
            val radius = cornerRadius.toPx()
            val inset = stroke * 2f
            val brush = Brush.sweepGradient(palette, Offset(size.width / 2f, size.height / 2f))
            if (aura && liquidMode == LiquidMode.LIQUID) {
                val auraInset = -(10.dp + 3.dp * pulse).toPx()
                val auraRect = RectF(auraInset, auraInset, size.width - auraInset, size.height - auraInset)
                val path = Path().apply { addRoundRect(auraRect, radius + 12.dp.toPx(), radius + 12.dp.toPx(), Path.Direction.CW) }
                drawIntoCanvas { canvas ->
                    val native = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = Paint.Style.STROKE
                        strokeWidth = (8.dp + 5.dp * pulse).toPx()
                        maskFilter = BlurMaskFilter((18.dp + 8.dp * pulse).toPx(), BlurMaskFilter.Blur.NORMAL)
                        shader = android.graphics.LinearGradient(
                            0f, 0f, size.width, size.height,
                            palette.map { it.copy(alpha = 0.55f + pulse * 0.25f).toArgb() }.toIntArray(),
                            null, android.graphics.Shader.TileMode.CLAMP
                        )
                    }
                    canvas.nativeCanvas.drawPath(path, native)
                }
            }
            drawRoundRect(
                color = colors.foreground,
                topLeft = Offset(inset, inset),
                size = Size(size.width - inset * 2, size.height - inset * 2),
                cornerRadius = CornerRadius(radius), style = Stroke(stroke)
            )
            drawRoundRect(
                brush = brush, alpha = if (liquidMode == LiquidMode.MONO) 0.7f else 0.95f,
                topLeft = Offset(inset, inset),
                size = Size(size.width - inset * 2, size.height - inset * 2),
                cornerRadius = CornerRadius(radius), style = Stroke(stroke * 1.35f)
            )
            if (progress >= 0f) {
                val rect = RectF(inset * 2, inset * 2, size.width - inset * 2, size.height - inset * 2)
                val path = Path().apply { addRoundRect(rect, radius, radius, Path.Direction.CW) }
                val measure = PathMeasure(path, false)
                val segment = Path()
                measure.getSegment(0f, measure.length * progress.coerceIn(0f, 1f), segment, true)
                drawIntoCanvas { canvas ->
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = Paint.Style.STROKE
                        strokeWidth = stroke * 2.2f
                        strokeCap = Paint.Cap.ROUND
                        alpha = (progressAlpha.coerceIn(0f, 1f) * 255).toInt()
                        shader = android.graphics.LinearGradient(
                            0f, 0f, size.width, 0f,
                            palette.map(Color::toArgb).toIntArray(), null, android.graphics.Shader.TileMode.CLAMP
                        )
                    }
                    canvas.nativeCanvas.drawPath(segment, paint)
                }
            }
            if (wave.value < 1f) {
                val grow = wave.value * min(size.width, size.height) * 0.42f
                drawRoundRect(
                    brush = brush,
                    alpha = (1f - wave.value) * 0.8f,
                    topLeft = Offset(inset - grow, inset - grow),
                    size = Size(size.width - inset * 2 + grow * 2, size.height - inset * 2 + grow * 2),
                    cornerRadius = CornerRadius(radius + grow),
                    style = Stroke(stroke * (1f + (1f - wave.value) * 4f))
                )
            }
        }
        content()
    }
}

@Composable
fun quadraticProgress(target: Int, energySaver: Boolean): State<Float> {
    var value by remember { mutableFloatStateOf(if (target >= 100) 0f else target / 100f) }
    LaunchedEffect(target, energySaver) {
        val destination = target.coerceIn(0, 100) / 100f
        if (energySaver || destination < value) {
            value = destination
            return@LaunchedEffect
        }
        val start = value
        val distance = destination - start
        val startTime = withFrameNanos { it }
        var previous = startTime
        var elapsed = 0f
        while (isActive && value < destination) {
            withFrameNanos { now ->
                val dt = ((now - previous) / 1_000_000_000f).coerceAtMost(0.05f)
                previous = now
                elapsed = ((now - startTime) / 1_000_000_000f)
                val velocity = 0.65f + distance * 2.8f * elapsed * elapsed
                value = (value + velocity * dt).coerceAtMost(destination)
            }
        }
        value = destination
    }
    return remember { androidx.compose.runtime.derivedStateOf { value } }
}

fun monochromeColorFilter(): ColorFilter {
    val matrix = ColorMatrix().apply { setToSaturation(0f) }
    val contrast = 1.13f
    val translate = (1f - contrast) * 127.5f
    val values = matrix.values
    for (row in 0 until 3) {
        for (column in 0 until 3) values[row * 5 + column] *= contrast
        values[row * 5 + 4] = translate
    }
    return ColorFilter.colorMatrix(ColorMatrix(values))
}
