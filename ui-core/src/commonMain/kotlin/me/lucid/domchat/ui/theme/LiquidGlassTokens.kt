package me.lucid.domchat.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Liquid Glass Material Tokens for ui-core (shared module).
 * Provides glassmorphism visual tokens without depending on :app module.
 */
object LiquidGlassTokens {
    val CardShape = RoundedCornerShape(20.dp)
    val BubbleShape = RoundedCornerShape(18.dp)
    val PillShape = RoundedCornerShape(100.dp)
    val Squircle = RoundedCornerShape(16.dp)

    // Specular border gradients - light reflects from top, disperses cleanly towards bottom
    val SpecularBorderBrush = Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = 0.40f),
            Color.White.copy(alpha = 0.12f),
            Color.White.copy(alpha = 0.03f),
            Color.Transparent,
        )
    )

    val SpecularBorderSubtle = Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = 0.22f),
            Color.White.copy(alpha = 0.06f),
            Color.Transparent,
        )
    )

    val LightSpecularBorder = Brush.verticalGradient(
        listOf(
            Color.Black.copy(alpha = 0.12f),
            Color.Black.copy(alpha = 0.04f),
            Color.Transparent,
        )
    )

    // Legacy static fallbacks
    val GlassSurfaceDark = Color(0xFF141520).copy(alpha = 0.22f)
    val GlassSurfaceLight = Color(0xFFFFFFFF).copy(alpha = 0.45f)
    val BubbleGlassDark = Color(0xFFFFFFFF).copy(alpha = 0.06f)
    val BubbleGlassLight = Color(0xFF000000).copy(alpha = 0.03f)
}
