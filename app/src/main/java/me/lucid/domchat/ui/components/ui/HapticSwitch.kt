package me.lucid.domchat.ui.components.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import me.lucid.domchat.ui.hooks.HapticPattern
import me.lucid.domchat.ui.hooks.rememberPremiumHaptics
import me.lucid.domchat.ui.theme.LiquidGlassTokens

/**
 * A Material 3 Switch wrapped with premium haptic feedback.
 * 
 * This component provides consistent haptic feedback across the app
 * when users toggle switches, following the "fidget toy" philosophy.
 * 
 * Uses [HapticPattern.Pop] for tactile feedback on state change.
 * 
 * In LIQUID_GLASS mode, renders an Apple-style glass switch with spring-animated thumb.
 * In OG_BLUR mode, renders the standard Material 3 switch.
 */
@Composable
fun HapticSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    thumbContent: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    colors: SwitchColors = SwitchDefaults.colors(),
    glassMode: String = "LIQUID_GLASS",
) {
    val haptics = rememberPremiumHaptics()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isLiquidGlass = glassMode == "LIQUID_GLASS"

    // Thumb position animation with spring physics
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 20.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = 300f,
        ),
        label = "switch_thumb_offset",
    )

    if (isLiquidGlass) {
        // Apple-style glass switch with spring-animated thumb & track refraction
        Surface(
            shape = CircleShape,
            color = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
            border = if (checked) {
                BorderStroke(
                    0.5.dp,
                    LiquidGlassTokens.SpecularBorderBrush,
                )
            } else {
                BorderStroke(
                    0.5.dp,
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                )
            },
            modifier = modifier
                .clickable(
                    enabled = enabled,
                    onClick = {
                        if (enabled) {
                            haptics.perform(HapticPattern.Pop)
                            onCheckedChange?.invoke(!checked)
                        }
                    },
                ),
        ) {
            // Thumb circle positioned based on checked state
            Surface(
                shape = CircleShape,
                color = Color.White,
                modifier = androidx.compose.ui.Modifier
                    .size(28.dp)
                    .offset(x = thumbOffset),
            )
        }
    } else {
        Switch(
            checked = checked,
            onCheckedChange = { newValue ->
                if (enabled) {
                    haptics.perform(HapticPattern.Pop)
                    onCheckedChange?.invoke(newValue)
                }
            },
            modifier = modifier,
            thumbContent = thumbContent,
            enabled = enabled,
            colors = colors
        )
    }
}
