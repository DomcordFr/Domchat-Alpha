package me.lucid.domchat.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.lucid.domchat.ui.context.LocalSettings
import me.lucid.domchat.data.model.LiquidGlassMode


/**
 * Dynamic liquid glass surface color derived from glassAlpha and glassBaseLuminance.
 * Returns Color.Transparent when glassAlpha <= 0.001f (0% opacity).
 */
@Composable
fun liquidGlassSurfaceColor(): Color {
    val settings = LocalSettings.current
    val display = settings.displaySetting
    val alpha = display.glassAlpha.coerceIn(0f, 1f)
    if (alpha <= 0.001f) return Color.Transparent
    val base = androidx.compose.ui.graphics.lerp(Color.Black, Color.White, display.glassBaseLuminance.coerceIn(0f, 1f))
    return base.copy(alpha = alpha)
}

/**
 * Dynamic specular border brush for glass surfaces. Clear Glass uses a 0.18f white specular rim;
 * DARK and FROSTED use subtler rim catches scaled to their surface brightness.
 */
@Composable
fun liquidGlassBorderBrush(): Brush {
    return LiquidGlassTokens.SpecularBorderBrush
}

/**
 * Dynamic chat bubble surface color derived from chatBubbleAlpha and chatBubbleTone.
 * Returns Color.Transparent when chatBubbleAlpha <= 0.001f (0% opacity).
 */
@Composable
fun liquidBubbleSurfaceColor(): Color {
    val settings = LocalSettings.current
    val display = settings.displaySetting
    val alpha = display.chatBubbleAlpha.coerceIn(0f, 1f)
    if (alpha <= 0.001f) return Color.Transparent
    val base = androidx.compose.ui.graphics.lerp(Color.Black, Color.White, display.chatBubbleTone.coerceIn(0f, 1f))
    return base.copy(alpha = alpha)
}

/**
 * Specular rim alpha for Clear Glass (0.18f white specular rim per design charter).
 */
val LiquidGlassSpecularRimAlpha = 0.18f

/**
 * Modifier that applies clean liquid glass surface optics without blurring foreground children.
 */
fun Modifier.liquidGlassSurface(
    shape: Shape = LiquidGlassTokens.CardShape,
    borderWidth: Dp = 0.5.dp,
    specularBrush: Brush = LiquidGlassTokens.SpecularBorderBrush,
    fillColor: Color = Color.Transparent,
    shadowElevation: Dp = 0.dp,
    glassMode: LiquidGlassMode = LiquidGlassMode.BALANCED_BLEND,
    isFloatingControl: Boolean = false
): Modifier {
    val withShadow = if (shadowElevation > 0.dp) {
        this.shadow(
            elevation = shadowElevation,
            shape = shape,
            ambientColor = Color.Black.copy(alpha = 0.25f),
            spotColor = Color.Black.copy(alpha = 0.35f)
        )
    } else {
        this
    }
    val withBg = if (fillColor != Color.Transparent && fillColor != Color.Unspecified) {
        withShadow.background(
            color = fillColor,
            shape = shape
        )
    } else {
        withShadow
    }
    return withBg
        .border(
            width = borderWidth,
            brush = specularBrush,
            shape = shape
        )
        .clip(shape)
}

/**
 * Liquid Glass Card Container. Keeps content, avatars, and text razor-sharp while providing optical glass depth.
 */
@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = LiquidGlassTokens.CardShape,
    borderWidth: Dp = 0.5.dp,
    specularBrush: Brush = LiquidGlassTokens.SpecularBorderBrush,
    fillColor: Color = Color.Transparent,
    glassMode: LiquidGlassMode = LiquidGlassMode.BALANCED_BLEND,
    isFloatingControl: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.liquidGlassSurface(
            shape = shape,
            borderWidth = borderWidth,
            specularBrush = specularBrush,
            fillColor = fillColor,
            glassMode = glassMode,
            isFloatingControl = isFloatingControl
        ),
        content = content
    )
}
