package me.lucid.domchat.ui.components.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.launch
import me.lucid.domchat.ui.component.backdrop.backdrops.layerBackdrop
import me.lucid.domchat.ui.component.backdrop.backdrops.rememberLayerBackdrop
import me.lucid.domchat.ui.component.backdrop.drawBackdrop
import me.lucid.domchat.ui.component.backdrop.effects.blur
import me.lucid.domchat.ui.component.backdrop.effects.lens
import me.lucid.domchat.ui.component.backdrop.highlight.Highlight
import me.lucid.domchat.ui.component.backdrop.isRuntimeShaderSupported
import me.lucid.domchat.ui.component.backdrop.shadow.InnerShadow
import me.lucid.domchat.ui.component.backdrop.shadow.Shadow
import me.lucid.domchat.ui.hooks.HapticPattern
import me.lucid.domchat.ui.hooks.rememberPremiumHaptics
import kotlin.math.abs

/**
 * Liquid Glass Style Switch (`DomChatGlassSwitch`).
 *
 * Adapted from Convx's CupertinoSwitch + DampedDragAnimation:
 * - Capsule track wrapped in `.layerBackdrop(trackBackdrop)`.
 * - Resting thumb is a crisp white pill with subtle shadow.
 * - While pressed/dragged on API 33+, expands smoothly and transitions into live
 *   refractive liquid glass with AGSL lens refraction, ambient highlight, and inner shadow.
 * - On API < 33, falls back smoothly to translucent specular pill.
 * - Supports crisp tap toggling, interactive thumb dragging with spring settle, and premium haptics.
 */
