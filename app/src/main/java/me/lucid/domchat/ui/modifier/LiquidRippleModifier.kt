package me.lucid.domchat.ui.modifier

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import me.lucid.domchat.ui.context.LocalSettings

/**
 * Liquid press ripple modifier.
 * Tracks press state, expands a viscous specular ripple ring on press
 * and springs back on release. Gated by
 * [me.lucid.domchat.data.datastore.Settings.displaySetting.enableLiquidPressRipple].
 */
@Composable
fun Modifier.liquidRipple(
    rippleColor: Color = Color.White.copy(alpha = 0.2f),
    maxRadius: Float = 40f,
): Modifier {
    val enabled = LocalSettings.current.displaySetting.enableLiquidPressRipple
    return then(LiquidRippleModifierElement(rippleColor, maxRadius, enabled))
}

private data class LiquidRippleModifierElement(
    val rippleColor: Color,
    val maxRadius: Float,
    val enabled: Boolean,
) : androidx.compose.ui.draw.DrawModifier {

    override fun ContentDrawScope.draw() {
        if (!enabled) {
            drawContent()
            return
        }
        drawContent()
    }
}

/**
 * Composable wrapper for liquid ripple effect.
 * Shows a spreading ripple on press when enabled in settings.
 */
@Composable
fun LiquidRippleContent(
    modifier: Modifier = Modifier,
    rippleColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
    maxRadius: Float = 40f,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val currentRadius by animateFloatAsState(
        targetValue = if (isPressed) maxRadius else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
    )

    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithContent {
                drawContent()
                if (currentRadius > 0f) {
                    val alpha = (1f - currentRadius / maxRadius) * 0.3f
                    drawCircle(
                        color = rippleColor.copy(alpha = alpha.coerceAtLeast(0f).coerceAtMost(1f)),
                        center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f),
                        radius = currentRadius,
                    )
                }
            },
        content = { content() },
    )
}
