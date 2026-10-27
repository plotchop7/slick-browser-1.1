package com.slick.browser.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slick.browser.BrowserViewModel
import com.slick.browser.ui.components.LiquidFrame
import com.slick.browser.ui.components.OmniGestureActions
import com.slick.browser.ui.components.omniGestures
import com.slick.browser.ui.theme.LocalSlickColors
import kotlin.math.roundToInt

@Composable
fun ZeroPoint(
    viewModel: BrowserViewModel,
    query: String,
    onQueryChange: (String) -> Unit,
    focused: Boolean,
    onFocused: (Boolean) -> Unit,
    editActive: Boolean = false,
    gestures: OmniGestureActions,
    modifier: Modifier = Modifier
) {
    val colors = LocalSlickColors.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val imeBottom = WindowInsets.ime.getBottom(density)
    val keyboardVisible = imeBottom > 0
    val appear = remember { Animatable(0f) }
    var logoDrag by remember { mutableFloatStateOf(0f) }
    var logoReleased by remember { mutableStateOf(false) }
    val logoAlpha by animateFloatAsState(if (logoReleased) 0.35f else 1f, tween(260), label = "logoAlpha")
    val cursorAlpha = if (viewModel.effectiveEnergySaver) 1f else rememberInfiniteTransition(label = "cursor")
        .animateFloat(1f, 0f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "blink").value

    LaunchedEffect(Unit) { appear.animateTo(1f, tween(650)) }
    BoxWithConstraints(
        modifier.fillMaxSize().omniGestures(gestures).graphicsLayer {
            alpha = appear.value
            scaleX = 0.96f + appear.value * 0.04f
            scaleY = scaleX
        }
    ) {
        val hideLogo = keyboardVisible && configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE &&
            maxHeight < 360.dp
        val logoAlphaAnimated by animateFloatAsState(if (hideLogo) 0f else 1f, tween(260), label = "orientationLogo")
        val searchY by animateFloatAsState(
            targetValue = if (focused || keyboardVisible) maxHeight.value - 96f else maxHeight.value / 2f + 24f,
            animationSpec = tween(if (viewModel.effectiveEnergySaver) 1 else 460),
            label = "searchPosition"
        )
        val logoY = maxHeight.value / 2f - 116f

        androidx.compose.material3.Text(
            text = "slick",
            color = if (viewModel.isIncognito) Color.White else colors.foreground,
            style = androidx.compose.material3.MaterialTheme.typography.displayLarge.copy(
                brush = if (viewModel.liquidMode == com.slick.browser.data.LiquidMode.LIQUID && !viewModel.isIncognito)
                    androidx.compose.ui.graphics.Brush.linearGradient(colors.liquid) else null
            ),
            fontSize = 104.sp,
            fontWeight = FontWeight.Black,
            fontStyle = FontStyle.Italic,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset { IntOffset(logoDrag.roundToInt(), with(density) { logoY.dp.roundToPx() }) }
                .graphicsLayer { alpha = logoAlpha * logoAlphaAnimated }
                .pointerInput(viewModel.isIncognito) {
                    detectTapGestures(
                        onTap = { viewModel.armIncognito() },
                        onLongPress = { viewModel.settingsOpen = true }
                    )
                }
                .pointerInput(viewModel.isIncognito) {
                    detectHorizontalDragGestures(
                        onDragStart = { logoReleased = false },
                        onHorizontalDrag = { change, amount ->
                            change.consume()
                            logoDrag = (logoDrag + amount).coerceIn(-180f, 180f)
                        },
                        onDragEnd = {
                            logoReleased = viewModel.completeIncognitoGesture(if (logoDrag < 0f) -1 else 1)
                            logoDrag = 0f
                        },
                        onDragCancel = { logoDrag = 0f }
                    )
                }
        )
        if (viewModel.isIncognito) {
            androidx.compose.material3.Text(
                "PRIVATE / WHITE SPECTRUM", color = Color.White, fontSize = 9.sp,
                modifier = Modifier.align(Alignment.TopCenter).offset(y = (logoY + 100f).dp).alpha(logoAlphaAnimated)
            )
        }

        LiquidFrame(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset { IntOffset(0, with(density) { searchY.dp.roundToPx() }) }
                .fillMaxWidth(0.88f)
                .widthIn(max = 460.dp)
                .height(60.dp),
            cornerRadius = 30.dp,
            liquidMode = viewModel.liquidMode,
            energySaver = viewModel.effectiveEnergySaver,
            aura = true,
            waveNonce = viewModel.waveNonce
        ) {
            Box(
                Modifier.fillMaxSize().padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                SlickSearchField(
                    value = query,
                    onValueChange = onQueryChange,
                    focused = focused,
                    onFocused = onFocused,
                    onSubmit = {
                        viewModel.submit(query, editActive)
                        onFocused(false)
                    },
                    incognito = viewModel.isIncognito,
                    modifier = Modifier.fillMaxWidth()
                )
                if (query.isEmpty() && !focused) {
                    Box(Modifier.size(3.dp, 22.dp).alpha(cursorAlpha).padding()) {
                        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                            drawRoundRect(colors.foreground, cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()))
                        }
                    }
                }
            }
        }
    }
}