@Composable
fun DomChatGlassSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    thumbContent: (@Composable () -> Unit)? = null
) {
    val currentChecked by rememberUpdatedState(checked)
    val currentOnCheckedChange by rememberUpdatedState(onCheckedChange)
    val density = LocalDensity.current
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val isDark = isSystemInDarkTheme()
    val haptics = rememberPremiumHaptics()
    val scope = rememberCoroutineScope()

    val trackWidthDp = 60.dp
    val trackHeightDp = 28.dp
    val thumbWidthDp = 36.dp
    val thumbHeightDp = 24.dp
    val paddingDp = 2.dp
    val dragWidthDp = trackWidthDp - thumbWidthDp - (paddingDp * 2) // 20.dp
    val dragWidthPx = with(density) { dragWidthDp.toPx() }

    val fractionAnim = remember { Animatable(if (checked) 1f else 0f) }
    val pressProgressAnim = remember { Animatable(0f) }

    LaunchedEffect(checked) {
        val target = if (checked) 1f else 0f
        if (fractionAnim.targetValue != target) {
            fractionAnim.animateTo(target, spring(dampingRatio = 0.85f, stiffness = 500f))
        }
    }

    val trackBackdrop = rememberLayerBackdrop()
    val uncheckedTrackColor = Color.White.copy(alpha = if (isDark) 0.16f else 0.12f)
    val themePrimary = MaterialTheme.colorScheme.primary
    val checkedTrackColor = if (themePrimary.luminance() > 0.82f) {
        Color(0xFF34C759)
    } else {
        themePrimary
    }
    val useRuntimeGlass = isRuntimeShaderSupported()

    Box(
        modifier = modifier
            .semantics {
                role = Role.Switch
                toggleableState = ToggleableState(checked)
            }
            .then(
                if (enabled && onCheckedChange != null) {
                    Modifier.pointerInput(enabled) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val downId = down.id
                            var isDrag = false
                            var totalDragX = 0f

                            scope.launch {
                                pressProgressAnim.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = 600f))
                            }

                            try {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.find { it.id == downId } ?: break
                                    if (change.changedToUp()) {
                                        change.consume()
                                        break
                                    }
                                    val dragDelta = change.positionChange()
                                    totalDragX += dragDelta.x
                                    if (abs(totalDragX) > viewConfiguration.touchSlop) {
                                        isDrag = true
                                    }
                                    if (isDrag) {
                                        change.consume()
                                        val deltaFraction = if (isLtr) {
                                            dragDelta.x / dragWidthPx
                                        } else {
                                            -dragDelta.x / dragWidthPx
                                        }
                                        val newFraction = (fractionAnim.value + deltaFraction).coerceIn(0f, 1f)
                                        scope.launch { fractionAnim.snapTo(newFraction) }
                                    }
                                }
                            } finally {
                                scope.launch {
                                    pressProgressAnim.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = 600f))
                                }
                            }

                            if (isDrag) {
                                val newChecked = fractionAnim.value >= 0.5f
                                val target = if (newChecked) 1f else 0f
                                if (newChecked != currentChecked) {
                                    haptics.perform(HapticPattern.Pop)
                                    currentOnCheckedChange?.invoke(newChecked)
                                }
                                scope.launch {
                                    fractionAnim.animateTo(target, spring(dampingRatio = 0.85f, stiffness = 400f))
                                }
                            } else {
                                val newChecked = !currentChecked
                                haptics.perform(HapticPattern.Pop)
                                currentOnCheckedChange?.invoke(newChecked)
                                scope.launch {
                                    fractionAnim.animateTo(if (newChecked) 1f else 0f, spring(dampingRatio = 0.85f, stiffness = 400f))
                                }
                            }
                        }
                    }
                } else Modifier
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        // Track with backdrop recording
        Box(
            modifier = Modifier
                .layerBackdrop(trackBackdrop)
                .clip(CircleShape)
                .drawBehind {
                    val currentFraction = fractionAnim.value
                    val currentTrackColor = lerp(uncheckedTrackColor, checkedTrackColor, currentFraction)
                    val effectiveTrackColor = if (enabled) {
                        currentTrackColor
                    } else {
                        currentTrackColor.copy(alpha = currentTrackColor.alpha * 0.38f)
                    }
                    drawRect(effectiveTrackColor)
                }
                .border(
                    width = 0.5.dp,
                    color = Color.White.copy(alpha = if (isDark) 0.12f else 0.08f),
                    shape = CircleShape
                )
                .size(trackWidthDp, trackHeightDp)
        )

        // Refractive Liquid Glass Thumb
        val pressProgress = pressProgressAnim.value
        val thumbModifier = if (useRuntimeGlass) {
            Modifier.drawBackdrop(
                backdrop = trackBackdrop,
                shape = { CircleShape },
                effects = {
                    blur(2.dp.toPx() + 1.dp.toPx() * pressProgress)
                    lens(
                        refractionHeight = (3.5.dp + 1.5.dp * pressProgress).toPx(),
                        refractionAmount = (8.dp + 4.dp * pressProgress).toPx(),
                        chromaticAberration = true
                    )
                },
                onDrawBackdrop = { drawBackdrop ->
                    drawBackdrop()
                },
                highlight = {
                    Highlight.Ambient.copy(
                        width = 0.5.dp,
                        blurRadius = 0.3.dp,
                        alpha = 0.70f + 0.30f * pressProgress
                    )
                },
                shadow = {
                    Shadow(
                        radius = 4.dp + 2.dp * pressProgress,
                        offset = DpOffset(0.dp, 1.5.dp + 0.5.dp * pressProgress),
                        color = Color.Black.copy(alpha = 0.35f + 0.10f * pressProgress)
                    )
                },
                innerShadow = {
                    InnerShadow(
                        radius = 2.dp + 2.dp * pressProgress,
                        offset = DpOffset(0.dp, 1.dp + 0.5.dp * pressProgress),
                        alpha = 0.35f + 0.35f * pressProgress
                    )
                },
                layerBlock = {
                    val scale = 1f + 0.15f * pressProgress
                    scaleX = scale
                    scaleY = scale
                },
                onDrawSurface = {
                    drawRect(Color.White.copy(alpha = 0.22f + 0.10f * (1f - pressProgress)))
                }
            )
        } else {
            Modifier
                .graphicsLayer {
                    val scale = 1f + 0.15f * pressProgress
                    scaleX = scale
                    scaleY = scale
                }
                .shadow(elevation = 3.dp, shape = CircleShape)
                .background(
                    Color.White.copy(alpha = (0.90f - pressProgress * 0.20f).coerceIn(0.70f, 0.90f)),
                    CircleShape
                )
                .border(
                    width = 0.5.dp,
                    color = Color.White.copy(alpha = 0.5f),
                    shape = CircleShape
                )
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .graphicsLayer {
                    val paddingPx = paddingDp.toPx()
                    val fraction = fractionAnim.value
                    translationX = if (isLtr) {
                        lerp(paddingPx, paddingPx + dragWidthPx, fraction)
                    } else {
                        lerp(-paddingPx, -(paddingPx + dragWidthPx), fraction)
                    }
                }
                .then(thumbModifier)
                .border(
                    width = 0.5.dp,
                    color = Color.White.copy(alpha = 0.35f),
                    shape = CircleShape
                )
                .size(thumbWidthDp, thumbHeightDp)
        ) {
            thumbContent?.invoke()
        }
    }
}
