package com.slick.browser.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import com.slick.browser.data.PanelState
import kotlin.math.abs

class OmniGestureActions(
    val panel: PanelState,
    val siteMode: Boolean,
    val onTap: () -> Unit,
    val onLongPress: () -> Unit,
    val onVerticalDrag: (Float) -> Unit,
    val onHorizontalDrag: (Float) -> Unit,
    val onOpenBookmarks: () -> Unit,
    val onOpenCarousel: () -> Unit,
    val onOpenGrid: () -> Unit,
    val onClosePanel: () -> Unit,
    val onSwitchTab: (Int) -> Unit,
    val onHorizontalStart: (Int) -> Unit,
    val onGestureEnd: () -> Unit
)

@Composable
fun Modifier.omniGestures(actions: OmniGestureActions): Modifier {
    val current by rememberUpdatedState(actions)
    return pointerInput(actions.siteMode) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            val start = down.position
            val downTime = down.uptimeMillis
            var axis: Char? = null
            var longPressed = false
            var opened = false
            var holdStartedAt = 0L
            var holdDirection = 0
            var horizontalTarget = 0
            var horizontalReady = false
            var horizontalHoldAt = 0L
            var total = Offset.Zero

            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (!change.pressed) {
                    val elapsed = change.uptimeMillis - downTime
                    when {
                        longPressed -> Unit
                        axis == null && total.getDistance() < viewConfiguration.touchSlop && elapsed < 500 -> current.onTap()
                        axis == 'v' -> {
                            when (current.panel) {
                                PanelState.BOOKMARKS -> if (total.y < -35f) current.onClosePanel()
                                PanelState.TAB_CAROUSEL -> when {
                                    total.y < -55f -> current.onOpenGrid()
                                    total.y > 35f -> current.onClosePanel()
                                }
                                PanelState.TAB_GRID -> if (total.y > 45f) current.onOpenCarousel()
                                PanelState.NONE -> if (opened && ((holdDirection > 0 && total.y < 0f) ||
                                        (holdDirection < 0 && total.y > 0f))) current.onClosePanel()
                            }
                        }
                        axis == 'h' && current.siteMode -> {
                            if (horizontalReady && change.uptimeMillis - horizontalHoldAt >= 200L) {
                                current.onSwitchTab(if (total.x < 0f) 1 else -1)
                            }
                        }
                    }
                    current.onGestureEnd()
                    change.consume()
                    break
                }

                total = change.position - start
                val now = change.uptimeMillis
                if (axis == null) {
                    if (total.getDistance() > viewConfiguration.touchSlop) {
                        axis = if (abs(total.x) > abs(total.y)) 'h' else 'v'
                        if (axis == 'h' && current.siteMode) {
                            horizontalTarget = if (total.x < 0f) 1 else -1
                            current.onHorizontalStart(horizontalTarget)
                        }
                    } else if (!longPressed && now - downTime >= 500L) {
                        longPressed = true
                        current.onLongPress()
                    }
                }

                if (axis == 'v') {
                    current.onVerticalDrag(total.y)
                    when (current.panel) {
                        PanelState.NONE -> {
                            val direction = when {
                                total.y > 55f -> 1
                                total.y < -55f -> -1
                                else -> 0
                            }
                            if (direction != holdDirection) {
                                holdDirection = direction
                                holdStartedAt = if (direction == 0) 0L else now
                                opened = false
                            }
                            if (direction != 0 && !opened && now - holdStartedAt >= 200L) {
                                opened = true
                                if (direction > 0) current.onOpenBookmarks() else current.onOpenCarousel()
                            }
                        }
                        else -> Unit
                    }
                } else if (axis == 'h' && current.siteMode) {
                    current.onHorizontalDrag(total.x)
                    val direction = if (total.x < 0f) 1 else -1
                    if (abs(total.x) < 55f || direction != horizontalTarget) {
                        horizontalReady = false
                        horizontalHoldAt = 0L
                    } else {
                        if (!horizontalReady) horizontalHoldAt = now
                        horizontalReady = true
                    }
                }
                if (axis != null || longPressed) change.consume()
            }
        }
    }
}
