package me.lucid.domchat.ui.components.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.launch
import me.lucid.domchat.ui.component.backdrop.Backdrop
import me.lucid.domchat.ui.component.backdrop.LocalAppBackdrop
import me.lucid.domchat.ui.component.backdrop.backdrops.CombinedBackdrop
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
import me.lucid.domchat.ui.theme.LocalDarkMode

/**
 * Liquid Glass Style Switch (`DomChatGlassSwitch`).
 *
 * Adapted from Convx's CupertinoSwitch:
 * - 60dp x 28dp pill track with specular border and dynamic accent/OLED contrast.
 * - 36dp x 24dp crisp solid white handle at rest with shadow.
 * - Interactive damped finger drag with touch-slop protection (never toggles on parent vertical scroll).
 * - On press/drag (API 33+), transitions smoothly into live refractive liquid glass
 *   refracting both the live sheet backdrop and track edges via combined backdrop.
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
    val isDark = LocalDarkMode.current
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
    val appBackdrop = LocalAppBackdrop.current
    val combinedBackdrop: Backdrop = remember(appBackdrop, trackBackdrop) {
        if (appBackdrop != null) {
            CombinedBackdrop(listOf(appBackdrop, trackBackdrop))
        } else {
            trackBackdrop
        }
    }

    val uncheckedTrackColor = if (isDark) Color.White.copy(alpha = 0.16f) else Color.Black.copy(alpha = 0.14f)
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
                if (!enabled) disabled()
                onClick {
                    if (enabled && onCheckedChange != null) {
                        onCheckedChange(!checked)
                        true
                    } else false
                }
            }
            .then(
                if (enabled && onCheckedChange != null) {
                    Modifier.pointerInput(enabled) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            scope.launch {
                                pressProgressAnim.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = 600f))
                            }
                            val up = try {
                                waitForUpOrCancellation()
                            } finally {
                                scope.launch {
                                    pressProgressAnim.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = 600f))
                                }
                            }
                            if (up != null) {
                                up.consume()
                                haptics.perform(HapticPattern.Pop)
                                currentOnCheckedChange?.invoke(!currentChecked)
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
                    color = if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f),
                    shape = CircleShape
                )
                .size(trackWidthDp, trackHeightDp)
        )

        // Refractive Liquid Glass Thumb (crisp white at rest, live refractive glass when pressed)
        val thumbModifier = if (useRuntimeGlass) {
            Modifier.drawBackdrop(
                backdrop = combinedBackdrop,
                shape = { CircleShape },
                effects = {
                    val progress = pressProgressAnim.value.coerceIn(0f, 1f)
                    if (progress > 0f) {
                        blur(4.dp.toPx() * (1f - progress))
                        lens(
                            refractionHeight = 5.dp.toPx() * progress,
                            refractionAmount = 10.dp.toPx() * progress,
                            chromaticAberration = true
                        )
                    }
                },
                onDrawBackdrop = { drawBackdrop ->
                    if (pressProgressAnim.value.coerceIn(0f, 1f) > 0f) {
                        drawBackdrop()
                    }
                },
                highlight = {
                    val progress = pressProgressAnim.value.coerceIn(0f, 1f)
                    if (progress > 0f) {
                        Highlight.Ambient.copy(
                            width = 0.5.dp,
                            blurRadius = 0.3.dp,
                            alpha = progress * 0.85f
                        )
                    } else null
                },
                shadow = {
                    Shadow(
                        radius = 4.dp,
                        offset = DpOffset(0.dp, 1.5.dp),
                        color = Color.Black.copy(alpha = 0.28f)
                    )
                },
                innerShadow = {
                    val progress = pressProgressAnim.value.coerceIn(0f, 1f)
                    if (progress > 0f) {
                        InnerShadow(
                            radius = 4.dp * progress,
                            offset = DpOffset(0.dp, 1.dp * progress),
                            alpha = progress
                        )
                    } else null
                },
                layerBlock = {
                    val progress = pressProgressAnim.value.coerceIn(0f, 1f)
                    val scale = 1f + 0.25f * progress
                    scaleX = scale
                    scaleY = scale
                    if (!enabled) {
                        alpha = 0.38f
                    }
                },
                onDrawSurface = {
                    val progress = pressProgressAnim.value.coerceIn(0f, 1f)
                    drawRect(Color.White.copy(alpha = (1f - progress).coerceIn(0.08f, 1f)))
                }
            )
        } else {
            Modifier
                .graphicsLayer {
                    val progress = pressProgressAnim.value.coerceIn(0f, 1f)
                    val scale = 1f + 0.25f * progress
                    scaleX = scale
                    scaleY = scale
                    if (!enabled) {
                        alpha = 0.38f
                    }
                }
                .shadow(elevation = 3.dp, shape = CircleShape)
                .background(
                    Color.White,
                    CircleShape
                )
                .border(
                    width = 0.5.dp,
                    color = Color.White.copy(alpha = 0.6f),
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
                .size(thumbWidthDp, thumbHeightDp)
                .then(thumbModifier)
        ) {
            thumbContent?.invoke()
        }
    }
}
