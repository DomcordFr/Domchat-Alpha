package me.lucid.domchat.ui.components.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import me.lucid.domchat.ui.theme.LocalDarkMode

/**
 * The single app-wide slider: bright white thumb/track on dark glass, subtly scaled for touch.
 * Every in-app slider (fade duration, samplers, context budgets, depth, etc.) must use this.
 */
@Composable
fun DomChatSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    activeTrackColorOverride: Color? = null,
    thumbColorOverride: Color? = null,
) {
    val isDark = LocalDarkMode.current
    val sliderColors = SliderDefaults.colors(
        thumbColor = thumbColorOverride ?: if (isDark) Color.White else MaterialTheme.colorScheme.primary,
        activeTrackColor = activeTrackColorOverride
            ?: if (isDark) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.primary,
        inactiveTrackColor = if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.1f)
    )
    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        valueRange = valueRange,
        steps = steps,
        onValueChangeFinished = onValueChangeFinished,
        colors = sliderColors,
    )
}
